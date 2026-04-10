package com.dermacare.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationToCustomer {
    
	private String message;
	private String hospitalName;
	private String doctorName;
	private String serviceName;
	private String serviceDate;
	private String serviceTime;
	private String consultationType;
	private double consultationFee;
	private double serviceFee;
}
