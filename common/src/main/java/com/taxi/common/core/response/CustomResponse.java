package com.taxi.common.core.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomResponse<T> {

    private int status;
    private T data;
    private String message;

    public static <T> CustomResponse<T> success(T data, ResponseCode responseCode) {
        return new CustomResponse<>(responseCode.getStatus().value(), data, responseCode.getMessage());
    }

    public static <T> CustomResponse<T> error(ResponseCode responseCode) {
        return new CustomResponse<>(responseCode.getStatus().value(), null, responseCode.getMessage());
    }
}
