package com.pharmacyManagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmacyManagement.enums.RefundMode;
import com.pharmacyManagement.enums.ReturnStatus;
import com.pharmacyManagement.enums.ReturnType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SalesReturnResponse {

    private String returnNo;
    private String originalBillNo;
    private PatientDetailsDto patientDetails;
    private ReturnDetailsDto returnDetails;
    private List<ReturnItemDto> items;
    private SummaryDto summary;
    private ReturnStatus status;
    private String clinicId;
    private String branchId;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PatientDetailsDto {
        private String fileNo;
        private String patientName;
        private String mobileNo;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReturnDetailsDto {
        private ReturnType returnType;
        private RefundMode refundMode;
        private LocalDateTime returnDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReturnItemDto {
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SummaryDto {
        private BigDecimal totalReturnAmount;
        private BigDecimal totalDiscount;
        private BigDecimal totalGST;
        private BigDecimal netRefundAmount;
    }
}
