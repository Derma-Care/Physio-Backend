package com.dermacare.bookingService.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {

	private String sessionId;
	private Integer sessionNo;
	private String date;

	private String status;
	private String paymentStatus;
	private String exerciseId;
    private String exerciseName;
    
    private String bookingStatus;
    private String slot;
}

