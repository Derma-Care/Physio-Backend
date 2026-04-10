package com.pharmacyManagement.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.pharmacyManagement.dto.SupplierContactDetails;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "supplier")
public class Supplier {
	@Id
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
