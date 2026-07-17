package com.clinicadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClinicLoginRequestDTO {

	private String userName;
	private String role;
	private String password;
	private String fcmToken;
}
