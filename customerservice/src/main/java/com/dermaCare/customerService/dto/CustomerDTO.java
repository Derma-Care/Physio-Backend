package com.dermaCare.customerService.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDTO {
 
	private String customerId;
    @NotBlank(message = "Full name is required and cannot start or end with spaces.")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Full name must contain only letters and spaces.")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
    
    @NotBlank(message = "Gender is required and cannot start or end with spaces.")
    private String gender;
    
    private String deviceId;
    @Email(message = "Email should be valid.")
    @NotBlank(message = "Email is required and cannot start or end with spaces.")
    private String emailId;

    private String referCode;
    																																																																																																																													
    @NotBlank(message = " DateOfBirth is required and cannot start or end with spaces.")
    private String dateOfBirth;
 
    
    public void trimFields() {
        this.fullName = this.fullName != null ? this.fullName.trim() : null;
        this.gender = this.gender != null ? this.gender.trim() : null;
        this.emailId = this.emailId != null ? this.emailId.trim() : null;

}}
