package com.pharmacyManagement.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.pharmacyManagement.enums.RefundMode;
import com.pharmacyManagement.enums.ReturnType;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesReturnCreateResponse {

    private String returnNo;
    private String originalBillNo;
    private String fileNo;
    private String patientName;
    private String mobileNo;
    private ReturnType returnType;
    private RefundMode refundMode;
    private BigDecimal netRefund;
    private LocalDateTime createdAt;
}
