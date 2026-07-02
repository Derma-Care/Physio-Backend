package com.dermacare.notification_service.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingResponse {

	private String bookingId;
	private String bookingFor;
	private String name;
	private String dob;
	private String patientMobileNumber;
	private String patientId;
	private String visitType; // 
	private String patientAddress;
	private String age;
	private String doctorDeviceId;
	private String subServiceName;
	private String gender;
	private String subServiceId;
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
	private String serviceDate;
	private String servicetime;
	private String consultationType;
	private Double consultationFee;
	private String reasonForCancel;
	private String BookedAt;
	private String status;
	private double totalFee;
	private String paymentType;
	private String followupDate;
	private String foc;
	private String focReason;

}
