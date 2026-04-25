package physiotherapydoctor.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import physiotherapydoctor.entity.PaymentRecord;

public interface PaymentRepository extends MongoRepository<PaymentRecord, String> {

    Optional<PaymentRecord> findByBookingId(String bookingId);

	Optional<PaymentRecord> findByTherapistRecordId(String therapistRecordId);
}