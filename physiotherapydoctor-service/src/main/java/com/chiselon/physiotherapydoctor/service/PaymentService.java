package com.chiselon.physiotherapydoctor.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.chiselon.physiotherapydoctor.dto.PaymentRequest;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.dto.response.PaymentRecordResponse;
import com.chiselon.physiotherapydoctor.util.RevenueResponse;

public interface PaymentService {

    PaymentRecordResponse createPayment(PaymentRequest req);

    PaymentRecordResponse updatePayment(PaymentRequest req);

    PaymentRecordResponse getByBookingId(String bookingId);

    void deleteByBookingId(String bookingId);

    void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);

//	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
//			String therapistRecordId, String exerciseId);

	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
			String therapistId,	String therapistRecordId);

	List<PaymentRecordResponse> findByClinicIdAndBranchId(String clinicId, String branchId);
	
	Response getCompletedTherapyRecord(
	        String clinicId,
	        String branchId,
	        String therapistRecordId,
	        String sessionId);


//	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
//			String therapistRecordId, String exerciseId);
	
	public int getTodaySessionCount(String clinicId,
            String branchId,
            String therapistId);

	ResponseEntity<RevenueResponse> getRevenueManagement(
			String clinicId,
			String branchId,
			String number);

	ResponseEntity<RevenueResponse> getRevenueManagementByDateRange(
			String clinicId,
			String branchId,
			String startDate,
			String endDate);

	ResponseEntity<Response> getRevenueSummary(
			String clinicId,
			String branchId);
}


















