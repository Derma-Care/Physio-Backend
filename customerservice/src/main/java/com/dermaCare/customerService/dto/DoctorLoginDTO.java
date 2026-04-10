
package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorLoginDTO {
	private String username;
	private String password;
	private String fcmToken;
	private String deviceId;
	private String doctorId;
	private String hospitalId;

	public void setDoctorMobileNumber(String doctorMobileNumber) {
		this.username = username != null ? username.trim() : null;
	}

	public void setPassword(String password) {
		this.password = password != null ? password.trim() : null;
	}

	public void setFcmToken(String fcmToken) {
		this.fcmToken = fcmToken != null ? fcmToken.trim() : null;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId != null ? deviceId.trim() : null;
	}

	public void setDoctorId(String doctorId) {
		this.doctorId = doctorId != null ? doctorId.trim() : null;
	}

	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId != null ? hospitalId.trim() : null;
	}
}
