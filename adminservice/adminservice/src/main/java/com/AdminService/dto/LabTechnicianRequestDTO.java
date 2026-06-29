// ✅ Request DTO
package com.AdminService.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabTechnicianRequestDTO {
	private String id;
	private String clinicId;
	private String hospitalName;
	private String branchId;
	private String branchName;
    private String fullName;

    private String gender;

    private String dateOfBirth;

    private String contactNumber;

    private String governmentId;

    private String qualificationOrCertifications;

    private String dateOfJoining;

    private String departmentOrAssignedLab;

    private String yearOfExperience;
    private String specialization;
    private String shiftTimingsOrAvailability;
    private Address address;
    private String profilePicture;

    private String emergencyContact;

    private BankAccountDetails bankAccountDetails;

    private String medicalFitnessCertificate;

    private String emailId;
    private String userName;
    private String password;
    
    private String role;
    
    private Map<String, List<String>> permissions;

    private String labLicenseOrRegistration;


    private String vaccinationStatus;

    private String previousEmploymentHistory;
    
   
}