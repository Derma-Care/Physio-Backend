package com.pharmacyManagement.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import com.pharmacyManagement.enums.RefundMode;
import com.pharmacyManagement.enums.ReturnStatus;
import com.pharmacyManagement.enums.ReturnType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "sales_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturn {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("return_no")
    private String returnNo;

    @Indexed
    @Field("original_bill_no")
    private String originalBillNo;

    // ── Patient Details (embedded) ────────────────────────────────────────
    @Field("file_no")
    private String fileNo;

    @Field("patient_name")
    private String patientName;

    @Indexed
    @Field("mobile_no")
    private String mobileNo;

    // ── Return Details ────────────────────────────────────────────────────
    @Field("return_type")
    private ReturnType returnType;

    @Field("refund_mode")
    private RefundMode refundMode;

    @Field("return_date")
    private LocalDateTime returnDate;

    // ── Summary ───────────────────────────────────────────────────────────
    @Field("total_return_amount")
    private BigDecimal totalReturnAmount;

    @Field("total_discount")
    private BigDecimal totalDiscount;

    @Field("total_gst")
    private BigDecimal totalGst;

    @Field("net_refund_amount")
    private BigDecimal netRefundAmount;

    // ── Items embedded (no separate collection needed in MongoDB) ─────────
    @Builder.Default
    private List<SalesReturnItem> items = new ArrayList<>();

    // ── Multi-tenancy ─────────────────────────────────────────────────────
    @Indexed
    @Field("clinic_id")
    private String clinicId;

    @Indexed
    @Field("branch_id")
    private String branchId;

    @Builder.Default
    private ReturnStatus status = ReturnStatus.ACTIVE;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt;
}
