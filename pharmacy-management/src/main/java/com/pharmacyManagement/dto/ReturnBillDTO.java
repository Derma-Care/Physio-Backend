package com.pharmacyManagement.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ReturnBillDTO {

    private String receiptNo;

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

    private List<ReturnItemDTO> items;
}