package com.AdminService.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Document(collection = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
	@Id
    private String id; // MongoDB ObjectId 
	private String customerId;
    private String mobileNumber;
    private String fullName; // required 
    private String fcm;
    private String gender; // required
    @Indexed(unique = true)
    private String emailId;
    private String dateOfBirth;
    private String referCode;
}