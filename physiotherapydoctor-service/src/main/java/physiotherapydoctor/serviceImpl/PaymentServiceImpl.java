package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.PaymentHistory;
import physiotherapydoctor.dto.PaymentRequest;
import physiotherapydoctor.dto.Program;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.dto.TherapistRecordDTO;
import physiotherapydoctor.dto.TherapyData;
import physiotherapydoctor.dto.TherapyExercise;
import physiotherapydoctor.dto.TherapyWithSessions;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.feign.BookingFeign;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.service.PaymentService;
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository repo;
    
    @Autowired
	private BookingFeign bookingFeign;
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;
    // ================= CREATE =================
    @Override
    public PaymentRecord createPayment(PaymentRequest req) {

        if (repo.findByBookingId(req.getBookingId()).isPresent()) {
            throw new RuntimeException("Already exists, use update");
        }

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        if (req.getTherapyWithSessions() == null || req.getTherapyWithSessions().isEmpty()) {
            throw new RuntimeException("therapyWithSessions is required");
        }

        PaymentRecord record = new PaymentRecord();

        // ================= BASIC =================
        record.setClinicId(req.getClinicId());
        record.setBranchId(req.getBranchId());
        record.setBookingId(req.getBookingId());
        record.setPatientId(req.getPatientId());
        record.setOverallStatus("Pending");

        record.setDoctorId(req.getDoctorId());
        record.setDoctorName(req.getDoctorName());

        record.setTherapistId(req.getTherapistId());
        record.setTherapistName(req.getTherapistName());
        record.setTherapistRecordId(req.getTherapistRecordId());

        record.setServiceType(req.getServiceType());

        // ================= SESSION =================
        record.setSessionStartDate(req.getSessionStartDate());
        record.setTotalSessionCount(req.getTotalSessionCount());

        // ================= TOTAL =================
        double total = calculateTotal(req.getTherapyWithSessions());
        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;

        double finalAmount = total - discount;

        record.setTotalAmount(total);
        record.setDiscountAmount(discount);
        record.setFinalAmount(finalAmount);

        double amount = req.getAmount();

        // ================= VALIDATIONS =================
        if (amount > finalAmount) {
            throw new RuntimeException("Amount exceeds final payable amount: " + finalAmount);
        }

        if ("FULL".equalsIgnoreCase(req.getPaymentType()) && amount != finalAmount) {
            throw new RuntimeException("Full payment must be exactly: " + finalAmount);
        }

        // ================= PAYMENT =================
        record.setTotalPaid(amount);
        record.setBalanceAmount(finalAmount - amount);
        record.setPaymentStatus(getStatus(record));

        // ================= CREATE SESSIONS =================
        boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
        record.setSessionTableCreatedStatus(created);

        // 🔥 FIX 1: SET DATA FIRST
        record.setTherapyWithSessions(req.getTherapyWithSessions());

        // 🔥 FIX 2: THEN DISTRIBUTE
        distributePaymentToSessions(record);

        // ================= HISTORY =================
        record.setPaymentHistory(new ArrayList<>());
        record.getPaymentHistory().add(buildHistory(req));

        // ================= APPLY LEVEL =================
        if (req.getPaymentTarget() != null) {
            applyPaymentLevel(record, req);
        }

        // ================= STATUS PROPAGATION =================
        updateStatuses(record);

        return repo.save(record);
    }    //    @Override
