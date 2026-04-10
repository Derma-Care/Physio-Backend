package com.dermacare.category_services.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubServicesDto {

	private String hospitalId;

	private String subServiceId;

	private String subServiceName;

	private String serviceId;

	private String serviceName;

	private String categoryName;

	private String categoryId;

	private String viewDescription;

	private String subServiceImage;

	private String status;

	private String minTime;

	private List<Map<String, List<String>>> preProcedureQA;
	private List<Map<String, List<String>>> procedureQA;
	private List<Map<String, List<String>>> postProcedureQA;

	private double price;

	private byte discountPercentage;

	private byte taxPercentage;

	private byte platformFeePercentage;

	private double discountAmount;

	private double taxAmount;

	private double platformFee;

	private byte gst;

	private double gstAmount;

	private double consultationFee;

	private double discountedCost; // price - discount Amount

	private double clinicPay; // Price - platformFee

	private double finalCost; // taxAmount + discounedCost

	private String consentFormType; // Generic or procedureConsent (1,2)

}
