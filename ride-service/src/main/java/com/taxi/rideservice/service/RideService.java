package com.taxi.rideservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.dto.UserDto;
import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.common.exception.CustomInternalException;
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
    public RideInfoDto acceptCall(CallAcceptRequestDto dto) {
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
            RideInfoDto rideInfo = new RideInfoDto();

            rideInfo.setRideId(ride.getId());

            // 기사가 확인할 정보 (이름, 전화번호, 출발지, 목적지)
            rideInfo.setPassengerUserId(passengerInfo.getUserId());
            rideInfo.setPassengerName(passengerInfo.getName());
            rideInfo.setPassengerPhoneNumber(passengerInfo.getPhoneNumber());
            rideInfo.setStartLocation(rideCallRequestDto.getStartLocation());
            rideInfo.setEndLocation(rideCallRequestDto.getEndLocation());

            // 승객이 확인할 정보 (
            rideInfo.setDriverUserId(driverInfo.getUserId());
            rideInfo.setDriverName(driverInfo.getName());
            rideInfo.setDriverPhoneNumber(driver.getPhoneNumber());
            rideInfo.setCarName(driver.getCarName());
            rideInfo.setCarNumber(driver.getCarNumber());
            rideInfo.setCapacity(driver.getCapacity());
            rideInfo.setTotalRides(driver.getTotalRides());

            return rideInfo;
        } catch (CustomBadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류 발생 : {}", e.getMessage());
            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }

    }

    // 운행 호출 취소
    @Transactional
    public void cancelRide(Long rideId) {
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
    }

    // 운행 시작
    @Transactional
    public void startRide(Long rideId) {
        Ride ride = getRide(rideId);
        Driver driver = getDriver(ride.getDriverId());

        // 운행 중으로 상태 변경
        if (!RideStatus.ACCEPT.equals(ride.getRideStatus()) || !DriverStatus.RESERVATION.equals(driver.getDriverStatus())) {
            throw new CustomBadRequestException("운행 시작을 할 수 없는 상태입니다.");
        }

        ride.updateRideStatus(RideStatus.DRIVING);

        // 운행 중으로 상태 변경
        driver.updateDriverStatus(DriverStatus.DRIVING);
    }

    // 운행 완료
    @Transactional
    public void completeRide(RideCompleteDto dto) {
        Ride ride = getRide(dto.getRideId());
        Driver driver = getDriver(ride.getDriverId());

        // 요금 추가 및 운행 상태 변경
        if (!RideStatus.DRIVING.equals(ride.getRideStatus()) || !DriverStatus.DRIVING.equals(driver.getDriverStatus())) {
            throw new CustomBadRequestException("운행 완료를 할 수 없는 상태입니다.");
        }

        ride.completeRide(dto.getFare());

        // 기사 상태 변경 (운행횟수 +1, 운행상태 wait)
        driver.finishRide();
    }

    // 롤백
    @Transactional
    public void rollBackRide(RideInfoDto rideInfoDto) {
        Ride ride = rideRepository.findById(rideInfoDto.getRideId())
                .orElse(null);

        if (ride != null) {
            rideRepository.delete(ride);
        }

        Driver driver = driverRepository.findByUserId(rideInfoDto.getDriverUserId());

        if (driver == null) {
            throw new CustomInternalException("해당 기사가 없습니다.");
        }

        driver.updateDriverStatus(DriverStatus.WAITING);
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
