package com.pharmacyManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpSalesResponse {

	    private String id;
	    private String billNo;
	    private String billDate;
	    private String billTime;
	    private String visitType;
	    private String opNo;
	    private String payCategory;
	    private String patientName;
	    private String mobile;
	    private Integer age;
	    private String sex;
	    private String consultingDoctor;
	    private Boolean includeReturns;
	    private List<OpMedicineDTO> medicines;

	    // ── Computed summary fields (excluding GST) ──
	    private Double totalAmt;          // sum of all (qty × rate)
	    private Double avgDiscPercent;    // average discount %
	    private Double totalDiscAmt;      // sum of all discounts
	    private Double netAmount;         // totalAmt - totalDiscAmt (NO GST)
	    private Double finalTotal;        // netAmount only (GST excluded from payment calc)

	    // ── Payment tracking ──
	    private Double currentPaymentAmount;   // amount paid in THIS transaction
	    private Double alreadyPaidAmount;      // sum of all previous payments
	    private Double totalPaidAmount;        // alreadyPaidAmount + currentPaymentAmount
	    private Double dueAmount;             // finalTotal - totalPaidAmount

	    /** Full payment history, newest first */
	    private List<PaymentEntryDTO> paymentHistory;

	    private String clinicId;
	    private String branchId;   
	    private LocalDateTime createdAt;	   
	    private LocalDateTime updatedAt;
    
}