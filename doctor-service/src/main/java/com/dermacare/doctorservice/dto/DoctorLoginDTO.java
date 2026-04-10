package com.dermacare.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorLoginDTO {
	
    private String userName;
    private String password;
    private String hospitalId;
//    private String fcmToken;
	private String deviceId;
	private String doctorId;


    public void setusername(String username) {
        this.userName = username != null ? username.trim() : null;
    }

    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }
    
//    public void setFcmToken(String fcmToken) {
//		this.fcmToken = fcmToken != null ? fcmToken.trim() : null;
//	}

	public void doctorId(String doctorId) {
		this.deviceId = deviceId != null ? deviceId.trim() : null;
	}
	public void hospitalId(String hospitalId) {
		this.hospitalId = hospitalId != null ? hospitalId.trim() : null;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId != null ? deviceId.trim() : null;
	}


}
