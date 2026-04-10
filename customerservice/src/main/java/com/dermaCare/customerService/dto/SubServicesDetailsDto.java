package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubServicesDetailsDto {
	
	private String hospitalId;
	private String hospitalName;
	private String hospitalLogo;
	private String city;
	private boolean recommanded;
	private double hospitalOverallRating;
	private String website;
	private String walkthrough;
	private String serviceName;
	private String subServiceName;
	private double subServicePrice;
	private double price;
	private double discountedCost; 
	private double taxAmount;
	private byte discountPercentage;
	private double consultationFee;

}
