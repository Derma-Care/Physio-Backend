package com.dermacare.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class NotificationResponse {
	
	private String hospitalId;
	private String doctorId;
	private String notificationId;
	private String appointmentId;
	private String subServiceId;
	private String status;
	private String reasonForCancel;

}
