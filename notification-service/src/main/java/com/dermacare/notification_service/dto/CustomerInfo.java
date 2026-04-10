package com.dermacare.notification_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerInfo {
	
	private String mobileNumber;
	private String customerId;
    private String patientId;

}