//    public PaymentRecord createPayment(PaymentRequest req) {
//
//        if (repo.findByBookingId(req.getBookingId()).isPresent()) {
//            throw new RuntimeException("Already exists, use update");
//        }
//
//        PaymentRecord record = new PaymentRecord();
//
//        // BASIC
//        record.setClinicId(req.getClinicId());
//        record.setBranchId(req.getBranchId());
//        record.setBookingId(req.getBookingId());
//        record.setPatientId(req.getPatientId());
//
//        record.setDoctorId(req.getDoctorId());
//        record.setDoctorName(req.getDoctorName());
//
//        record.setTherapistId(req.getTherapistId());
//        record.setTherapistName(req.getTherapistName());
//        record.setTherapistRecordId(req.getTherapistRecordId());
//
//        record.setServiceType(req.getServiceType());
//
//        // SESSION
//        record.setSessionStartDate(req.getSessionStartDate());
//        record.setTotalSessionCount(req.getTotalSessionCount());
//
//        // TOTAL
//        double total = calculateTotal(req.getTherapyWithSessions());
//        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
//
//        record.setTotalAmount(total);
//        record.setDiscountAmount(discount);
//        record.setFinalAmount(total - discount);
//
//        // PAYMENT
//        record.setTotalPaid(req.getAmount());
//        record.setBalanceAmount(record.getFinalAmount() - req.getAmount());
//        record.setPaymentStatus(getStatus(record));
//
//        // CREATE SESSIONS
//        boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
//        record.setSessionTableCreatedStatus(created);
//
//        record.setTherapyWithSessions(req.getTherapyWithSessions());
//
//        // HISTORY
//        record.setPaymentHistory(new ArrayList<>());
//        record.getPaymentHistory().add(buildHistory(req));
//
//        // STATUS UPDATE
//        updateStatuses(record);
//
//        return repo.save(record);
//    }

    // ================= UPDATE =================
    @Override
    public PaymentRecord updatePayment(PaymentRequest req) {

        PaymentRecord record = repo.findByBookingId(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // ❌ Prevent sending full structure in update
        if (req.getTherapyWithSessions() != null) {
            throw new RuntimeException("Do not send full data in update");
        }

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }

        if (req.getPaymentTarget() == null) {
            throw new RuntimeException("paymentTarget is required");
        }

        double currentPaid = record.getTotalPaid();
        double finalAmount = record.getFinalAmount();
        double remaining = finalAmount - currentPaid;

        // ================= 🔥 FULL PAYMENT VALIDATION =================
        if ("FULL".equalsIgnoreCase(req.getPaymentType())) {

            if (req.getAmount() != remaining) {
                throw new RuntimeException(
                        "Full payment must be exactly remaining amount: " + remaining
                );
            }
        }

        // ================= 🔥 OVERPAYMENT PREVENTION =================
        double newPaid = currentPaid + req.getAmount();

        if (newPaid > finalAmount) {
            throw new RuntimeException(
                    "Payment exceeds final amount. Remaining payable: " + remaining
            );
        }

        // ================= UPDATE AMOUNT =================
        record.setTotalPaid(newPaid);
        record.setBalanceAmount(finalAmount - newPaid);
        record.setPaymentStatus(getStatus(record));
     // 🔥 NEW LINE
        distributePaymentToSessions(record);
        // ================= APPLY PAYMENT LEVEL =================
        applyPaymentLevel(record, req);

        // ================= SESSION COMPLETION =================
        int completed = countCompleted(record);

        record.setNoOfSessionCompletedCount(completed);
        record.setNoOfSessionCompletedStatus(
                completed >= record.getTotalSessionCount()
        );

        // ================= HISTORY =================
        record.getPaymentHistory().add(buildHistory(req));

        // ================= STATUS PROPAGATION =================
        updateStatuses(record);

        return repo.save(record);
    }
    
