package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
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

	private Double amount;
	private String paymentMode;
	private String paymentType;

	private Double discountAmount;
	private String discountIssuedBy;

	private String paymentLevel;
	private PaymentTarget paymentTarget;

	private String paymentDate;

	private List<TherapyWithSessions> therapyWithSessions;
}