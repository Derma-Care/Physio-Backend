package com.dermacare.doctorservice.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoctorDTO {
    private String userName;
    
    private String password;
    
    private String hospitalId; 

//    private String fcmToken;
    
    private String deviceId;

    private String doctorEmail;
    
  
    public void setDoctorMobileNumber(String username) {
        this.userName = username != null ? username.trim() : null;
    }

    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }

//    public void fcmToken(String fcmToken) {
//        this.fcmToken = fcmToken != null ? fcmToken.trim() : null;
//    }

    public void deviceId(String deviceId) {
        this.deviceId = deviceId != null ? deviceId.trim() : null;
    }
    public void hospitalId(String hospitalId) {
        this.deviceId = hospitalId != null ? hospitalId.trim() : null;
    }
}

