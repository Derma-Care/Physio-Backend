package com.pharmacyManagement.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntryDTO {
    private Double amountPaid;
    private Double alreadyPaid;
    private Double totalPaidSoFar;
    private Double dueAmount;
    private LocalDateTime paidAt;
}