package com.taxi.rideservice.service;

import com.taxi.common.dto.UserDto;
import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserServiceClient userServiceClient;

    // 기사(차량) 추가 정보 저장
    @Transactional
    public void registerDriver(DriverRegisterDto dto) {
        if (driverRepository.findByUserId(dto.getUserId()) != null) {
            log.error("이미 등록된 회원입니다.");
            throw new CustomBadRequestException("이미 등록된 회원입니다.");
        }

        Driver driver = Driver.builder()
                .userId(dto.getUserId())
                .carNumber(dto.getCarNumber())
                .capacity(dto.getCapacity())
                .carName(dto.getCarName())
                .license(dto.getLicense())
                .phoneNumber(dto.getPhoneNumber())
                .driverStatus(DriverStatus.OFFLINE)
                .totalRides(0)
                .build();

        driverRepository.save(driver);
    }

    // 기사(차량) 정보 수정
    @Transactional
    public void updateDriver(DriverUpdateDto dto) {
        Driver driver = getDriver(dto.getEmail());

        driver.updateCarName(dto.getCarName());
        driver.updateCarNumber(dto.getCarNumber());
        driver.updateCapacity(dto.getCapacity());
        driver.updateLicense(dto.getLicense());
        driver.updatePhoneNumber(dto.getPhoneNumber());
    }

    // 기사(차량) 상태 수정
    @Transactional
    public void updateDriverStatus(DriverStatusUpdateDto dto) {
        Driver driver = getDriver(dto.getEmail());

        driver.updateDriverStatus(DriverStatus.valueOf(dto.getDriverStatus()));
    }

    private Driver getDriver(String email) {
        UserDto userInfo = userServiceClient.getUserInfoByEmail(email);
        Driver driver = driverRepository.findByUserId(userInfo.getUserId());

        if (driver == null) {
            log.error("정보가 없는 회원입니다.");
            throw new CustomBadRequestException("정보가 없는 회원입니다.");
        }

        return driver;
    }
}
