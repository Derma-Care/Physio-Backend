package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import physiotherapydoctor.dto.response.ExerciseResponse;
import physiotherapydoctor.dto.response.PackageResponse;
import physiotherapydoctor.dto.response.PaymentRecordResponse;
import physiotherapydoctor.dto.response.ProgramResponse;
import physiotherapydoctor.dto.response.TherapyResponse;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.service.PaymentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository repo;

	@Autowired
	private BookingFeignClient bookingFeign;

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;

	// =====================================================
	// ✅ WHATSAPP SERVICE INJECTED
	// =====================================================
	@Autowired
	private PaymentWhatsAppService paymentWhatsAppService;

	// ========================================================
	// CREATE
	// ========================================================
	@Override
	public PaymentRecordResponse createPayment(PaymentRequest req) {

		if (repo.findByBookingId(req.getBookingId()).isPresent()) {
			throw new RuntimeException("Already exists, use update");
		}

		if (req.isPayAfterService()) {
			if (req.getAmount() == null || req.getAmount() <= 0) {
				throw new RuntimeException("Amount must be greater than 0");
			}
		}

		if (req.getTherapyWithSessions() == null || req.getTherapyWithSessions().isEmpty()) {
			throw new RuntimeException("therapyWithSessions is required");
		}

		// ✅ STEP 1: Normalize payload to full Package structure
		List<TherapyWithSessions> normalized = normalizePayload(req);
		req.setTherapyWithSessions(normalized);

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
		record.setTreatmentName(req.getTreatmentName());
		record.setServiceType(req.getServiceType());

		// ================= SESSION =================
		record.setSessionStartDate(req.getSessionStartDate());

		// ================= TOTAL =================
		double total = calculateTotal(req.getTherapyWithSessions());

		double exerciseDiscount = calculateTotalDiscount(req.getTherapyWithSessions());
		record.setOverallReceiptNumber(generateOverallReceiptNumber());
		// Create payment history first
		record.setPaymentHistory(new ArrayList<>());
		record.getPaymentHistory().add(buildHistory(req, generateReceiptNumber()));

		// Total discount from payment history
		double paymentHistoryDiscount = calculatePaymentHistoryDiscount(record.getPaymentHistory());

		double finalAmount = total - paymentHistoryDiscount;

		record.setTotalAmount(total);
		record.setDiscountAmount(paymentHistoryDiscount);
		record.setFinalAmount(finalAmount);
		double amount = req.isPayAfterService() ? req.getAmount() : 0;

		record.setPayAfterService(req.isPayAfterService());

		// ================= PAYMENT =================
		record.setTotalPaid(amount);
		record.setBalanceAmount(finalAmount - amount);
		record.setPaymentStatus(getStatus(record));

		// ================= CREATE SESSIONS =================
		boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
		record.setSessionTableCreatedStatus(created);
		// ✅ Set session end date
		record.setSessionEndDate(getLastSessionDate(req.getTherapyWithSessions()));

		// ✅ STEP 2: Set normalized data on record BEFORE distribute/status calls
		record.setTherapyWithSessions(req.getTherapyWithSessions());

		// ✅ AUTO-COUNT total sessions
		record.setTotalSessionCount(countTotalSessions(record));

		// ✅ STEP 3: Distribute payment across sessions
		distributePaymentToSessions(record);

		// ================= HISTORY =================
		record.setPaymentHistory(new ArrayList<>());
		record.getPaymentHistory().add(buildHistory(req, generateReceiptNumber()));

		// ================= APPLY LEVEL =================
		if (req.getPaymentTarget() != null) {
			applyPaymentLevel(record, req);
		}

		// ================= STATUS PROPAGATION =================
		updateStatuses(record);

		// =====================================================
		// ✅ STEP 4 — SAVE RECORD
		// =====================================================
		PaymentRecord savedRecord = repo.save(record);

		// =====================================================
		// ✅ STEP 5 — SEND WHATSAPP (fire-and-forget)
		// fetches booking data internally via Feign
		// WhatsApp failure must NEVER fail payment creation
		// =====================================================
		try {
			paymentWhatsAppService.sendPaymentConfirmation(savedRecord);
			log.info("WhatsApp triggered for bookingId={}", savedRecord.getBookingId());
		} catch (Exception e) {
			log.warn("WhatsApp notification failed for bookingId={} : {}", savedRecord.getBookingId(), e.getMessage());
		}

		return mapToResponse(savedRecord);
	}

	// ========================================================
	// UPDATE
	// ========================================================
	@Override
	public PaymentRecordResponse updatePayment(PaymentRequest req) {

		PaymentRecord record = repo.findByBookingId(req.getBookingId())
				.orElseThrow(() -> new RuntimeException("Payment not found"));
		
		record.setTreatmentName(req.getTreatmentName());

		if (req.getTherapyWithSessions() != null) {
			throw new RuntimeException("Do not send therapyWithSessions in update");
		}

		record.setPayAfterService(req.isPayAfterService());

		if (req.isPayAfterService()) {
			if (req.getAmount() == null || req.getAmount() <= 0) {
				throw new RuntimeException("Amount must be greater than 0");
			}
		}

		double amount = req.isPayAfterService() ? req.getAmount() : 0;

		// ================= APPLY ADDITIONAL DISCOUNT =================
		double additionalDiscount = (req.getDiscountAmount() != null) ? req.getDiscountAmount() : 0;
		if (additionalDiscount > 0) {
			double newFinalAmount = record.getFinalAmount() - additionalDiscount;
			record.setDiscountAmount(record.getDiscountAmount() + additionalDiscount);
			record.setFinalAmount(newFinalAmount);
		}

		double currentPaid = record.getTotalPaid();
		double finalAmount = record.getFinalAmount();
		double remaining = finalAmount - currentPaid;

		// ================= OVERPAYMENT PREVENTION =================
		double newPaid = currentPaid + amount;

		if (newPaid > finalAmount) {
			throw new RuntimeException("Payment exceeds final amount. Remaining payable: " + remaining);
		}

		// ================= UPDATE AMOUNT =================
		record.setTotalPaid(newPaid);
		record.setBalanceAmount(finalAmount - newPaid);
		record.setPaymentStatus(getStatus(record));

		// ================= DISTRIBUTE =================
		distributePaymentToSessions(record);

		// ================= APPLY PAYMENT LEVEL =================
		applyPaymentLevel(record, req);

		// ================= SESSION COMPLETION =================
		int completed = countCompleted(record);
		record.setNoOfSessionCompletedCount(completed);
		record.setNoOfSessionCompletedStatus(completed >= record.getTotalSessionCount());

		// ================= HISTORY =================
		record.getPaymentHistory().add(buildHistory(req, generateReceiptNumber()));

		// ================= STATUS PROPAGATION =================
		updateStatuses(record);

		PaymentRecord savedRecord = repo.save(record);

		// =====================================================
		// ✅ SEND WHATSAPP ON UPDATE TOO (balance due changed)
		// =====================================================
		try {
			paymentWhatsAppService.sendPaymentConfirmation(savedRecord);
			log.info("WhatsApp update triggered for bookingId={}", savedRecord.getBookingId());
		} catch (Exception e) {
			log.warn("WhatsApp update notification failed for bookingId={} : {}", savedRecord.getBookingId(),
					e.getMessage());
		}

		return mapToResponse(savedRecord);
	}

	private double calculatePaymentHistoryDiscount(List<PaymentHistory> paymentHistory) {

		if (paymentHistory == null || paymentHistory.isEmpty()) {
			return 0;
		}

		return paymentHistory.stream().mapToDouble(ph -> ph.getDiscountAmount() != null ? ph.getDiscountAmount() : 0)
				.sum();
	}

	private double calculateTotalDiscount(List<TherapyWithSessions> data) {

		double totalDiscount = 0;

		for (var pkg : data) {
			for (var prog : pkg.getPrograms()) {
				for (var therapy : prog.getTherapyData()) {
					for (var ex : therapy.getExercises()) {

						if (ex.getDiscountAmount() != 0) {
							totalDiscount += ex.getDiscountAmount();
						} else if (ex.getDiscountPercentage() != 0 && ex.getDiscountPercentage() > 0) {
							double exPrice = ex.getPricePerSession() * ex.getNoOfSessions();
							totalDiscount += (exPrice * ex.getDiscountPercentage() / 100);
						}
					}
				}
			}
		}

		return totalDiscount;
	}

	// ========================================================
	// GET BY BOOKING ID
	// ========================================================
	@Override
	public PaymentRecordResponse getByBookingId(String bookingId) {

		PaymentRecord record = repo.findByBookingId(bookingId)
				.orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));

		int completed = countCompleted(record);
		record.setNoOfSessionCompletedCount(completed);
		record.setNoOfSessionCompletedStatus(completed >= record.getTotalSessionCount());

		return mapToResponse(record);
	}

	// ========================================================
	// DELETE BY BOOKING ID
	// ========================================================
	@Override
	public void deleteByBookingId(String bookingId) {

		PaymentRecord record = repo.findByBookingId(bookingId)
				.orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));

		repo.delete(record);
	}

	// ========================================================
	// UPDATE SESSION STATUS FROM THERAPIST
	// ========================================================
	@Override
	public void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId) {

		List<PaymentRecord> records = repo.findByTherapistRecordId(therapistRecordId);

		if (records == null || records.isEmpty()) {
			throw new RuntimeException("No payment records found for therapistRecordId: " + therapistRecordId);
		}

		PaymentRecord targetRecord = null;
		boolean sessionFound = false;

		outer: for (PaymentRecord record : records) {

			List<TherapyWithSessions> packageList = record.getTherapyWithSessions();
			if (packageList == null || packageList.isEmpty())
				continue;

			for (TherapyWithSessions pkg : packageList) {
				if (pkg.getPrograms() == null)
					continue;

				for (Program program : pkg.getPrograms()) {
					if (program.getTherapyData() == null)
						continue;

					for (TherapyData therapy : program.getTherapyData()) {
						if (therapy.getExercises() == null)
							continue;

						for (TherapyExercise exercise : therapy.getExercises()) {
							if (exercise.getSessions() == null)
								continue;

							for (Session session : exercise.getSessions()) {
								if (sessionId.equals(session.getSessionId())) {
									session.setStatus("Completed");
									targetRecord = record;
									sessionFound = true;
									break outer;
								}
							}
						}
					}
				}
			}
		}

		if (!sessionFound || targetRecord == null) {
			throw new RuntimeException("Session not found with ID: " + sessionId);
		}

		targetRecord.setOverallStatus(calculateOverallStatus(targetRecord));
		repo.save(targetRecord);
		updateBookingStatus(targetRecord);
	}

	// ========================================================
	// NORMALIZE PAYLOAD
	// ========================================================
	private List<TherapyWithSessions> normalizePayload(PaymentRequest req) {

		String serviceType = req.getServiceType() != null ? req.getServiceType().toLowerCase() : "package";

		List<TherapyWithSessions> incoming = req.getTherapyWithSessions();

		if (incoming == null || incoming.isEmpty()) {
			throw new RuntimeException("therapyWithSessions is required");
		}

		switch (serviceType) {

		case "package":
			return incoming;

		case "program": {
			TherapyWithSessions dummyPackage = new TherapyWithSessions();
			dummyPackage.setPackageId("PKG_AUTO");
			dummyPackage.setPackageName("Auto Package");

			List<Program> programs = new ArrayList<>();
			for (TherapyWithSessions item : incoming) {
				if (item.getProgramId() == null)
					continue;
				Program prog = new Program();
				prog.setProgramId(item.getProgramId());
				prog.setProgramName(item.getProgramName());
				prog.setTherapyData(item.getTherapyData());
				programs.add(prog);
			}

			if (programs.isEmpty()) {
				throw new RuntimeException("No valid program data found");
			}

			dummyPackage.setPrograms(programs);
			return List.of(dummyPackage);
		}

		case "therapy": {
			TherapyWithSessions dummyPackage = new TherapyWithSessions();
			dummyPackage.setPackageId("PKG_AUTO");
			dummyPackage.setPackageName("Auto Package");

			Program dummyProgram = new Program();
			dummyProgram.setProgramId("PROG_AUTO");
			dummyProgram.setProgramName("Auto Program");

			List<TherapyData> therapyList = new ArrayList<>();
			for (TherapyWithSessions item : incoming) {
				if (item.getTherapyId() == null)
					continue;
				TherapyData therapy = new TherapyData();
				therapy.setTherapyId(item.getTherapyId());
				therapy.setTherapyName(item.getTherapyName());
				therapy.setExercises(item.getExercises());
				therapyList.add(therapy);
			}

			if (therapyList.isEmpty()) {
				throw new RuntimeException("No valid therapy data found");
			}

			dummyProgram.setTherapyData(therapyList);
			dummyPackage.setPrograms(List.of(dummyProgram));
			return List.of(dummyPackage);
		}

		case "exercise": {
			TherapyWithSessions dummyPackage = new TherapyWithSessions();
			dummyPackage.setPackageId("PKG_AUTO");
			dummyPackage.setPackageName("Auto Package");

			Program dummyProgram = new Program();
			dummyProgram.setProgramId("PROG_AUTO");
			dummyProgram.setProgramName("Auto Program");

			TherapyData dummyTherapy = new TherapyData();
			dummyTherapy.setTherapyId("THER_AUTO");
			dummyTherapy.setTherapyName("Auto Therapy");

			List<TherapyExercise> allExercises = new ArrayList<>();
			for (TherapyWithSessions item : incoming) {
				if (item.getExercises() != null) {
					allExercises.addAll(item.getExercises());
				}
			}

			if (allExercises.isEmpty()) {
				throw new RuntimeException("No valid exercise data found");
			}

			dummyTherapy.setExercises(allExercises);
			dummyProgram.setTherapyData(List.of(dummyTherapy));
			dummyPackage.setPrograms(List.of(dummyProgram));
			return List.of(dummyPackage);
		}

		default:
			throw new RuntimeException("Invalid serviceType: " + serviceType);
		}
	}

	// ========================================================
	// APPLY PAYMENT LEVEL
	// ========================================================
	private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {

		if (req.getPaymentLevel() == null || req.getPaymentTarget() == null)
			return;

		String level = req.getPaymentLevel().toUpperCase();
		String status = getStatus(record);

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

	// ========================================================
	// DISTRIBUTE PAYMENT TO SESSIONS
	// ========================================================
	private void distributePaymentToSessions(PaymentRecord record) {

		if (record.getTherapyWithSessions() == null)
			return;

		double remaining = record.getTotalPaid();
		double totalAmount = record.getTotalAmount();
		double finalAmount = record.getFinalAmount();
		double discountRatio = (totalAmount > 0) ? (finalAmount / totalAmount) : 1.0;

		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null)
							continue;

						double rawPrice = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;
						double effectivePrice = Math.round(rawPrice * discountRatio * 100.0) / 100.0;

						for (var s : ex.getSessions()) {
							if (remaining + 0.01 >= effectivePrice) {
								s.setPaymentStatus("Paid");
								remaining -= effectivePrice;
								remaining = Math.round(remaining * 100.0) / 100.0;
							} else {
								s.setPaymentStatus("Unpaid");
							}
						}
					}
				}
			}
		}
	}

	// ========================================================
	// PACKAGE STATUS UPDATE
	// ========================================================
	private void updatePackageStatus(PaymentRecord record, List<String> ids, String status) {
		if (ids == null)
			return;
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

	// ========================================================
	// PROGRAM STATUS UPDATE
	// ========================================================
	private void updateProgramStatus(PaymentRecord record, List<String> ids, String status) {
		if (ids == null)
			return;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
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

	// ========================================================
	// THERAPY STATUS UPDATE
	// ========================================================
	private void updateTherapyStatus(PaymentRecord record, List<String> ids, String status) {
		if (ids == null)
			return;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
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

	// ========================================================
	// EXERCISE STATUS UPDATE
	// ========================================================
	private void updateExerciseStatus(PaymentRecord record, List<String> ids, String status) {
		if (ids == null)
			return;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ids.contains(ex.getExerciseId())) {
							ex.setPaymentStatus(status);
						}
					}
				}
			}
		}
	}

	// ========================================================
	// SESSION PAY
	// ========================================================
	private void paySessions(PaymentRecord record, List<String> ids) {
		if (ids == null)
			return;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null)
							continue;
						for (var s : ex.getSessions()) {
							if (ids.contains(s.getSessionId())) {
								s.setPaymentStatus("Paid");
							}
						}
						boolean allPaid = ex.getSessions().stream()
								.allMatch(x -> "Paid".equalsIgnoreCase(x.getPaymentStatus()));
						ex.setPaymentStatus(allPaid ? "Paid" : "Unpaid");
					}
				}
			}
		}
	}

	// ========================================================
	// STATUS PROPAGATION
	// ========================================================
	private void updateStatuses(PaymentRecord record) {
		if (record.getTherapyWithSessions() == null)
			return;

		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null || ex.getSessions().isEmpty()) {
							ex.setPaymentStatus("Unpaid");
							continue;
						}
						boolean allPaid = ex.getSessions().stream()
								.allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));
						ex.setPaymentStatus(allPaid ? "Paid" : "Unpaid");
					}
					boolean allTherapyPaid = therapy.getExercises().stream()
							.allMatch(e -> "Paid".equalsIgnoreCase(e.getPaymentStatus()));
					therapy.setPaymentStatus(allTherapyPaid ? "Paid" : "Unpaid");
				}
				boolean allProgPaid = prog.getTherapyData().stream()
						.allMatch(t -> "Paid".equalsIgnoreCase(t.getPaymentStatus()));
				prog.setPaymentStatus(allProgPaid ? "Paid" : "Unpaid");
			}
			boolean allPkgPaid = pkg.getPrograms().stream()
					.allMatch(p -> "Paid".equalsIgnoreCase(p.getPaymentStatus()));
			pkg.setPaymentStatus(allPkgPaid ? "Paid" : "Unpaid");
		}
	}

	// ========================================================
	// CALCULATE OVERALL STATUS
	// ========================================================
	private String calculateOverallStatus(PaymentRecord record) {

		if (record.getTherapyWithSessions() == null)
			return "Pending";

		boolean allCompleted = true;
		boolean anyCompleted = false;

		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null)
							continue;
						for (var s : ex.getSessions()) {
							if ("Completed".equalsIgnoreCase(s.getStatus())) {
								anyCompleted = true;
							} else {
								allCompleted = false;
							}
						}
					}
				}
			}
		}

		if (allCompleted && anyCompleted)
			return "Completed";
		if (anyCompleted)
			return "Active";
		return "Pending";
	}

	// ========================================================
	// UPDATE BOOKING STATUS
	// ========================================================
	private void updateBookingStatus(PaymentRecord record) {

		if (record.getBookingId() == null || record.getBookingId().trim().isEmpty())
			return;

		try {
			BookingResponse request = new BookingResponse();
			request.setBookingId(record.getBookingId().trim());

			if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {
				request.setStatus("completed");
			} else if ("Active".equalsIgnoreCase(record.getOverallStatus())) {
				request.setStatus("in-progress");
			} else {
				request.setStatus("pending");
			}

			clinicAdminFeign.updateAppointment(request);

		} catch (Exception e) {
			log.error("Booking status update failed for bookingId={} : {}", record.getBookingId(), e.getMessage());
		}
	}

	// ========================================================
	// MAP TO RESPONSE
	// ========================================================
	private PaymentRecordResponse mapToResponse(PaymentRecord record) {

		PaymentRecordResponse res = new PaymentRecordResponse();

		res.setId(record.getId());
		res.setPayAfterService(record.isPayAfterService());
		res.setClinicId(record.getClinicId());
		res.setBranchId(record.getBranchId());
		res.setBookingId(record.getBookingId());
		res.setPatientId(record.getPatientId());
		res.setDoctorId(record.getDoctorId());
		res.setDoctorName(record.getDoctorName());
		res.setTherapistId(record.getTherapistId());
		res.setTherapistName(record.getTherapistName());
		res.setTherapistRecordId(record.getTherapistRecordId());
		res.setTreatmentName(record.getTreatmentName());
		res.setServiceType(record.getServiceType());
		res.setOverallStatus(record.getOverallStatus());
		res.setTotalAmount(record.getTotalAmount());
		res.setDiscountAmount(record.getDiscountAmount());
		res.setFinalAmount(record.getFinalAmount());
		res.setTotalPaid(record.getTotalPaid());
		res.setBalanceAmount(record.getBalanceAmount());
		res.setPaymentStatus(record.getPaymentStatus());
		res.setSessionStartDate(record.getSessionStartDate());
		res.setSessionEndDate(record.getSessionEndDate());
		res.setTotalSessionCount(record.getTotalSessionCount());
		res.setNoOfSessionCompletedCount(record.getNoOfSessionCompletedCount());
		res.setNoOfSessionCompletedStatus(record.isNoOfSessionCompletedStatus());
		res.setSessionTableCreatedStatus(record.isSessionTableCreatedStatus());
		res.setOverallReceiptNumber(record.getOverallReceiptNumber());
		res.setPaymentHistory(record.getPaymentHistory());

		String serviceType = record.getServiceType() != null ? record.getServiceType().toLowerCase() : "package";

		switch (serviceType) {
		case "package":
			res.setTherapyWithSessions(mapPackages(record));
			break;
		case "program":
			res.setTherapyWithSessions(mapPrograms(record));
			break;
		case "therapy":
			res.setTherapyWithSessions(mapTherapies(record));
			break;
		case "exercise":
			res.setTherapyWithSessions(mapExercises(record));
			break;
		default:
			res.setTherapyWithSessions(record.getTherapyWithSessions());
		}

		return res;
	}

	// ========================================================
	// PACKAGE MAPPER
	// ========================================================
	private List<PackageResponse> mapPackages(PaymentRecord record) {
		List<PackageResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			PackageResponse p = new PackageResponse();
			p.setPackageId(pkg.getPackageId());
			p.setPackageName(pkg.getPackageName());
			p.setTotalPackagePrice(pkg.getTotalPackagePrice());
			p.setPaymentStatus(pkg.getPaymentStatus());
			p.setPrograms(mapProgramList(pkg.getPrograms()));
			result.add(p);
		}
		return result;
	}

	// ========================================================
	// PROGRAM MAPPER
	// ========================================================
	private List<ProgramResponse> mapPrograms(PaymentRecord record) {
		List<ProgramResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() != null) {
				result.addAll(mapProgramList(pkg.getPrograms()));
			}
		}
		return result;
	}

	// ========================================================
	// THERAPY MAPPER
	// ========================================================
	private List<TherapyResponse> mapTherapies(PaymentRecord record) {
		List<TherapyResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() != null) {
					result.addAll(mapTherapyList(prog.getTherapyData()));
				}
			}
		}
		return result;
	}

	// ========================================================
	// EXERCISE MAPPER
	// ========================================================
	private List<ExerciseResponse> mapExercises(PaymentRecord record) {
		List<ExerciseResponse> result = new ArrayList<>();
		if (record.getTherapyWithSessions() == null)
			return result;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() != null) {
						result.addAll(mapExerciseList(therapy.getExercises()));
					}
				}
			}
		}
		return result;
	}

	// ========================================================
	// SHARED LIST MAPPERS
	// ========================================================
	private List<ProgramResponse> mapProgramList(List<Program> programs) {
		List<ProgramResponse> result = new ArrayList<>();
		if (programs == null)
			return result;
		for (var prog : programs) {
			ProgramResponse p = new ProgramResponse();
			p.setProgramId(prog.getProgramId());
			p.setProgramName(prog.getProgramName());
			p.setTotalProgramPrice(prog.getTotalProgramPrice());
			p.setPaymentStatus(prog.getPaymentStatus());
			p.setTherapyData(mapTherapyList(prog.getTherapyData()));
			result.add(p);
		}
		return result;
	}

	private List<TherapyResponse> mapTherapyList(List<TherapyData> therapies) {
		List<TherapyResponse> result = new ArrayList<>();
		if (therapies == null)
			return result;
		for (var t : therapies) {
			TherapyResponse tr = new TherapyResponse();
			tr.setTherapyId(t.getTherapyId());
			tr.setTherapyName(t.getTherapyName());
			tr.setTotalTherapyPrice(t.getTotalTherapyPrice());
			tr.setPaymentStatus(t.getPaymentStatus());
			tr.setExercises(mapExerciseList(t.getExercises()));
			result.add(tr);
		}
		return result;
	}

	private List<ExerciseResponse> mapExerciseList(List<TherapyExercise> exercises) {
		List<ExerciseResponse> result = new ArrayList<>();
		if (exercises == null)
			return result;
		for (TherapyExercise ex : exercises) {
			ExerciseResponse er = new ExerciseResponse();
			er.setExerciseId(ex.getExerciseId());
			er.setExerciseName(ex.getExerciseName());
			er.setPricePerSession(ex.getPricePerSession());
			er.setNoOfSessions(ex.getNoOfSessions());
			er.setDiscountPercentage(ex.getDiscountPercentage());
			er.setDiscountAmount(ex.getDiscountAmount());
			er.setGst(ex.getGst());
			er.setOtherTax(ex.getOtherTax());
			er.setTotalExercisePrice(ex.getTotalExercisePrice());
			er.setTotalPrice(ex.getTotalPrice());
			er.setPaymentStatus(ex.getPaymentStatus());
			er.setRepetitions(ex.getRepetitions());
			er.setFrequency(ex.getFrequency());
			er.setSets(ex.getSets());
			er.setYoutubeUrl(ex.getYoutubeUrl());
			er.setNotes(ex.getNotes());
			er.setTechnique(ex.getTechnique());
			er.setMachine(ex.getMachine());
			er.setIntensity(ex.getIntensity());
			er.setAssistanceLevel(ex.getAssistanceLevel());
			er.setType(ex.getType());
			er.setArea(ex.getArea());
			er.setMetric(ex.getMetric());
			er.setValue(ex.getValue());
			er.setUnit(ex.getUnit());
			er.setBodyPart(ex.getBodyPart());
			er.setActivityType(ex.getActivityType());
			er.setActivityDuration(ex.getActivityDuration());
			er.setSessions(ex.getSessions());
			result.add(er);
		}
		return result;
	}

	// ========================================================
	// UTILS
	// ========================================================
	private String getStatus(PaymentRecord r) {
		if (r.getTotalPaid() <= 0)
			return "Unpaid";
		if (r.getTotalPaid() < r.getFinalAmount())
			return "Partial";
		if (Double.compare(r.getTotalPaid(), r.getFinalAmount()) == 0)
			return "Paid";
		return "Overpaid";
	}

	private PaymentHistory buildHistory(PaymentRequest req, String receiptNumber) {

		PaymentHistory history = new PaymentHistory();

		history.setAmount(req.getAmount());
		history.setPaymentMode(req.getPaymentMode());
		history.setPaymentType(req.getPaymentType());
		history.setPaymentDate(req.getPaymentDate());
		history.setPaymentLevel(req.getPaymentLevel());
		history.setDiscountAmount(req.getDiscountAmount());
		history.setDiscountIssuedBy(req.getDiscountIssuedBy());
		history.setReceiptNumber(receiptNumber);

		return history;
	}

	private String generateOverallReceiptNumber() {

		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

		return "OVR-" + date + "-" + random;
	}

	private String generateReceiptNumber() {

		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

		String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

		return "PAY-" + dateTime + "-" + random;
	}

	// ========================================================
	// CALCULATE TOTAL
	// ========================================================
	private double calculateTotal(List<TherapyWithSessions> data) {

		double total = 0;

		for (var pkg : data) {
			double pkgTotal = 0;
			for (var prog : pkg.getPrograms()) {
				double progTotal = 0;
				for (var therapy : prog.getTherapyData()) {
					double therapyTotal = 0;
					for (var ex : therapy.getExercises()) {
						double exTotal;
						if (ex.getTotalExercisePrice() != null && ex.getTotalExercisePrice() > 0) {
							exTotal = ex.getTotalExercisePrice();
						} else {
							exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
							ex.setTotalExercisePrice(exTotal);
						}
						therapyTotal += exTotal;
					}
					therapy.setTotalTherapyPrice(therapyTotal);
					progTotal += therapyTotal;
				}
				prog.setTotalProgramPrice(progTotal);
				pkgTotal += progTotal;
			}
			pkg.setTotalPackagePrice(pkgTotal);
			total += pkgTotal;
		}

		return total;
	}

	// ========================================================
	// COUNT TOTAL SESSIONS
	// ========================================================
	private int countTotalSessions(PaymentRecord record) {
		int count = 0;
		if (record.getTherapyWithSessions() == null)
			return count;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() != null) {
							count += ex.getSessions().size();
						}
					}
				}
			}
		}
		return count;
	}

	// ========================================================
	// CREATE SESSIONS
	// ========================================================
	private boolean createSessions(List<TherapyWithSessions> data, String startDate) {

		boolean created = false;

		for (var pkg : data) {
			for (var prog : pkg.getPrograms()) {
				for (var therapy : prog.getTherapyData()) {
					for (var ex : therapy.getExercises()) {

						List<Session> sessions = new ArrayList<>();
						LocalDate currentDate = LocalDate.parse(startDate);
						int noOfSessions = ex.getNoOfSessions();
						String freqType = parseFrequencyType(ex.getFrequency());
						int sessionNo = 1;

						if (freqType.equals("day")) {
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(1);
							}
						} else if (freqType.equals("week")) {
							int gapDays = noOfSessions > 0 ? 7 / noOfSessions : 1;
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(gapDays);
							}
						} else if (freqType.equals("month")) {
							int gapDays = noOfSessions > 0 ? 30 / noOfSessions : 1;
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(gapDays);
							}
						}

						if (!sessions.isEmpty())
							created = true;
						ex.setSessions(sessions);
					}
				}
			}
		}

		return created;
	}

	// ========================================================
	// BUILD SESSION HELPER
	// ========================================================
	private Session buildSession(String exerciseId, int sessionNo, LocalDate date) {
		String uniqueSessionId = exerciseId + "_" + sessionNo + "_"
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		return new Session(uniqueSessionId, sessionNo, date.toString(), "Pending", "Unpaid");
	}

	// ========================================================
	// PARSE FREQUENCY TYPE
	// ========================================================
	private String parseFrequencyType(String frequency) {
		if (frequency == null || frequency.trim().isEmpty()) {
			return "day";
		}
		String lower = frequency.toLowerCase().trim();
		if (lower.contains("month"))
			return "month";
		if (lower.contains("week"))
			return "week";
		return "day";
	}

	// ========================================================
	// COUNT COMPLETED
	// ========================================================
	private int countCompleted(PaymentRecord record) {
		int count = 0;
		if (record.getTherapyWithSessions() == null)
			return count;
		for (var pkg : record.getTherapyWithSessions()) {
			if (pkg.getPrograms() == null)
				continue;
			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;
				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;
					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null)
							continue;
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

	// ========================================================
	// GET EXERCISE SESSIONS WITH RECORDS
	// ========================================================
	@Override
	public Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
			String therapistId, String therapistRecordId) {

		Response response = new Response();

		try {
			PaymentRecord record = repo
					.findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistIdAndTherapistRecordId(clinicId,
							branchId, bookingId, patientId, therapistId, therapistRecordId)
					.get();

			List<Object> exerciseList = new ArrayList<>();

			if (record != null) {
				for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {
					if (pkg.getPrograms() == null)
						continue;
					for (Program program : pkg.getPrograms()) {
						if (program.getTherapyData() == null)
							continue;
						for (TherapyData therapy : program.getTherapyData()) {
							if (therapy.getExercises() == null)
								continue;
							for (TherapyExercise exercise : therapy.getExercises()) {
								List<Object> sessionList = new ArrayList<>();
								for (Session session : exercise.getSessions()) {
									Map<String, Object> map = new LinkedHashMap<>();
									map.put("sessionId", session.getSessionId());
									map.put("sessionNo", session.getSessionNo());
									map.put("date", session.getDate());
									map.put("paymentStatus", session.getPaymentStatus());
									try {
										ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr = clinicAdminFeign
												.getRecordBySession(clinicId, branchId, bookingId, patientId,
														session.getSessionId());
										if (tr != null && tr.getBody() != null && tr.getBody().getData() != null) {
											map.put("status", "Completed");
										} else {
											map.put("status", session.getStatus());
										}
									} catch (Exception e) {
										map.put("status", session.getStatus());
									}
									sessionList.add(map);
								}
								Map<String, Object> exerciseData = new LinkedHashMap<>();
								exerciseData.put("exerciseId", exercise.getExerciseId());
								exerciseData.put("exerciseName", exercise.getExerciseName());
								exerciseData.put("sessions", sessionList);
								exerciseList.add(exerciseData);
							}
						}
					}
					response.setSuccess(true);
					response.setStatus(200);
					response.setMessage("All exercises fetched successfully");
					response.setData(exerciseList);
				}
			} else {
				response.setSuccess(false);
				response.setStatus(200);
				response.setMessage("Record not found");
			}
		} catch (Exception e) {
			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}

		return response;
	}
	
	
	@Override
	public int getTodaySessionCount(String clinicId,
	                                     String branchId,
	                                     String therapistId) {

	    Response response = new Response();

	    try {

	        List<PaymentRecord> records =
	                repo.findByClinicIdAndBranchIdAndTherapistId(
	                        clinicId,
	                        branchId,
	                        therapistId);

	        if (records == null || records.isEmpty()) {

	            return 0;
	        }

	        String today = LocalDate.now()
	                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

	        int todaySessionCount = 0;

	        for (PaymentRecord record : records) {

	            if (record.getTherapyWithSessions() == null)
	                continue;

	            for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {

	                if (pkg.getPrograms() == null)
	                    continue;

	                for (Program program : pkg.getPrograms()) {

	                    if (program.getTherapyData() == null)
	                        continue;

	                    for (TherapyData therapy : program.getTherapyData()) {

	                        if (therapy.getExercises() == null)
	                            continue;

	                        for (TherapyExercise exercise : therapy.getExercises()) {

	                            if (exercise.getSessions() == null)
	                                continue;

	                            for (Session session : exercise.getSessions()) {

	                                if (today.equals(session.getDate())) {
	                                    todaySessionCount++;
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }

	       return todaySessionCount;

	    } catch (Exception e) {

	       return 0;
	    }

	}

	// ========================================================
	// GET COMPLETED THERAPY RECORD
	// ========================================================
	@Override
	public Response getCompletedTherapyRecord(String clinicId, String branchId, String therapistRecordId,
			String sessionId) {

		Response response = new Response();

		try {
			ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr = clinicAdminFeign
					.getCompletedTherapyRecord(clinicId, branchId, therapistRecordId, sessionId);

			if (tr != null && tr.getBody() != null && tr.getBody().getData() != null) {
				TherapistRecordDTO dto = tr.getBody().getData();
				dto.setConsentPdfUrl(refreshSignedUrl(dto.getConsentPdfUrl()));
				dto.setBeforeImage(refreshSignedUrl(dto.getBeforeImage()));
				dto.setAfterImage(refreshSignedUrl(dto.getAfterImage()));
				dto.setBeforeVideo(refreshSignedUrl(dto.getBeforeVideo()));
				dto.setAfterVideo(refreshSignedUrl(dto.getAfterVideo()));
				dto.setVoiceRecord(refreshSignedUrl(dto.getVoiceRecord()));
				response.setSuccess(true);
				response.setStatus(200);
				response.setMessage("Therapy record fetched successfully");
				response.setData(dto);
			} else {
				response.setSuccess(false);
				response.setStatus(404);
				response.setMessage("Therapy record not found");
			}
		} catch (Exception e) {
			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}

		return response;
	}

	private String refreshSignedUrl(String signedUrl) {
		if (signedUrl == null || signedUrl.isBlank())
			return signedUrl;
		try {
			String fileKey = extractKey(signedUrl);
			ResponseEntity<String> result = clinicAdminFeign.getSignedUrl(fileKey);
			if (result != null && result.getBody() != null) {
				return result.getBody();
			}
		} catch (Exception e) {
			log.error("Failed to refresh signed URL: {}", e.getMessage());
		}
		return signedUrl;
	}

	private String extractKey(String signedUrl) {
		try {
			String withoutQuery = signedUrl.split("\\?")[0];
			java.net.URI uri = new java.net.URI(withoutQuery);
			String path = uri.getPath();
			return path.startsWith("/") ? path.substring(1) : path;
		} catch (Exception e) {
			return signedUrl;
		}
	}

	// ========================================================
	// FIND BY CLINIC AND BRANCH
	// ========================================================
	@Override
	public List<PaymentRecordResponse> findByClinicIdAndBranchId(String clinicId, String branchId) {

		return repo.findByClinicIdAndBranchId(clinicId, branchId).stream().map(this::mapToResponse).toList();
	}

	private String getLastSessionDate(List<TherapyWithSessions> data) {

		LocalDate lastDate = null;

		for (var pkg : data) {
			if (pkg.getPrograms() == null)
				continue;

			for (var prog : pkg.getPrograms()) {
				if (prog.getTherapyData() == null)
					continue;

				for (var therapy : prog.getTherapyData()) {
					if (therapy.getExercises() == null)
						continue;

					for (var ex : therapy.getExercises()) {
						if (ex.getSessions() == null || ex.getSessions().isEmpty())
							continue;

						Session lastSession = ex.getSessions().get(ex.getSessions().size() - 1);

						LocalDate sessionDate = LocalDate.parse(lastSession.getDate());

						if (lastDate == null || sessionDate.isAfter(lastDate)) {
							lastDate = sessionDate;
						}
					}
				}
			}
		}

		return lastDate != null ? lastDate.toString() : null;
	}
}