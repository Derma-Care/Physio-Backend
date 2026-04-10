package com.pharmacyManagement.dto;

import lombok.Data;

@Data
public class PaymentDetails {
	private String paymentMode;
	private double paidAmount;
	private double previousAdjustment;
	private double dueAmount;
	private String duePaidBillNo;
	
}
