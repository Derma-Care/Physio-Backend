package com.pharmacyManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupplierDTO {
	
	private String supplierId;
	private String supplierName;
	private String gstNumber; // GSTIN
	private String registrationNumber;
	private String cstNumber;
	private String tinNumber;
	private String form20B;
	private String form21B;
	private String address;
	private String city;
	private String area;
	private boolean nonLocalSupplier;
	private boolean active;
	private double previousAdjustment;
	private String duePaidBill; 
	private SupplierContactDetails contactDetails;
	private String userName;
	private String password;
	
	private String clinicId;
	private String branchId;
}
