package com.pharmacyManagement.dto;

import lombok.Data;

@Data
public class ReturnItemDTO {

    private String productId;
    private String productName;

    private String batchNo;

    private int returnQty;

    private double netRate;
    private double mrp;

    private double returnAmount;

    private String reason;

    private int availableStock;
}