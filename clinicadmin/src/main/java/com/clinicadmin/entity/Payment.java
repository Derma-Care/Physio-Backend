package com.clinicadmin.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class Payment {

    private String paymentMode;
    private String transactionId;

    private Double paidAmount;
    private Double dueAmount;

    private String remarks;
}