package physiotherapydoctor.service;


import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.entity.PaymentRecord;
public interface PaymentService {

    PaymentRecord createPayment(PaymentRequest request);

    PaymentRecord updatePayment(PaymentRequest request);

    PaymentRecord getByBookingId(String bookingId);

    void deleteByBookingId(String bookingId);

	void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId);

	Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
			String therapistRecordId, String exerciseId);
}