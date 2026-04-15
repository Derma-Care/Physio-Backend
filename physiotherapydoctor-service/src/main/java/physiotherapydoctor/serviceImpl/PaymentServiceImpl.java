package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.*;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.service.PaymentService;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repo;

    @Override
    public PaymentRecord createOrUpdatePayment(PaymentRequest req) {

        Optional<PaymentRecord> optional = repo.findByBookingId(req.getBookingId());

        if (optional.isEmpty()) {
            return createFirstPayment(req);
        } else {
            return addPayment(optional.get(), req);
        }
    }

    @Override
    public PaymentRecord getByBookingId(String bookingId) {
        return repo.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    // ✅ FIRST PAYMENT
    private PaymentRecord createFirstPayment(PaymentRequest req) {

        PaymentRecord record = new PaymentRecord();

        record.setClinicId(req.getClinicId());
        record.setBranchId(req.getBranchId());
        record.setBookingId(req.getBookingId());
        record.setPatientId(req.getPatientId());

        // ✅ NEW FIELDS
        record.setDoctorId(req.getDoctorId());
        record.setDoctorName(req.getDoctorName());
        record.setTherapistId(req.getTherapistId());
        record.setTherapistName(req.getTherapistName());

        record.setTherapistRecordId(req.getTherapistRecordId());

        double total = calculateTotal(req.getTherapyWithSessions());

        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
        double finalAmount = total - discount;

        record.setTotalAmount(total);
        record.setDiscountAmount(discount);
        record.setFinalAmount(finalAmount);

        double paid = req.getAmount() != null ? req.getAmount() : 0;

        double balance = finalAmount - paid;

        // ✅ prevent negative balance
        if (balance < 0) {
            balance = 0;
        }

        record.setTotalPaid(paid);
        record.setBalanceAmount(balance);
        
//        record.setTotalPaid(req.getAmount());
//        record.setBalanceAmount(finalAmount - req.getAmount());

        record.setPaymentStatus(getStatus(record));

        record.setTherapyWithSessions(req.getTherapyWithSessions());

        record.setPaymentHistory(new ArrayList<>());
        record.getPaymentHistory().add(buildHistory(req));

        return repo.save(record);
    }

    // ✅ ADD PAYMENT
    private PaymentRecord addPayment(PaymentRecord record, PaymentRequest req) {

        if (req.getDiscountAmount() != null && req.getDiscountAmount() > 0) {
            throw new RuntimeException("Discount already applied");
        }

        double newPaid = record.getTotalPaid() + req.getAmount();

        record.setTotalPaid(newPaid);
        record.setBalanceAmount(record.getFinalAmount() - newPaid);

        record.setPaymentStatus(getStatus(record));

        applyPaymentLevel(record, req);

        record.getPaymentHistory().add(buildHistory(req));

        return repo.save(record);
    }

    // ✅ STATUS
    private String getStatus(PaymentRecord r) {

        if (r.getTotalPaid() == 0) return "UNPAID";
        if (r.getTotalPaid() < r.getFinalAmount()) return "PARTIAL";
        return "PAID";
    }

    // ✅ TOTAL CALCULATION (NEW MODEL)
    private double calculateTotal(List<TherapyWithSessions> data) {

        if (data == null) return 0;

        double total = 0;

        for (TherapyWithSessions pkg : data) {

            if (pkg.getPrograms() == null) continue;

            for (Program prog : pkg.getPrograms()) {

                double programTotal = 0; // ✅ ADD THIS

                if (prog.getTherapyData() == null) continue;

                for (TherapyData therapy : prog.getTherapyData()) {

                    double therapyTotal = 0;

                    if (therapy.getExercises() == null) continue;

                    for (TherapyExercise ex : therapy.getExercises()) {

                        double exerciseTotal = 0;

                        if (ex.getTotalExercisePrice() != null) {
                            exerciseTotal = ex.getTotalExercisePrice();
                        } else if (ex.getPricePerSession() != null && ex.getNoOfSessions() != null) {
                            exerciseTotal = ex.getPricePerSession() * ex.getNoOfSessions();
                        }

                        ex.setTotalExercisePrice(exerciseTotal);
                        therapyTotal += exerciseTotal;
                    }

                    therapy.setTotalPrice(therapyTotal);

                    // ✅ ADD THIS
                    programTotal += therapyTotal;
                }

                // ✅ VERY IMPORTANT
                prog.setTotalPrice(programTotal);

                total += programTotal; // ✅ update correctly
            }
            }
        return total;
    }

    // ✅ HISTORY
    private PaymentHistory buildHistory(PaymentRequest req) {

        PaymentHistory h = new PaymentHistory();
        h.setAmount(req.getAmount());
        h.setPaymentMode(req.getPaymentMode());
        h.setPaymentType(req.getPaymentType());
        h.setPaymentDate(req.getPaymentDate());
        h.setDiscountAmount(req.getDiscountAmount());
        h.setDiscountIssuedBy(req.getDiscountIssuedBy());

        return h;
    }

    // ✅ SESSION PAYMENT (NEW MODEL)
    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {

        if ("SESSION".equalsIgnoreCase(req.getPaymentLevel())) {
            paySessions(record, req.getPaymentTarget().getSessionIds());
        }
    }

    private void paySessions(PaymentRecord record, List<String> sessionIds) {

        if (sessionIds == null || sessionIds.isEmpty()) return;

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {

                        ex.setPaymentStatus("PAID");
                        ex.setTotalExercisePrice(0.0);
                    }
                }
            }
        }
    }
}