package com.dermacare.notification_service.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "priceDropAlertNotifications")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceDropAlertEntity {
	
	@Id
	private String id;
	private String clinicId;
	private String branchId;
	private String title;
	private String body;
	private byte[] image;
	private boolean sendAll;
	private List<String> tokens;
	private String customerName;
	private String mobileNumber;
	private LocalDateTime localDateTime;


}
