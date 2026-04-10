package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class PaymentRequest {

    private String clinicId;
    private String branchId;
    private String bookingId;
    private String patientId;
    private String therapistRecordId;

    private double paidAmount;
    private double discountAmount;

    private String paymentMode;
    private String paymentType;
    private String discountIssuedBy;

    private String transactionId;

    private List<TherapyWithSessions> therapyWithSessions;
}
