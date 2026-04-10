package com.dermacare.notification_service.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceDropAlertDto {

	private String id;
	private String clinicId;
	private String branchId;
	private String title;
	private String body;
	private String image;
	private boolean sendAll;
	private List<String> tokens;
	private List<Map<String,CustomerInfo>> customerData;
	private LocalDateTime localDateTime;

	
	public boolean getSendAll() {
		return sendAll;
	}
}
