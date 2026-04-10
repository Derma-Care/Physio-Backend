package com.pharmacyManagement.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicineDTO {

    private String id;
    private String barcode;

    private String productName;
    private String brandName;
    private String category;
    private String composition;
    private String manufacturer;
    private String packSize;
    private String hsnCode;

    private double gstPercent;
    private double mrp;
    private double minStock;

    private String status;
    

    private String clinicId;
    private String branchId;

    private LocalDateTime createdAt;
}