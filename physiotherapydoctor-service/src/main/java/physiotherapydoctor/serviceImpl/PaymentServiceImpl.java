package physiotherapydoctor.serviceImpl;
//
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//
//import org.jvnet.hk2.annotations.Service;
//
//import lombok.RequiredArgsConstructor;
//import physiotherapydoctor.dto.DoctorSuggestExercise;
//import physiotherapydoctor.dto.PaymentHistory;
//import physiotherapydoctor.dto.PaymentRequest;
//import physiotherapydoctor.entity.PaymentRecord;
//import physiotherapydoctor.repository.PaymentRepository;
//
//@Service
//@RequiredArgsConstructor
public class PaymentServiceImpl {
//
//    private final PaymentRepository repository;
//
//    public PaymentRecord processPayment(PaymentRequest request) {
//
//        PaymentRecord record = repository.findByBookingId(request.getBookingId())
//                .orElse(new PaymentRecord());
//
//        // ===== BASIC INFO =====
//        record.setClinicId(request.getClinicId());
//        record.setBranchId(request.getBranchId());
//        record.setBookingId(request.getBookingId());
//        record.setPatientId(request.getPatientId());
//        record.setTherapistRecordId(request.getTherapistRecordId());
//        record.setTherapyWithSessions(request.getTherapyWithSessions());
//
//        // ===== SESSION CALCULATION (UPDATED 🔥) =====
//
//        long totalSessions = request.getTherapyWithSessions().stream()
//                .flatMap(t -> t.getTherophyData().stream())
//                .flatMap(td -> td.getExercises().stream())
//                .mapToLong(ex -> ex.getSessions().size())
//                .sum();
//
//        long completedSessions = request.getTherapyWithSessions().stream()
//                .flatMap(t -> t.getTherophyData().stream())
//                .flatMap(td -> td.getExercises().stream())
//                .flatMap(ex -> ex.getSessions().stream())
//                .filter(s -> "Completed".equalsIgnoreCase(s.getStatus()))
//                .count();
//
//        record.setTotalSessionCount((int) totalSessions);
//        record.setCompletedSessionCount(completedSessions);
//
//        // ===== INIT PAYMENT HISTORY =====
//        if (record.getPaymentHistory() == null) {
//            record.setPaymentHistory(new ArrayList<>());
//        }
//
//        // ===== DUPLICATE CHECK =====
//        boolean exists = record.getPaymentHistory().stream()
//                .anyMatch(p -> p.getTransactionId().equals(request.getTransactionId()));
//
//        if (exists) {
//            throw new RuntimeException("Duplicate payment detected");
//        }
//
//        // ===== PAYMENT CALCULATION =====
//        double previousPaid = record.getTotalPaid();
//        double newTotalPaid = previousPaid + request.getPaidAmount();
//
//        record.setTotalPaid(newTotalPaid);
//        record.setDiscountAmount(request.getDiscountAmount());
//
//        // 👉 Calculate totalAmount from exercises
//        double totalAmount = request.getTherapyWithSessions().stream()
//                .flatMap(t -> t.getTherophyData().stream())
//                .flatMap(td -> td.getExercises().stream())
//                .mapToDouble(DoctorSuggestExercise::getTotalSessionCost)
//                .sum();
//
//        record.setTotalAmount(totalAmount);
//
//        double finalAmount = totalAmount - request.getDiscountAmount();
//        record.setFinalAmount(finalAmount);
//
//        double balance = finalAmount - newTotalPaid;
//        record.setBalanceAmount(balance);
//
//        record.setPaymentStatus(balance == 0 ? "Paid" : "Partial");
//
//        // ===== PAYMENT % =====
//        double paymentPercent = (newTotalPaid * 100.0) / finalAmount;
//
//        // ===== 50% RULE =====
//        boolean isHalfCompleted = completedSessions >= (totalSessions / 2);
//
//        record.setSessionWarningFlag(isHalfCompleted && balance > 0);
//
//        // ===== ADD PAYMENT HISTORY =====
//        PaymentHistory history = new PaymentHistory();
//        history.setTransactionId(request.getTransactionId());
//        history.setAmount(request.getPaidAmount());
//        history.setDate(LocalDate.now().toString());
//        history.setPaymentMode(request.getPaymentMode());
//        history.setPaymentType(request.getPaymentType());
//        history.setDiscountIssuedBy(request.getDiscountIssuedBy());
//        history.setPaymentPercent(paymentPercent);
//
//        record.getPaymentHistory().add(history);
//
//        return repository.save(record);
//    }
}