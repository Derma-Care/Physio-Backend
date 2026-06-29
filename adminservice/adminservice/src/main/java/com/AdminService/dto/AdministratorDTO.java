package com.AdminService.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdministratorDTO {

    private String id;
    private String adminId;
    private String clinicId;
    private String branchId;
    private String branchName;
    private String hospitalName;
    private String fullName;
    private String gender;
    private String dateOfBirth;
    private String contactNumber;
    private String emailId;
    private String governmentId;
    private String qualificationOrCertifications;
    private String dateOfJoining;
    private String department;
    private String yearOfExperience;
    private String role;
    private Address address;
    private String emergencyContact;
    private String profilePicture;
    private BankAccountDetails bankAccountDetails;
    private Map<String, List<String>> permissions;
    private String userName;
    private String password;
}