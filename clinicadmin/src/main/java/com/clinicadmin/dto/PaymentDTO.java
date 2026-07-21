package com.clinicadmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class PaymentDTO {

    private String paymentMode;
    private String transactionId;

    private Double paidAmount;
    private Double dueAmount;

    private String remarks;
}