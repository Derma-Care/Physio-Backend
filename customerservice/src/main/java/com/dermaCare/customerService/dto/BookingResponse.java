package com.dermaCare.customerService.dto;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BookingResponse {
	
	private String bookingId;
	private String bookingFor;
	private String name;
	private String relation;
	private String patientMobileNumber;
	private String patientId;
	private String visitType;
	private Integer freeFollowUpsLeft;
	private Integer freeFollowUps;
	private String patientAddress;
	private String age;
	private String gender;
	private String mobileNumber;
	private String customerId;
	private String consultationExpiration;
	private String customerDeviceId;
	private String problem;
	private String symptomsDuration;
	private String clinicId;
	private String clinicDeviceId;
	private String clinicName;
	private String branchId;
	private String branchname;
	private String doctorId;
	private String doctorName;
	private String doctorMobileDeviceId;
	private String doctorWebDeviceId;
	private String subServiceId;
	private String subServiceName;
	private String serviceDate;
	private String servicetime;
	private String consultationType;
	private double consultationFee;
	private Integer visitCount;
	private String channelId;
	private String reasonForCancel;
	private String notes;
	private List<ReportsDtoList> reports;
	private String BookedAt;
	private List<String> status;
	private String currentStatus;
	private double totalFee;
	private List<String> attachments;
	private String consentFormPdf;
	private List<String> prescriptionPdf;
	private String doctorRefCode;
	private String paymentType;
//	private String followupStatus;
	private Integer totalSittings;
	private Integer pendingSittings;
	private Integer takenSittings;
	private Integer currentSitting;
	private String followupDate;
	private String foc;  /////paid , unpaid	
    // ✅ Treatments info
    private TreatmentResponseDTO treatments;
    private String bodyPartId;
	private String bodyPartName;
	private String partImage;
	private List<QuestionsDTO> questions;
	private Map<String,List<TheraphyAnswersDTO>> theraphyAnswers;	
	private List<String> parts;
	private double partAmount;
	private double dueAmount;
	private String paymentStatus;       ///session related payment
	private String focReason;
	private String previousInjuries;
	private String currentMedications;
	private String allergies;
	private String occupation;
	private String insuranceProvider;
	private String policyNumber;
	private List<String> activityLevels;
	
}
