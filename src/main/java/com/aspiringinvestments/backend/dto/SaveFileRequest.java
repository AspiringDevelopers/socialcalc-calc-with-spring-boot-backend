package com.aspiringinvestments.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class SaveFileRequest {
    
    @NotBlank
    private String fname;
    
    @NotBlank
    private String data;

    // Constructors
    public SaveFileRequest() {}

    public SaveFileRequest(String fname, String data) {
        this.fname = fname;
        this.data = data;
    }

    // Getters and Setters
    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}