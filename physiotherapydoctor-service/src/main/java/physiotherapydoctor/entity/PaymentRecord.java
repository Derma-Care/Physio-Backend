package physiotherapydoctor.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import physiotherapydoctor.dto.PaymentHistory;
import physiotherapydoctor.dto.PaymentTarget;
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

	private String doctorId;
	private String doctorName;

	private String therapistId;
	private String therapistName;
	private String therapistRecordId;

	private String serviceType;
	private String overallSatatus;
	// 💰 SUMMARY
	private double totalAmount;
	private double discountAmount;
	private double finalAmount;

	private double totalPaid;
	private double balanceAmount;

	private String paymentStatus;

	// 📅 SESSION
	private String sessionStartDate;
	private int totalSessionCount;

	private int noOfSessionCompletedCount;
	private boolean noOfSessionCompletedStatus;

	private boolean sessionTableCreatedStatus;

	// 🧾 HISTORY
	private List<PaymentHistory> paymentHistory;

	// 🌳 DATA
	private List<TherapyWithSessions> therapyWithSessions;

	
}