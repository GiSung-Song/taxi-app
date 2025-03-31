package com.taxi.rideservice.client;

import com.taxi.common.core.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/auth/email/{email}")
    UserDto getUserInfoByEmail(@PathVariable("email") String email);

    @GetMapping("/api/auth/id/{id}")
    UserDto getUserInfoById(@PathVariable("id") Long id);
}