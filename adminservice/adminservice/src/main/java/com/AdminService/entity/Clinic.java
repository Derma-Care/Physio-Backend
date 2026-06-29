package com.AdminService.entity;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "clinics") // MongoDB collection
public class Clinic {
    @Id
    private String id;
    private String name;
    private String hospitalId;
    private String address;
    private String city;
    private String contactNumber;
    private String openingTime;
    private String closingTime;

    @Field(targetType = FieldType.BINARY)

    
    private byte[] hospitalLogo;
    private String emailAddress;
    private String website;
    private String licenseNumber;
    private String issuingAuthority;
 
    @Field(targetType = FieldType.BINARY)
    private byte[] contractorDocuments;

    @Field(targetType = FieldType.BINARY)
    private byte[] hospitalDocuments;

    private boolean recommended;
    private double hospitalOverallRating;

    // Basic Registration Certificates
    @Field(targetType = FieldType.BINARY)
    private byte[] clinicalEstablishmentCertificate;

    @Field(targetType = FieldType.BINARY)
    private byte[] businessRegistrationCertificate;

    // Clinic Type: Proprietorship, Partnership, LLP, Pvt Ltd
    private String clinicType;

    // Medicines Handling
    private String medicinesSoldOnSite;  // Yes / No

    @Field(targetType = FieldType.BINARY)
    private byte[] drugLicenseCertificate; // Only if medicinesSoldOnSite is true

    @Field(targetType = FieldType.BINARY)
    private byte[] drugLicenseFormType; // Form 20 or 21

    // Pharmacist info
    private String hasPharmacist; // Yes / No / NA

    @Field(targetType = FieldType.BINARY)
    private byte[] pharmacistCertificate;

    // Other Licenses
    @Field(targetType = FieldType.BINARY)
    private byte[] biomedicalWasteManagementAuth; // SPCB

    @Field(targetType = FieldType.BINARY)
    private byte[] tradeLicense; // Municipality

    @Field(targetType = FieldType.BINARY)
    
    private byte[] fireSafetyCertificate; // Local Fire Department

    @Field(targetType = FieldType.BINARY)
    private byte[] professionalIndemnityInsurance; // Insurance company

    @Field(targetType = FieldType.BINARY)
    private byte[] gstRegistrationCertificate;

    private String consultationExpiration;
    
    private String subscription;

    @Field(targetType = FieldType.BINARY)
    private List<byte[]> others;
    
    private int freeFollowUps;
    private double latitude;
    private double longitude;
    private String walkthrough;
    private int nabhScore;
    private String branch;
    
    private List<Branch> branches;
    private String role;    
    private Map<String, List<String>> permissions;

    private String instagramHandle;
    private String twitterHandle;
    private String facebookHandle;
	private String status ;
	private String createdAt;
	   
}
