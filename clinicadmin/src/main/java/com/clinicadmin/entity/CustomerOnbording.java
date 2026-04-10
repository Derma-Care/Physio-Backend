package com.clinicadmin.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.clinicadmin.dto.Address;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Customers_Data")
public class CustomerOnbording {
	@Id
	private String id;
	private String mobileNumber;
	private String email;
	private String fullName;
	private String gender;
	private String dateOfBirth;
	private String age;
	private Address address;
	private String hospitalId;
	private String hospitalName;
	private String branchId;
	private String customerId;
	private String patientId;
	private String deviceId;
	private String referralCode;
	private String referredBy;
    private String createdBy;
    
    private String createdAt;
    
    private String updatedDate;

}
