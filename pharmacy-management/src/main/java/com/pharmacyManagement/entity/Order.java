package com.pharmacyManagement.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String orderId;

  
    private Clinic clinic;
    private Branch branch;
    private Suppliers supplier;

    private int expectedDeliveryDays;
    private String expectedDeliveryDate;


    private String overallStatus;
    private String overallReason;

    private List<StatusHistory> statusHistory;

 
    private List<Product> products;

    
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
}