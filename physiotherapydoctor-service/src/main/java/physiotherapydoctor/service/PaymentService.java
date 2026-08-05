package physiotherapydoctor.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.UpdateSessionBookingDTO;
import physiotherapydoctor.dto.response.PaymentRecordResponse;
import physiotherapydoctor.util.RevenueResponse;

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

	Response updateSessionBookingDetails(UpdateSessionBookingDTO dto);
}


















