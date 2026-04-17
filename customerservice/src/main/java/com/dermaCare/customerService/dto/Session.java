package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Session {

	private String sessionId;
	private Integer sessionNo;
	private String date;

	private String status;
	private String paymentStatus;
}