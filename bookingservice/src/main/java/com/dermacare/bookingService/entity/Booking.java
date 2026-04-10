package com.dermacare.bookingService.entity;

import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.dermacare.bookingService.dto.TreatmentResponseDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "Appointments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Booking  {
    
	@Id
    private String bookingId;
    private String bookingFor;
    private String relation;
    private String patientMobileNumber;
    private String patientAddress;
    private String patientId;
    private Integer freeFollowUpsLeft;
    private Integer freeFollowUps;
    private String followupDate;
    private String visitType;
    private String name;
    private String age;
    private String gender;
    private String mobileNumber;
    private String customerId;
    private String customerDeviceId;
    private String problem;
    private String symptomsDuration;
    private String clinicId;
    private String clinicName;
    private String branchId;
    private String branchname;
    private String clinicDeviceId;
    private String doctorId;
    private String doctorName;
    private String doctorDeviceId;
    private String doctorWebDeviceId;
    private String subServiceId;
    private String subServiceName;
    private String serviceDate;
    private String servicetime;
    private String consultationType;
    private double consultationFee;
    private String reasonForCancel;
    private String notes;
    private List<String> currentStatus;
	private String status;
    private List<ReportsList> reports;
    private String channelId;
    private String bookedAt;
    private Integer visitCount;
    private List<byte[]> attachments;
    private byte[] consentFormPdf;
    private List<byte[]> prescriptionPdf;
    private double totalFee;
	private String paymentType;
    private String doctorRefCode;
    private String consultationExpiration;
    private String followupStatus;
    private Integer sittings;
    private Integer totalSittings;
    private Integer pendingSittings;
    private Integer takenSittings;
    private Integer currentSitting;
    private String foc;
    // ------------------- NEW: Treatments and dates -------------------
    private TreatmentResponseDTO treatments; // treatmentName -> treatment details
    private String bodyPartId;
   	private String bodyPartName;
   	private byte[] partImage;
   	private Map<String,List<TheraphyAnswersEntity>> theraphyAnswers;
   	private List<String> parts;
   	private double partAmount;
   	private double dueAmount;
   	private String referredByType;
	private String referredByName;
	private String paymentStatus;
	private String previousInjuries;
	private String currentMedications;
	private String allergies;
	private String occupation;
	private String insuranceProvider;
	private String policyNumber;
	private List<String> activityLevels;
   	

    public Booking(Booking booking) {
        this.bookingId = booking.getBookingId();
        this.bookingFor = booking.getBookingFor();
        this.relation = booking.getRelation();
        this.patientMobileNumber = booking.getPatientMobileNumber();
        this.patientAddress = booking.getPatientAddress();
        this.patientId = booking.getPatientId();
        this.freeFollowUpsLeft = booking.getFreeFollowUpsLeft();
        this.freeFollowUps = booking.getFreeFollowUps();
        this.followupDate = booking.getFollowupDate();
        this.visitType = booking.getVisitType();
        this.name = booking.getName();
        this.age = booking.getAge();
        this.gender = booking.getGender();
        this.mobileNumber = booking.getMobileNumber();
        this.customerId = booking.getCustomerId();
        this.customerDeviceId = booking.getCustomerDeviceId();
        this.problem = booking.getProblem();
        this.symptomsDuration = booking.getSymptomsDuration();
        this.clinicId = booking.getClinicId();
        this.clinicName = booking.getClinicName();
        this.branchId = booking.getBranchId();
        this.branchname = booking.getBranchname();
        this.clinicDeviceId = booking.getClinicDeviceId();
        this.doctorId = booking.getDoctorId();
        this.doctorName = booking.getDoctorName();
        this.doctorDeviceId = booking.getDoctorDeviceId();
        this.doctorWebDeviceId = booking.getDoctorWebDeviceId();
        this.subServiceId = booking.getSubServiceId();
        this.subServiceName = booking.getSubServiceName();
        this.serviceDate = booking.getServiceDate();
        this.servicetime = booking.getServicetime();
        this.consultationType = booking.getConsultationType();
        this.consultationFee = booking.getConsultationFee();
        this.reasonForCancel = booking.getReasonForCancel();
        this.notes = booking.getNotes();
        this.reports = booking.getReports();
        this.channelId = booking.getChannelId();
        this.bookedAt = booking.getBookedAt();
        this.status = booking.getStatus();
        this.visitCount = booking.getVisitCount();
        this.attachments = booking.getAttachments();
        this.consentFormPdf = booking.getConsentFormPdf();
        this.prescriptionPdf = booking.getPrescriptionPdf();
        this.totalFee = booking.getTotalFee();
        this.doctorRefCode = booking.getDoctorRefCode();
        this.consultationExpiration = booking.getConsultationExpiration();
        this.followupStatus = booking.getFollowupStatus();
        this.totalSittings = booking.getTotalSittings();
        this.pendingSittings = booking.getPendingSittings();
        this.takenSittings = booking.getTakenSittings();
        this.currentSitting = booking.getCurrentSitting();
        this.treatments = booking.getTreatments();
    }
}
