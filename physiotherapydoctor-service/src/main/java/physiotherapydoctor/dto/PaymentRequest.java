package physiotherapydoctor.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    private String clinicId;
    private String branchId;
    private String bookingId;
    private String patientId;

    private String doctorId;
    private String doctorName;

    private String therapistId;
    private String therapistName;
    private String therapistRecordId;

    private String serviceType;

    private Double amount;
    private String paymentMode;
    private String paymentType;

    private Double discountAmount;
    private String discountIssuedBy;

    // ✅ STRING BASED LEVEL
    private String paymentLevel; // PACKAGE / PROGRAM / THERAPY / EXERCISE / SESSION

    private PaymentTarget paymentTarget; //pakageId = all sessins are paid(sessionSatus)   if programId=only that program session are paid   if therpyId=that therpy session paid if exercise session paid ,session just paid 

    private String paymentDate;

    // FIRST TIME ONLY
    private String sessionStartDate;
    private Integer totalSessionCount;

    private List<TherapyWithSessions> therapyWithSessions;
}