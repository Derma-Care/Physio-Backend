package com.pharmacyManagement.dto;

import lombok.Data;

@Data
public class ExpiryMedicineReportDTO {
	
	private String medicineId;
    private String medicineName;
    private String batchNumber;
    private String expiryDate;
    private String status;
    private double quantity;
}