//    @Override
//    public PaymentRecord updatePayment(PaymentRequest req) {
//
//        PaymentRecord record = repo.findByBookingId(req.getBookingId())
//                .orElseThrow(() -> new RuntimeException("Not found"));
//
//        if (req.getTherapyWithSessions() != null) {
//            throw new RuntimeException("Do not send full data in update");
//        }
//
//        double newPaid = record.getTotalPaid() + req.getAmount();
//
//        record.setTotalPaid(newPaid);
//        record.setBalanceAmount(record.getFinalAmount() - newPaid);
//        record.setPaymentStatus(getStatus(record));
//
//        applyPaymentLevel(record, req);
//
//        int completed = countCompleted(record);
//        record.setNoOfSessionCompletedCount(completed);
//        record.setNoOfSessionCompletedStatus(
//                completed >= record.getTotalSessionCount()
//        );
//
//        record.getPaymentHistory().add(buildHistory(req));
//
//        updateStatuses(record);
//
//        return repo.save(record);
//    }

    // ================= APPLY LEVEL =================
    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {

        if (req.getPaymentLevel() == null || req.getPaymentTarget() == null) return;

        String level = req.getPaymentLevel().toUpperCase();
        String status = getStatus(record); // Unpaid / Partial / Paid

        switch (level) {

            case "PACKAGE":
                updatePackageStatus(record, req.getPaymentTarget().getPackageIds(), status);
                break;

            case "PROGRAM":
                updateProgramStatus(record, req.getPaymentTarget().getProgramIds(), status);
                break;

            case "THERAPY":
                updateTherapyStatus(record, req.getPaymentTarget().getTherapyIds(), status);
                break;

            case "EXERCISE":
                updateExerciseStatus(record, req.getPaymentTarget().getExerciseIds(), status);
                break;

            case "SESSION":
                paySessions(record, req.getPaymentTarget().getSessionIds());
                break;
        }
    }
    private void distributePaymentToSessions(PaymentRecord record) {

        if (record.getTherapyWithSessions() == null) return; // 🔥 FIX

        double remaining = record.getTotalPaid();

        for (var pkg : record.getTherapyWithSessions()) {
            if (pkg.getPrograms() == null) continue;

            for (var prog : pkg.getPrograms()) {
                if (prog.getTherapyData() == null) continue;

                for (var therapy : prog.getTherapyData()) {
                    if (therapy.getExercises() == null) continue;

                    for (var ex : therapy.getExercises()) {

                        if (ex.getSessions() == null) continue;

                        double price = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;

                        for (var s : ex.getSessions()) {

                            if (remaining <= 0) {
                                s.setPaymentStatus("Unpaid");
                            } else if (remaining >= price) {
                                s.setPaymentStatus("Paid");
                                remaining -= price;
                            } else {
                                s.setPaymentStatus("Partial");
                                remaining = 0;
                            }
                        }
                    }
                }
            }
        }
    }    private static class SessionWrapper {
        Session session;
        double price;

        SessionWrapper(Session s, double p) {
            this.session = s;
            this.price = p;
        }
    }
    //    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
    

//
//        if (req.getPaymentLevel() == null || req.getPaymentTarget() == null) return;
//
//        // 🔥 Only allow marking Paid when FULL payment is done
//        boolean isFullPaid = Double.compare(
//                record.getTotalPaid(),
//                record.getFinalAmount()
//        ) == 0;
//
//        if (!isFullPaid) {
//            return; // ❌ DO NOTHING for partial payment
//        }
//
//        String level = req.getPaymentLevel().toUpperCase();
//
//        switch (level) {
//
//            case "SESSION":
//                paySessions(record, req.getPaymentTarget().getSessionIds());
//                break;
//
//            case "EXERCISE":
//                payExercises(record, req.getPaymentTarget().getExerciseIds());
//                break;
//
//            case "THERAPY":
//                payTherapies(record, req.getPaymentTarget().getTherapyIds());
//                break;
//
//            case "PROGRAM":
//                payPrograms(record, req.getPaymentTarget().getProgramIds());
//                break;
//
//            case "PACKAGE":
//                payPackages(record, req.getPaymentTarget().getPackageIds());
//                break;
//        }
//    }
//    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
//
//        if (req.getPaymentLevel() == null) return;
//
//        String level = req.getPaymentLevel().toUpperCase();
//
//        switch (level) {
//
//            case "SESSION":
//                paySessions(record, req.getPaymentTarget().getSessionIds());
//                break;
//
//            case "EXERCISE":
//                payExercises(record, req.getPaymentTarget().getExerciseIds());
//                break;
//
//            case "THERAPY":
//                payTherapies(record, req.getPaymentTarget().getTherapyIds());
//                break;
//
//            case "PROGRAM":
//                payPrograms(record, req.getPaymentTarget().getProgramIds());
//                break;
//
//            case "PACKAGE":
//                payPackages(record, req.getPaymentTarget().getPackageIds());
//                break;
//
//            default:
//                throw new RuntimeException("Invalid payment level");
//        }
//    }

    // ================= PACKAGE =================
    private void updatePackageStatus(PaymentRecord record, List<String> ids, String status) {

        if (ids == null) return;

        for (var pkg : record.getTherapyWithSessions()) {
            if (ids.contains(pkg.getPackageId())) {

                pkg.setPaymentStatus(status);

                for (var prog : pkg.getPrograms()) {
                    prog.setPaymentStatus(status);

                    for (var therapy : prog.getTherapyData()) {
                        therapy.setPaymentStatus(status);

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus(status);
                        }
                    }
                }
            }
        }
    }
    //    private void payPackages(PaymentRecord record, List<String> ids) {
