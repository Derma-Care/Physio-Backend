package com.AdminService.dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WardBoyDTO {

    private String wardBoyId;  
    private String fullName;
    private String clinicId;
    private String hospitalName;
    private String branchId;
    private String branchName;
    private String dateOfBirth;
    private String contactNumber;
    private String governmentId;
    private String dateOfJoining;
    private String department;
    private String profilePicture;
    private BankAccountDetails bankAccountDetails;
    private String medicalFitnessCertificate;
    private String emailId;
    private String basicHealthFirstAidTrainingCertificate;
    private String previousEmploymentHistory;
    private String policeVerification;
    private String policeVerificationCertificate;
    private String gender;
    private String workExprience;
    private String shiftTimingsOrAvailability;
    private Address address;
    private String emergencyContact;
    private String userName;
    private String password;
    private String role;
    private Map<String, List<String>> permissions;
}
