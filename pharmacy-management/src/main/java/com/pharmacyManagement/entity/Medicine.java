package com.pharmacyManagement.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "medicines")
public class Medicine {

    @Id
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