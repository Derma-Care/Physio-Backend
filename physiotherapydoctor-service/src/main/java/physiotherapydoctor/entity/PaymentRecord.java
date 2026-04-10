package physiotherapydoctor.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import physiotherapydoctor.dto.PaymentHistory;
import physiotherapydoctor.dto.TherapyWithSessions;

@Document(collection = "payments")
@Data
public class PaymentRecord {

    @Id
    private String id;

    private String clinicId;
    private String branchId;
    private String bookingId;
    private String patientId;
    private String therapistRecordId;

    private double totalAmount;
    private double finalAmount;
    private double totalPaid;
    private double discountAmount;
    private double balanceAmount;

    private String paymentStatus;

    private int totalSessionCount;
    private long completedSessionCount;
    private boolean sessionWarningFlag;

    private List<PaymentHistory> paymentHistory;

    private List<TherapyWithSessions> therapyWithSessions;
}