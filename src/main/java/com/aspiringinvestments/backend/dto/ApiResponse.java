package com.aspiringinvestments.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private String result;
    private T data;
    private String message;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(String result) {
        this.result = result;
    }

    public ApiResponse(String result, T data) {
        this.result = result;
        this.data = data;
    }

    public ApiResponse(String result, String message) {
        this.result = result;
        this.message = message;
    }

    public ApiResponse(String result, T data, String message) {
        this.result = result;
        this.data = data;
        this.message = message;
    }

    // Static factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("ok", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>("ok");
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("fail", message);
    }

    // Getters and Setters
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}