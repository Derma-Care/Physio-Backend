package physiotherapydoctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import physiotherapydoctor.entity.PaymentRecord;

public interface PaymentRepository extends MongoRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByBookingId(String bookingId);

	List<PaymentRecord> findByTherapistRecordId(String therapistRecordId);

	Optional<PaymentRecord> findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistRecordId(String clinicId,
			String branchId, String bookingId, String patientId, String therapistRecordId);

	List<PaymentRecord> findByClinicIdAndBranchId(String clinicId, String branchId);
}