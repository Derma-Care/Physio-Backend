package com.pharmacyManagement.entity;

import lombok.*;

import java.math.BigDecimal;

/**
 * Embedded sub-document inside SalesReturn.
 * No @Document annotation — stored inside the parent document array.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnItem {

    private String medicineId;
    private String medicineName;
    private String batchNo;
    private Integer soldQty;
    private Integer returnQty;
    private BigDecimal rate;
    private BigDecimal discountPercent;
    private BigDecimal gstPercent;
    private BigDecimal netAmount;
}
