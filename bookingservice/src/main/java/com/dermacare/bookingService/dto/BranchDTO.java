package com.dermacare.bookingService.dto;

import lombok.Data;

@Data
public class BranchDTO {

    private String clinicId;
    private String hospitalName;
    private String branchId;
    private String branchName;
    private String address;
    private String contactNumber; // WhatsApp
    private String email;

    private String latitude;
    private String longitude;
}