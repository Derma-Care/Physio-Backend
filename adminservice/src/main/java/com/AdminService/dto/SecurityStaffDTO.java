package com.AdminService.dto;


import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SecurityStaffDTO {

    private String securityStaffId;

    private String clinicId;
    private String hospitalName;
    private String branchId;
    private String branchName;
    private String role;

    private String fullName;

    private String dateOfBirth;

    private String gender;

    private String contactNumber;

    private String govermentId;

    private String dateOfJoining;

    private String department;

    private Address address;

    private BankAccountDetails bankAccountDetails;

    private String policeVerification;
    
    private String policeVerificationCertificate;

    private String medicalFitnessCertificate;

    private String profilePicture;

    private String emailId;

    private String traningOrGuardLicense;

    private String previousEmployeeHistory;
    private Map<String, List<String>> permissions;
    private String shiftTimingsOrAvailability;

    private String userName;
    private String password;
}
