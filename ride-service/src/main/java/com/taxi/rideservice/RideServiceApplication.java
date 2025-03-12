package com.taxi.rideservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.taxi.common", "com.taxi.rideservice"})
@EnableFeignClients
public class RideServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideServiceApplication.class, args);
    }

}
