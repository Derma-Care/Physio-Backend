package com.pharmacyManagement.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderDTO {

    private String orderId;


    private ClinicDTO clinic;
    private BranchDTO branch;
    private SuppliersDTO supplier;

    private int expectedDeliveryDays;
    private String expectedDeliveryDate;


    private String overallStatus;  
    private String overallReason;

    private List<StatusHistoryDTO> statusHistory; 
    private List<ProductDTO> products;

   
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}