package com.clinicadmin.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerOnbordingDTO {
    private String id;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Gender is required")
    private String gender;

    
    private String dateOfBirth;

    private String age;


    private Address address;

    @NotBlank(message = "Hospital ID is required")
    private String hospitalId;

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    @NotBlank(message = "Branch ID is required")
    private String branchId;

    private String customerId;
    private String patientId;
    private String deviceId;
    private String referralCode;
    private String referredBy;
    private String userName;
    private String password;
    
    private String createdBy;
    
    private String createdAt;
    
    private String updatedDate;
}
