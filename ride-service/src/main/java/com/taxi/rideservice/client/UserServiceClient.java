package com.taxi.rideservice.client;

import com.taxi.common.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/auth/{email}")
    UserDto getUserInfoByEmail(@PathVariable("email") String email);
}