package com.clinicadmin.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "billing")
public class Billing {

    @Id
    private String billingId;

    private String clinicId;
    private String branchId;

    private Patient patient;

    private String doctorId;
    private String visitType;

    private LocalDate billDate;
    private LocalDate invoiceDate;

    private List<ServiceItem> services;

    private Payment payment;

    private AdditionalDetails additionalDetails;

    private String invoiceStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



   
}