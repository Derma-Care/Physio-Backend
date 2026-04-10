package com.dermacare.doctorservice.model;

import java.util.List;
import java.util.UUID;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medicine {
    private String id = UUID.randomUUID().toString(); 
    private String name;
    private String dose;
    private String duration;
    private String durationUnit;
    private String note;
    private String food;
    private String medicineType;
    private String remindWhen;
    private String others;
    private List<String> times;
    private String serialNumber;
    private String genericName;
    private String brandName;
    private String nameAndAddressOfTheManufacturer;
    private String batchNumber;
    private String dateOfManufacturing;
    private String dateOfExpriy;
    private String ManufacturingLicenseNumber;
    private String stock;
}
