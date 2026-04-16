package physiotherapydoctor.service;


import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.entity.PaymentRecord;
public interface PaymentService {

    PaymentRecord createPayment(PaymentRequest request);

    PaymentRecord updatePayment(PaymentRequest request);

    PaymentRecord getByBookingId(String bookingId);

    void deleteByBookingId(String bookingId);
}