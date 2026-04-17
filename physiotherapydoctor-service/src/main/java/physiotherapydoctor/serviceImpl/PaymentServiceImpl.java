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

    // ================= CREATE =================
    @Override
    public PaymentRecord createPayment(PaymentRequest req) {

        if (repo.findByBookingId(req.getBookingId()).isPresent()) {
            throw new RuntimeException("Already exists, use update");
        }

        PaymentRecord record = new PaymentRecord();

        // BASIC
        record.setClinicId(req.getClinicId());
        record.setBranchId(req.getBranchId());
        record.setBookingId(req.getBookingId());
        record.setPatientId(req.getPatientId());

        record.setDoctorId(req.getDoctorId());
        record.setDoctorName(req.getDoctorName());

        record.setTherapistId(req.getTherapistId());
        record.setTherapistName(req.getTherapistName());
        record.setTherapistRecordId(req.getTherapistRecordId());

        record.setServiceType(req.getServiceType());

        // SESSION
        record.setSessionStartDate(req.getSessionStartDate());
        record.setTotalSessionCount(req.getTotalSessionCount());

        // TOTAL
        double total = calculateTotal(req.getTherapyWithSessions());
        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;

        record.setTotalAmount(total);
        record.setDiscountAmount(discount);
        record.setFinalAmount(total - discount);

        // PAYMENT
        record.setTotalPaid(req.getAmount());
        record.setBalanceAmount(record.getFinalAmount() - req.getAmount());
        record.setPaymentStatus(getStatus(record));

        // CREATE SESSIONS
        boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
        record.setSessionTableCreatedStatus(created);

        record.setTherapyWithSessions(req.getTherapyWithSessions());

        // HISTORY
        record.setPaymentHistory(new ArrayList<>());
        record.getPaymentHistory().add(buildHistory(req));

        // STATUS UPDATE
        updateStatuses(record);

        return repo.save(record);
    }

    // ================= UPDATE =================
    @Override
    public PaymentRecord updatePayment(PaymentRequest req) {

        PaymentRecord record = repo.findByBookingId(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (req.getTherapyWithSessions() != null) {
            throw new RuntimeException("Do not send full data in update");
        }

        double newPaid = record.getTotalPaid() + req.getAmount();

        record.setTotalPaid(newPaid);
        record.setBalanceAmount(record.getFinalAmount() - newPaid);
        record.setPaymentStatus(getStatus(record));

        applyPaymentLevel(record, req);

        int completed = countCompleted(record);
        record.setNoOfSessionCompletedCount(completed);
        record.setNoOfSessionCompletedStatus(
                completed >= record.getTotalSessionCount()
        );

        record.getPaymentHistory().add(buildHistory(req));

        updateStatuses(record);

        return repo.save(record);
    }

    // ================= APPLY LEVEL =================
    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {

        if (req.getPaymentLevel() == null) return;

        String level = req.getPaymentLevel().toUpperCase();

        switch (level) {

            case "SESSION":
                paySessions(record, req.getPaymentTarget().getSessionIds());
                break;

            case "EXERCISE":
                payExercises(record, req.getPaymentTarget().getExerciseIds());
                break;

            case "THERAPY":
                payTherapies(record, req.getPaymentTarget().getTherapyIds());
                break;

            case "PROGRAM":
                payPrograms(record, req.getPaymentTarget().getProgramIds());
                break;

            case "PACKAGE":
                payPackages(record, req.getPaymentTarget().getPackageIds());
                break;

            default:
                throw new RuntimeException("Invalid payment level");
        }
    }

    // ================= PACKAGE =================
    private void payPackages(PaymentRecord record, List<String> ids) {

        for (var pkg : record.getTherapyWithSessions()) {
            if (ids.contains(pkg.getPackageId())) {

                pkg.setPaymentStatus("Paid");

                for (var prog : pkg.getPrograms()) {
                    prog.setPaymentStatus("Paid");

                    for (var therapy : prog.getTherapyData()) {
                        therapy.setPaymentStatus("Paid");

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus("Paid");

                            for (var s : ex.getSessions()) {
                                s.setPaymentStatus("Paid");
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= PROGRAM =================
    private void payPrograms(PaymentRecord record, List<String> ids) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {

                if (ids.contains(prog.getProgramId())) {

                    prog.setPaymentStatus("Paid");

                    for (var therapy : prog.getTherapyData()) {
                        therapy.setPaymentStatus("Paid");

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus("Paid");

                            for (var s : ex.getSessions()) {
                                s.setPaymentStatus("Paid");
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= THERAPY =================
    private void payTherapies(PaymentRecord record, List<String> ids) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {

                    if (ids.contains(therapy.getTherapyId())) {

                        therapy.setPaymentStatus("Paid");

                        for (var ex : therapy.getExercises()) {
                            ex.setPaymentStatus("Paid");

                            for (var s : ex.getSessions()) {
                                s.setPaymentStatus("Paid");
                            }
                        }
                    }
                }
            }
        }
    }

    // ================= EXERCISE =================
    private void payExercises(PaymentRecord record, List<String> ids) {

        for (var pkg : record.getTherapyWithSessions()) {
            for (var prog : pkg.getPrograms()) {
                for (var therapy : prog.getTherapyData()) {
                    for (var ex : therapy.getExercises()) {

                        if (ids.contains(ex.getExerciseId())) {

                            ex.setPaymentStatus("Paid");

                            for (var s : ex.getSessions()) {
                                s.setPaymentStatus("Paid");
                            }
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

        for (var pkg : record.getTherapyWithSessions()) {

            boolean allPkgPaid = true;

            for (var prog : pkg.getPrograms()) {

                boolean allProgPaid = true;

                for (var therapy : prog.getTherapyData()) {

                    boolean allTherapyPaid = true;

                    for (var ex : therapy.getExercises()) {

                        boolean allExPaid = ex.getSessions().stream()
                                .allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));

                        ex.setPaymentStatus(allExPaid ? "Paid" : "Partial");

                        if (!allExPaid) allTherapyPaid = false;
                    }

                    therapy.setPaymentStatus(allTherapyPaid ? "Paid" : "Partial");

                    if (!allTherapyPaid) allProgPaid = false;
                }

                prog.setPaymentStatus(allProgPaid ? "Paid" : "Partial");

                if (!allProgPaid) allPkgPaid = false;
            }

            pkg.setPaymentStatus(allPkgPaid ? "Paid" : "Partial");
        }
    }

    // ================= UTIL =================
    private String getStatus(PaymentRecord r) {
        if (r.getTotalPaid() == 0) return "Unpaid";
        if (r.getTotalPaid() < r.getFinalAmount()) return "Partial";
        return "Paid";
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
}