//
//        for (var pkg : record.getTherapyWithSessions()) {
//            if (ids.contains(pkg.getPackageId())) {
//
//                pkg.setPaymentStatus("Paid");
//
//                for (var prog : pkg.getPrograms()) {
//                    prog.setPaymentStatus("Paid");
//
//                    for (var therapy : prog.getTherapyData()) {
//                        therapy.setPaymentStatus("Paid");
//
//                        for (var ex : therapy.getExercises()) {
//                            ex.setPaymentStatus("Paid");
//
//                            for (var s : ex.getSessions()) {
//                                s.setPaymentStatus("Paid");
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    // ================= PROGRAM =================
    private void updateProgramStatus(PaymentRecord record, List<String> ids, String status) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {

                if (ids.contains(prog.getProgramId())) {

                    prog.setPaymentStatus(status);

                    for (var therapy : prog.getTherapyData()) {
                        therapy.setPaymentStatus(status);

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus(status);
                        }
                    }
                }
            }
        }
    }
    // ================= THERAPY =================
    private void updateTherapyStatus(PaymentRecord record, List<String> ids, String status) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {

                    if (ids.contains(therapy.getTherapyId())) {

                        therapy.setPaymentStatus(status);

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus(status);
                        }
                    }
                }
            }
        }
    }
    // ================= EXERCISE =================
    private void updateExerciseStatus(PaymentRecord record, List<String> ids, String status) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {

                        if (ids.contains(ex.getExerciseId())) {
                            ex.setPaymentStatus(status);
                        }
                    }
                }
            }
        }
    }

    // ================= SESSION =================
    private void paySessions(PaymentRecord record, List<String> ids) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {

                        for (var s : ex.getSessions()) {
                            if (ids.contains(s.getSessionId())) {
                                s.setPaymentStatus("Paid");
                            }
                        }

                        boolean allPaid = ex.getSessions().stream()
                                .allMatch(x -> "Paid".equalsIgnoreCase(x.getPaymentStatus()));

                        ex.setPaymentStatus(allPaid ? "Paid" : "Partial");
                    }
                }
            }
        }
    }

    // ================= STATUS PROPAGATION =================
    private void updateStatuses(PaymentRecord record) {

        if (record.getTherapyWithSessions() == null) return; // 🔥 FIX

        for (var pkg : record.getTherapyWithSessions()) {

            if (pkg.getPrograms() == null) continue;

            for (var prog : pkg.getPrograms()) {

                if (prog.getTherapyData() == null) continue;

                for (var therapy : prog.getTherapyData()) {

                    if (therapy.getExercises() == null) continue;

                    for (var ex : therapy.getExercises()) {

                        if (ex.getSessions() == null || ex.getSessions().isEmpty()) {
                            ex.setPaymentStatus("Unpaid");
                            continue;
                        }

                        boolean allPaid = ex.getSessions().stream()
                                .allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));

                        boolean anyPaid = ex.getSessions().stream()
                                .anyMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));

                        if (allPaid) ex.setPaymentStatus("Paid");
                        else if (anyPaid) ex.setPaymentStatus("Partial");
                        else ex.setPaymentStatus("Unpaid");
                    }
                }
            }
        }
    }
    // ================= UTIL =================
    
    private String getStatus(PaymentRecord r) {

        if (r.getTotalPaid() <= 0) return "Unpaid";

        if (r.getTotalPaid() < r.getFinalAmount()) return "Partial";

        if (Double.compare(r.getTotalPaid(), r.getFinalAmount()) == 0) return "Paid";

        return "Overpaid"; // safety fallback
    }

    private PaymentHistory buildHistory(PaymentRequest req) {
        return new PaymentHistory(
                req.getAmount(),
                req.getPaymentMode(),
                req.getPaymentType(),
                req.getPaymentDate(),
                req.getPaymentLevel(),
                req.getDiscountAmount(),
                req.getDiscountIssuedBy()
        );
    }

    private double calculateTotal(List<TherapyWithSessions> data) {

        double total = 0;

        for (var pkg : data) {

            double pkgTotal = 0;

            for (var prog : pkg.getPrograms()) {

                double progTotal = 0;

                for (var therapy : prog.getTherapyData()) {

                    double therapyTotal = 0;

                    for (var ex : therapy.getExercises()) {

                        double exTotal = ex.getPricePerSession() * ex.getNoOfSessions();

                        ex.setTotalExercisePrice(exTotal);
                        therapyTotal += exTotal;
                    }

                    // ✅ FIX
                    therapy.setTotalTherapyPrice(therapyTotal);

                    progTotal += therapyTotal;
                }

                // ✅ FIX
                prog.setTotalProgramPrice(progTotal);

                pkgTotal += progTotal;
            }

            // ✅ FIX
            pkg.setTotalPackagePrice(pkgTotal);

            total += pkgTotal;
        }

        return total;
    }

    private boolean createSessions(List<TherapyWithSessions> data, String startDate) {

        boolean created = false;

        for (var pkg : data) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {

                        List<Session> sessions = new ArrayList<>();

                        for (int i = 1; i <= ex.getNoOfSessions(); i++) {
                            sessions.add(new Session(
                                    ex.getExerciseId() + "_" + i,
                                    i,
                                    startDate,
                                    "Pending",
                                    "Unpaid"
                            ));
                        }

                        if (!sessions.isEmpty()) created = true;

                        ex.setSessions(sessions);
                    }
                }
            }
        }

        return created;
    }

    private int countCompleted(PaymentRecord record) {

        int count = 0;

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {
                        for (var s : ex.getSessions()) {
                            if ("Completed".equalsIgnoreCase(s.getStatus())) {
                                count++;
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

    @Override
    public PaymentRecord getByBookingId(String bookingId) {

        PaymentRecord record = repo.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));

        // ✅ Recalculate completed sessions
        int completed = countCompleted(record);

        record.setNoOfSessionCompletedCount(completed);
        record.setNoOfSessionCompletedStatus(
                completed >= record.getTotalSessionCount()
        );

        return record;
    }
    @Override
    public void deleteByBookingId(String bookingId) {

        PaymentRecord record = repo.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));

        repo.delete(record);
    }
    @Override
    public void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId) {

        PaymentRecord record = repo.findByTherapistRecordId(therapistRecordId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        List<TherapyWithSessions> packageList = record.getTherapyWithSessions();

        if (packageList == null || packageList.isEmpty()) {
            throw new RuntimeException("No sessions found");
        }

        boolean sessionFound = false;

        for (TherapyWithSessions packageData : packageList) {

            List<Program> programList = packageData.getPrograms();

            if (programList == null || programList.isEmpty()) {
                continue;
            }

            for (Program program : programList) {

                List<TherapyData> therapyList = program.getTherapyData();

                if (therapyList == null || therapyList.isEmpty()) {
                    continue;
                }

                for (TherapyData therapy : therapyList) {

                    List<TherapyExercise> exerciseList = therapy.getExercises();

                    if (exerciseList == null || exerciseList.isEmpty()) {
                        continue;
                    }

                    for (TherapyExercise exercise : exerciseList) {

                        List<Session> sessionList = exercise.getSessions();

                        if (sessionList == null || sessionList.isEmpty()) {
                            continue;
                        }

                        for (Session session : sessionList) {

                            if (sessionId.equals(session.getSessionId())) {
                                session.setStatus("Completed");
                                sessionFound = true;
                                break;
                            }
                        }

                        if (sessionFound) {
                            break;
                        }
                    }

                    if (sessionFound) {
                        break;
                    }
                }

                if (sessionFound) {
                    break;
                }
            }

            if (sessionFound) {
                break;
            }
        }

        if (!sessionFound) {
            throw new RuntimeException("Session not found with ID: " + sessionId);
        }

        record.setOverallStatus(calculateOverallStatus(record));

        repo.save(record);

        updateBookingStatus(record);
    }

    private String calculateOverallStatus(PaymentRecord record) {

        List<TherapyWithSessions> packageList = record.getTherapyWithSessions();

        if (packageList == null || packageList.isEmpty()) {
            return "Pending";
        }

        boolean allCompleted = true;
        boolean anyCompleted = false;

        for (TherapyWithSessions packageData : packageList) {

            List<Program> programList = packageData.getPrograms();

            if (programList == null || programList.isEmpty()) {
                continue;
            }

            for (Program program : programList) {

                List<TherapyData> therapyList = program.getTherapyData();

                if (therapyList == null || therapyList.isEmpty()) {
                    continue;
                }

                for (TherapyData therapy : therapyList) {

                    List<TherapyExercise> exerciseList = therapy.getExercises();

                    if (exerciseList == null || exerciseList.isEmpty()) {
                        continue;
                    }

                    for (TherapyExercise exercise : exerciseList) {

                        List<Session> sessionList = exercise.getSessions();

                        if (sessionList == null || sessionList.isEmpty()) {
                            continue;
                        }

                        for (Session session : sessionList) {

                            if ("Completed".equalsIgnoreCase(session.getStatus())) {
                                anyCompleted = true;
                            } else {
                                allCompleted = false;
                            }
                        }
                    }
                }
            }
        }

        if (allCompleted) {
            return "Completed";
        }

        if (anyCompleted) {
            return "Active";
        }

        return "Pending";
    }

    private void updateBookingStatus(PaymentRecord record) {

        if (record.getBookingId() == null || record.getBookingId().trim().isEmpty()) {
            return;
        }

        try {

            BookingResponse request = new BookingResponse();
            request.setBookingId(record.getBookingId().trim());

            // ✅ Map payment overall status to booking status
            if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {
                request.setStatus("completed");
            } else if ("Active".equalsIgnoreCase(record.getOverallStatus())) {
                request.setStatus("in-progress");
            } else {
                request.setStatus("pending");
            }

            clinicAdminFeign.updateAppointment(request);

            System.out.println("Booking status updated successfully => "
                    + request.getBookingId() + " | " + request.getStatus());

        } catch (Exception e) {
            System.out.println("Booking status update failed");
            e.printStackTrace();
        }
    }
    @Override
    public Response getExerciseSessionsWithRecords(
            String clinicId,
            String branchId,
            String bookingId,
            String patientId,
            String therapistRecordId,
            String exerciseId) {

        Response response = new Response();

        try {

            PaymentRecord record = paymentRepository
                    .findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistRecordId(
                            clinicId, branchId, bookingId, patientId, therapistRecordId)
                    .orElseThrow(() -> new RuntimeException("Payment record not found"));

            for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {
                if (pkg.getPrograms() == null) continue;

                for (Program program : pkg.getPrograms()) {
                    if (program.getTherapyData() == null) continue;

                    for (TherapyData therapy : program.getTherapyData()) {
                        if (therapy.getExercises() == null) continue;

                        for (TherapyExercise exercise : therapy.getExercises()) {

                            if (!exerciseId.equals(exercise.getExerciseId())) {
                                continue;
                            }

                            List<Object> sessionList = new ArrayList<>();

                            for (Session session : exercise.getSessions()) {

                                Map<String, Object> map = new LinkedHashMap<>();

                                // ✅ First session details
                                map.put("sessionId", session.getSessionId());
                                map.put("sessionNo", session.getSessionNo());
                                map.put("date", session.getDate());
                                map.put("paymentStatus", session.getPaymentStatus());

                                try {

                                    ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
                                            clinicAdminFeign.getRecordBySession(
                                                    clinicId,
                                                    branchId,
                                                    bookingId,
                                                    patientId,
                                                    session.getSessionId()
                                            );

                                    if (tr != null
                                            && tr.getBody() != null
                                            && tr.getBody().getData() != null) {

                                        map.put("status", "Completed");

                                        // ✅ Later therapist record
                                        map.put("therapistRecord",
                                                tr.getBody().getData());

                                    } else {
                                        map.put("status", session.getStatus());
                                        map.put("therapistRecord", null);
                                    }

                                } catch (Exception e) {
                                    map.put("status", session.getStatus());
                                    map.put("therapistRecord", null);
                                }

                                sessionList.add(map);
                            }

                            Map<String, Object> finalData = new LinkedHashMap<>();
                            finalData.put("exerciseId", exercise.getExerciseId());
                            finalData.put("exerciseName", exercise.getExerciseName());
                            finalData.put("sessions", sessionList);

                            response.setSuccess(true);
                            response.setStatus(200);
                            response.setMessage("Sessions fetched successfully");
                            response.setData(finalData);
                            return response;
                        }
                    }
                }
            }

            response.setSuccess(false);
            response.setStatus(404);
            response.setMessage("Exercise not found");

        } catch (Exception e) {
            response.setSuccess(false);
            response.setStatus(500);
            response.setMessage(e.getMessage());
        }

        return response;
    }
}