package com.AdminService.dto;


import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
 
	private String customerId;
    @NotBlank(message = "Full name is required and cannot start or end with spaces.")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Full name must contain only letters and spaces.")
    private String fullName;

    private String mobileNumber;
    @NotBlank(message = "Gender is required and cannot start or end with spaces.")
    @Pattern(regexp = "^(male|female|other)$", message = "Gender must be either 'male', 'female', or 'other'.")
    private String gender;
    
    private String fcm;
    @Email(message = "Email should be valid.")
    @NotBlank(message = "Email is required and cannot start or end with spaces.")
    private String emailId;

    private String referCode;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @NotBlank(message = " DateOfBirth is required and cannot start or end with spaces.")
    private String dateOfBirth;
 
    
    public void trimFields() {
        this.fullName = this.fullName != null ? this.fullName.trim() : null;
        this.gender = this.gender != null ? this.gender.trim() : null;
        this.emailId = this.emailId != null ? this.emailId.trim() : null;

}}
