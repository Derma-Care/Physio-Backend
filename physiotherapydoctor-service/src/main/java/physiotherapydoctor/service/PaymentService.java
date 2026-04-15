package physiotherapydoctor.service;


import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.entity.PaymentRecord;

public interface PaymentService {

    PaymentRecord createOrUpdatePayment(PaymentRequest request);

    PaymentRecord getByBookingId(String bookingId);
}
