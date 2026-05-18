package physiotherapydoctor.service;

import java.util.List;

import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.response.PaymentRecordResponse;
import physiotherapydoctor.entity.PaymentRecord;

public interface PaymentService {

    PaymentRecordResponse createPayment(PaymentRequest req);

    PaymentRecordResponse updatePayment(PaymentRequest req);

    PaymentRecordResponse getByBookingId(String bookingId);

    void deleteByBookingId(String bookingId);

    void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);

//	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
//			String therapistRecordId, String exerciseId);

	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
			String therapistRecordId);

	List<PaymentRecordResponse> findByClinicIdAndBranchId(String clinicId, String branchId);
	
	Response getCompletedTherapyRecord(
	        String clinicId,
	        String branchId,
	        String therapistRecordId,
	        String sessionId);


//	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
//			String therapistRecordId, String exerciseId);


}


















//package physiotherapydoctor.service;
//
//
//import physiotherapydoctor.dto.PaymentRequest;
//import physiotherapydoctor.dto.Response;
//import physiotherapydoctor.entity.PaymentRecord;
//public interface PaymentService {
//
//    PaymentRecord createPayment(PaymentRequest request);
//
//    PaymentRecord updatePayment(PaymentRequest request);
//
//    PaymentRecord getByBookingId(String bookingId);
//
//    void deleteByBookingId(String bookingId);
//
//	void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);
//
//	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
//			String therapistRecordId, String exerciseId);
//}