package com.pharmacyManagement.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.pharmacyManagement.enums.RefundMode;
import com.pharmacyManagement.enums.ReturnStatus;
import com.pharmacyManagement.enums.ReturnType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesReturnSummaryResponse {

    private String returnNo;
    private String fileNo;
    private String patientName;
    private String mobileNo;
    private String originalBillNo;
    private ReturnType returnType;
    private RefundMode refundMode;
    private BigDecimal netRefundAmount;
    private LocalDate returnDate;
    private ReturnStatus status;
}
