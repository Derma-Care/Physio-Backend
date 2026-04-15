package physiotherapydoctor.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import physiotherapydoctor.dto.PaymentHistory;
import physiotherapydoctor.dto.TherapyWithSessions;

@Document(collection = "payments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRecord {

    @Id
    private String id;

    private String clinicId;
    private String branchId;
    private String bookingId;
    private String patientId;

    // ✅ ADD THESE
    private String doctorId;
    private String doctorName;

    private String therapistId;
    private String therapistName;

    private String therapistRecordId;

    private double totalAmount;
    private double finalAmount;
    private double totalPaid;
    private double discountAmount;
    private double balanceAmount;

    private String paymentStatus;

    private List<PaymentHistory> paymentHistory;

    private List<TherapyWithSessions> therapyWithSessions;
}