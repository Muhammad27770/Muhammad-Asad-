package com.example.student_service.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String emailAddress;  // Changed from 'email'
    private String loginPassword; // Changed from 'password'

    // Getters and setters
    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public void setLoginPassword(String loginPassword) {
        this.loginPassword = loginPassword;
    }
}