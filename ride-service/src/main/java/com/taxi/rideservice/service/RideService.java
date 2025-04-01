package com.taxi.rideservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.*;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.common.core.exception.CustomInternalException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.entity.Ride;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.enums.RideStatus;
import com.taxi.rideservice.repository.DriverRepository;
import com.taxi.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final DriverRepository driverRepository;
    private final UserServiceClient userServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String GEO_KEY = "ride:request";
    private static final String DETAIL_KEY_PREFIX = "ride:detail:";

    // 택시 호출
    public void saveCallRequest(RideCallRequestDto dto) {
        try {
            // Geo 정보 저장
            log.info("longitude : {}, latitude : {}", dto.getStartLongitude(), dto.getStartLatitude());

            redisTemplate.opsForGeo().add(GEO_KEY,
                    new Point(dto.getStartLongitude(), dto.getStartLatitude()),
                    dto.getPassengerEmail());

            String key = DETAIL_KEY_PREFIX + dto.getPassengerEmail();
            String value = objectMapper.writeValueAsString(dto);

            // 승객 상세정보 저장 (이메일, 탑승지, 목적지)
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류가 발생 : {}", e.getMessage());
            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }

    // 호출 목록 조회 (기사의 현재 위치를 받아 근처 5km 이내에 있는 호출 목록을 조회)
    public List<CallResponseDto> findNearbyCall(FindCallRequestDto dto) {
        // 5km 이내에서 호출한 승객 목록
        List<String> passengers = redisTemplate.opsForGeo()
                .radius(GEO_KEY, new Circle(new Point(dto.getLongitude(), dto.getLatitude()), new Distance(5, Metrics.KILOMETERS)))
                .getContent().stream()
                .map(result -> result.getContent().getName())
                .collect(Collectors.toList());

        // 승객 이메일을 이용하여 상세 정보(이메일, 출발지, 목적지 가져오기) 반환
        return passengers.stream()
                .map(email -> redisTemplate.opsForValue().get(DETAIL_KEY_PREFIX + email))
                .map(json -> {
                    try {
                        RideCallRequestDto rideCallRequestDto = objectMapper.readValue(json, RideCallRequestDto.class);

                        return new CallResponseDto(rideCallRequestDto.getPassengerEmail(), rideCallRequestDto.getStartLocation(), rideCallRequestDto.getEndLocation());
                    } catch (Exception e) {
                        log.error("Json to Dto 변환 시 내부적인 오류가 발생 : {}", e.getMessage());
                        throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
                    }
                })
                .collect(Collectors.toList());
    }

    // 택시 호출 수락
    @Transactional
    public RideAcceptDto acceptCall(CallAcceptRequestDto dto) {
        try {
            String jsonData = redisTemplate.opsForValue().get(DETAIL_KEY_PREFIX + dto.getPassengerEmail());
            RideCallRequestDto rideCallRequestDto = objectMapper.readValue(jsonData, RideCallRequestDto.class);

            // 승객의 USER ID
            UserDto passengerInfo = userServiceClient.getUserInfoByEmail(dto.getPassengerEmail());

            // 기사의 USER ID
            UserDto driverInfo = userServiceClient.getUserInfoByEmail(dto.getDriverEmail());

            // 기사 정보
            Driver driver = driverRepository.findByUserId(driverInfo.getUserId());

            // 기사 정보가 없으면 예외처리
            if (driver == null) {
                log.error("호출 수락 중 기사 정보 없음");
                throw new CustomBadRequestException("호출을 수락할 수 없습니다.");
            }

            // 기사 상태가 대기중이 아니면 예외처리
            if (!DriverStatus.WAITING.equals(driver.getDriverStatus())) {
                log.error("대기상태가 아니면 수락할 수 없음");
                throw new CustomBadRequestException("호출을 수락할 수 없습니다.");
            }

            // ride 정보 저장
            Ride ride = Ride.builder()
                    .passengerId(passengerInfo.getUserId())
                    .driverId(driver.getId())
                    .fare(0) // 요금은 바뀌기 때문에 0으로 세팅
                    .startLatitude(rideCallRequestDto.getStartLatitude())
                    .startLongitude(rideCallRequestDto.getStartLongitude())
                    .startLocation(rideCallRequestDto.getStartLocation())
                    .endLatitude(rideCallRequestDto.getEndLatitude())
                    .endLongitude(rideCallRequestDto.getEndLongitude())
                    .endLocation(rideCallRequestDto.getEndLocation())
                    .rideStatus(RideStatus.ACCEPT)
                    .build();

            rideRepository.save(ride);

            // 기사 상태 변경
            driver.updateDriverStatus(DriverStatus.RESERVATION);

            // redis -> 승객정보 삭제
            redisTemplate.delete(DETAIL_KEY_PREFIX + dto.getPassengerEmail());

            // redis -> 승객 출발지 위치정보 삭제
            redisTemplate.opsForGeo().remove(GEO_KEY, dto.getPassengerEmail());

            // kafka로 전송할 데이터 반환
            RideAcceptDto rideAcceptDto = new RideAcceptDto();

            rideAcceptDto.setRideId(ride.getId());

            // 기사가 확인할 정보 (전화번호, 출발지, 목적지)
            rideAcceptDto.setPassengerUserId(passengerInfo.getUserId());
            rideAcceptDto.setPassengerPhoneNumber(passengerInfo.getPhoneNumber());
            rideAcceptDto.setStartLocation(rideCallRequestDto.getStartLocation());
            rideAcceptDto.setEndLocation(rideCallRequestDto.getEndLocation());

            // 승객이 확인할 정보 (기사 이름, 기사 전화번호, 차 번호, 차 종류)
            rideAcceptDto.setDriverUserId(driverInfo.getUserId());
            rideAcceptDto.setDriverName(driverInfo.getName());
            rideAcceptDto.setDriverPhoneNumber(driver.getPhoneNumber());
            rideAcceptDto.setCarName(driver.getCarName());
            rideAcceptDto.setCarNumber(driver.getCarNumber());
            rideAcceptDto.setCapacity(driver.getCapacity());
            rideAcceptDto.setTotalRides(driver.getTotalRides());

            rideAcceptDto.setRideStatus(RideStatus.ACCEPT.name());
            rideAcceptDto.setAcceptTime(ride.getCreatedAt());

            return rideAcceptDto;
        } catch (CustomBadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류 발생 : {}", e.getMessage());
            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }

    }

    // 운행 호출 취소
    @Transactional
    public RideCancelDto cancelRide(Long rideId) {
        Ride ride = getRide(rideId);
        Driver driver = getDriver(ride.getDriverId());

        // 운행 수락이 된 상태가 아니라면 ex) 취소, 종료, 운행중
        if (!ride.getRideStatus().equals(RideStatus.ACCEPT)) {
            log.error("호출 수락 상태가 아니면 취소할 수 없음");
            throw new CustomBadRequestException("호출 취소를 할 수 없는 상태입니다.");
        }

        // 운행 취소로 변경
        ride.updateRideStatus(RideStatus.CANCEL);

        // 운행 대기상태로 변경
        driver.updateDriverStatus(DriverStatus.WAITING);

        UserDto driverInfo = userServiceClient.getUserInfoById(driver.getUserId());

        // dto 설정
        RideCancelDto rideCancelDto = new RideCancelDto();

        rideCancelDto.setDriverUserId(driverInfo.getUserId());
        rideCancelDto.setRideId(rideId);
        rideCancelDto.setCancelTime(ride.getUpdatedAt());
        rideCancelDto.setRideStatus(RideStatus.CANCEL.name());

        return rideCancelDto;
    }

    // 운행 시작
    @Transactional
    public RideStartDto startRide(Long rideId) {
        Ride ride = getRide(rideId);
        Driver driver = getDriver(ride.getDriverId());

        // 운행 중으로 상태 변경
        if (!RideStatus.ACCEPT.equals(ride.getRideStatus()) || !DriverStatus.RESERVATION.equals(driver.getDriverStatus())) {
            throw new CustomBadRequestException("운행 시작을 할 수 없는 상태입니다.");
        }

        // 운행 중으로 상태 변경
        ride.updateRideStatus(RideStatus.DRIVING);
        driver.updateDriverStatus(DriverStatus.DRIVING);

        // driver, passenger 정보 조회
        UserDto passengerInfo = userServiceClient.getUserInfoById(ride.getPassengerId());
        UserDto driverInfo = userServiceClient.getUserInfoById(driver.getUserId());

        RideStartDto rideStartDto = new RideStartDto();

        rideStartDto.setRideId(rideId);

        rideStartDto.setPassengerPhoneNumber(passengerInfo.getPhoneNumber());

        rideStartDto.setDriverName(driverInfo.getName());
        rideStartDto.setDriverPhoneNumber(driverInfo.getPhoneNumber());
        rideStartDto.setCarName(driver.getCarName());
        rideStartDto.setCarNumber(driver.getCarNumber());

        rideStartDto.setStartLocation(ride.getStartLocation());
        rideStartDto.setEndLocation(ride.getEndLocation());
        rideStartDto.setRideStatus(RideStatus.DRIVING.name());
        rideStartDto.setStartTime(ride.getUpdatedAt());

        return rideStartDto;
    }

    // 운행 완료
    @Transactional
    public DriveCompleteDto completeRide(RideCompleteDto dto) {
        Ride ride = getRide(dto.getRideId());
        Driver driver = getDriver(ride.getDriverId());

        // 요금 추가 및 운행 상태 변경
        if (!RideStatus.DRIVING.equals(ride.getRideStatus()) || !DriverStatus.DRIVING.equals(driver.getDriverStatus())) {
            throw new CustomBadRequestException("운행 완료를 할 수 없는 상태입니다.");
        }

        ride.completeRide(dto.getFare());

        // 기사 상태 변경 (운행횟수 +1, 운행상태 wait)
        driver.finishRide();

        // driver, passenger 정보 조회
        UserDto passengerInfo = userServiceClient.getUserInfoById(ride.getPassengerId());
        UserDto driverInfo = userServiceClient.getUserInfoById(driver.getUserId());

        DriveCompleteDto driveCompleteDto = new DriveCompleteDto();

        driveCompleteDto.setRideId(ride.getId());

        driveCompleteDto.setPassengerPhoneNumber(passengerInfo.getPhoneNumber());

        driveCompleteDto.setDriverName(driverInfo.getName());
        driveCompleteDto.setDriverPhoneNumber(driver.getPhoneNumber());
        driveCompleteDto.setCarName(driver.getCarName());
        driveCompleteDto.setCarNumber(driver.getCarNumber());

        driveCompleteDto.setFare(ride.getFare());
        driveCompleteDto.setStartLocation(ride.getStartLocation());
        driveCompleteDto.setEndLocation(ride.getEndLocation());
        driveCompleteDto.setRideStatus(RideStatus.COMPLETE.name());
        driveCompleteDto.setCompleteTime(ride.getUpdatedAt());

        return driveCompleteDto;
    }

    // 롤백
    @Transactional
    public void rollBackRide(RollBackDto rollBackDto) {
        Ride ride = rideRepository.findById(rollBackDto.getRideId())
                .orElse(null);

        if (ride != null) {
            rideRepository.delete(ride);
        }

        Driver driver = driverRepository.findByUserId(rollBackDto.getDriverUserId());

        if (driver == null) {
            throw new CustomInternalException("해당 기사가 없습니다.");
        }

        driver.updateDriverStatus(DriverStatus.WAITING);
    }

    // 운행 시작 메서드 실패 시 강제 시작
    // kafka 보상 메서드
    public void rollBackStartRide(RideStartDto rideStartDto) {
        Ride ride = getRide(rideStartDto.getRideId());
        Driver driver = getDriver(ride.getDriverId());

        // 운행 중으로 상태 강제 변경
        ride.updateRideStatus(RideStatus.DRIVING);
        driver.updateDriverStatus(DriverStatus.DRIVING);
    }

    // 운행 종료 메서드 실패 시 강제 종료
    // kafka 보상 메서드
    public void rollBackCompleteRide(DriveCompleteDto rideCompleteDto) {
        Ride ride = getRide(rideCompleteDto.getRideId());
        Driver driver = getDriver(ride.getDriverId());

        ride.completeRide(rideCompleteDto.getFare());

        // 기사 상태 변경 (운행횟수 +1, 운행상태 wait)
        driver.finishRide();
    }

    private Driver getDriver(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomInternalException("정확한 기사 번호가 입력되어지지 않았습니다."));
        return driver;
    }

    private Ride getRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new CustomBadRequestException("정확한 운행번호를 입력해주세요."));
        return ride;
    }
}
