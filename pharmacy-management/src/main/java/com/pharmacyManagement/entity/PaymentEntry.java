package com.pharmacyManagement.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

/**
 * Represents a single payment installment.
 * Stored as an embedded object inside OpSales.amountToBePaid list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntry {
    private Double amountPaid;       // amount paid in this installment
    private Double alreadyPaid;      // total paid before this installment
    private Double totalPaidSoFar;   // alreadyPaid + amountPaid
    private Double dueAmount; 
    private LocalDateTime paidAt;
}