package com.pharmacyManagement.entity;

import lombok.Data;

@Data
public class ReturnItem {

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