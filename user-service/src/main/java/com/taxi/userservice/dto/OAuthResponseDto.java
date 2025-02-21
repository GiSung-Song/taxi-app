package com.taxi.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthResponseDto {

    private String email;
    private String provider;
    private String providerId;
    private String role;
    private String name;
}