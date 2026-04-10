package com.pharmacyManagement.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "return_bills")
public class ReturnBill {

    @Id
    private String receiptNo; // ✅ PRIMARY KEY

    private String billNo;

    private String supplierName;
    private String supplierId;

    private String returnType;
    private String refundMode;

    private double totalAmount;

    private String clinicId;
    private String branchId;

    private LocalDateTime date;
    private String createdBy;

    private List<ReturnItem> items;
}