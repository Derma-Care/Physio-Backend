package com.dermaCare.customerService.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BranchInfo {
		
	private String serviceName;
	private String subServiceName;
	private double subServicePrice;
	private double price;
	private double discountedCost; 
	private double taxAmount;
	private byte discountPercentage;
	private double consultationFee;
	private String hospitalId;
	private String hospitalName;
	private String hospitalLogo;
	private String city;
	private boolean recommanded;
	private double hospitalOverallRating;
	private String website;
	private String walkthrough;
	private List<BranchDTO> branches;


}
