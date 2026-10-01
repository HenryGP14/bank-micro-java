package com.example.ms_transaction_service.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BasicResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;

    public static <T> BasicResponse<T> success(int code, String message, T data) {
        return BasicResponse.<T>builder().success(true).code(code).message(message).data(data).build();
    }

    public static <T> BasicResponse<T> success(int code, String message) {
        return BasicResponse.<T>builder().success(true).code(code).message(message).build();
    }

    public static <T> BasicResponse<T> error(int code, String message) {
        return BasicResponse.<T>builder().success(false).code(code).message(message).build();
    }
}
