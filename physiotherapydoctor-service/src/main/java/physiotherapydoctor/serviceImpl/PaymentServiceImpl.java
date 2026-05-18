package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import physiotherapydoctor.dto.response.ExerciseResponse;
import physiotherapydoctor.dto.response.PackageResponse;
import physiotherapydoctor.dto.response.PaymentRecordResponse;
import physiotherapydoctor.dto.response.ProgramResponse;
import physiotherapydoctor.dto.response.TherapyResponse;
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
	private ClinicAdminFeign clinicAdminFeign;

	// ========================================================
	// CREATE
	// ========================================================
	@Override
	public PaymentRecordResponse createPayment(PaymentRequest req) {

		if (repo.findByBookingId(req.getBookingId()).isPresent()) {
			throw new RuntimeException("Already exists, use update");
		}

		if (req.getAmount() == null || req.getAmount() <= 0) {
			throw new RuntimeException("Amount must be greater than 0");
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

		record.setServiceType(req.getServiceType());

		// ================= SESSION =================
		record.setSessionStartDate(req.getSessionStartDate());

		// ================= TOTAL =================
		double total = calculateTotal(req.getTherapyWithSessions());
		double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
		double finalAmount = total - discount;

		record.setTotalAmount(total);
		record.setDiscountAmount(discount);
		record.setFinalAmount(finalAmount);

		double amount = req.getAmount();

		// ================= PAYMENT =================
		record.setTotalPaid(amount);
		record.setBalanceAmount(finalAmount - amount);
		record.setPaymentStatus(getStatus(record));

		// ================= CREATE SESSIONS =================
		boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
		record.setSessionTableCreatedStatus(created);

		// ✅ STEP 2: Set normalized data on record BEFORE distribute/status calls
		record.setTherapyWithSessions(req.getTherapyWithSessions());

		// ✅ AUTO-COUNT total sessions
		record.setTotalSessionCount(countTotalSessions(record));

		// ✅ STEP 3: Distribute payment across sessions
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

		// ✅ STEP 4: Save and map to clean response
		return mapToResponse(repo.save(record));
	}

	// ========================================================
	// UPDATE
	// ========================================================
	@Override
	public PaymentRecordResponse updatePayment(PaymentRequest req) {

		PaymentRecord record = repo.findByBookingId(req.getBookingId())
				.orElseThrow(() -> new RuntimeException("Payment not found"));

		if (req.getTherapyWithSessions() != null) {
			throw new RuntimeException("Do not send therapyWithSessions in update");
		}

		if (req.getAmount() == null || req.getAmount() <= 0) {
			throw new RuntimeException("Amount must be greater than 0");
		}

		double currentPaid = record.getTotalPaid();
		double finalAmount = record.getFinalAmount();
		double remaining = finalAmount - currentPaid;

		// ================= OVERPAYMENT PREVENTION =================
		double newPaid = currentPaid + req.getAmount();

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
		record.getPaymentHistory().add(buildHistory(req));

		// ================= STATUS PROPAGATION =================
		updateStatuses(record);

		return mapToResponse(repo.save(record));
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
						double price = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;
						for (var s : ex.getSessions()) {
							if (remaining >= price) {
								s.setPaymentStatus("Paid");
								remaining -= price;
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
			System.out.println("Booking status update failed");
			e.printStackTrace();
		}
	}

	// ========================================================
	// MAP TO RESPONSE
	// ========================================================
	private PaymentRecordResponse mapToResponse(PaymentRecord record) {

		PaymentRecordResponse res = new PaymentRecordResponse();

		res.setId(record.getId());
		res.setClinicId(record.getClinicId());
		res.setBranchId(record.getBranchId());
		res.setBookingId(record.getBookingId());
		res.setPatientId(record.getPatientId());
		res.setDoctorId(record.getDoctorId());
		res.setDoctorName(record.getDoctorName());
		res.setTherapistId(record.getTherapistId());
		res.setTherapistName(record.getTherapistName());
		res.setTherapistRecordId(record.getTherapistRecordId());
		res.setServiceType(record.getServiceType());
		res.setOverallStatus(record.getOverallStatus());
		res.setTotalAmount(record.getTotalAmount());
		res.setDiscountAmount(record.getDiscountAmount());
		res.setFinalAmount(record.getFinalAmount());
		res.setTotalPaid(record.getTotalPaid());
		res.setBalanceAmount(record.getBalanceAmount());
		res.setPaymentStatus(record.getPaymentStatus());
		res.setSessionStartDate(record.getSessionStartDate());
		res.setTotalSessionCount(record.getTotalSessionCount());
		res.setNoOfSessionCompletedCount(record.getNoOfSessionCompletedCount());
		res.setNoOfSessionCompletedStatus(record.isNoOfSessionCompletedStatus());
		res.setSessionTableCreatedStatus(record.isSessionTableCreatedStatus());
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

	private PaymentHistory buildHistory(PaymentRequest req) {
		return new PaymentHistory(req.getAmount(), req.getPaymentMode(), req.getPaymentType(), req.getPaymentDate(),
				req.getPaymentLevel(), req.getDiscountAmount(), req.getDiscountIssuedBy());
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

						// ✅ Use frontend totalExercisePrice if provided
						if (ex.getTotalExercisePrice() != null && ex.getTotalExercisePrice() > 0) {
							exTotal = ex.getTotalExercisePrice();
						} else {
							// ✅ Auto calculate
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

							// ✅ 1 session per day
							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(1);
							}

						} else if (freqType.equals("week")) {

							// ✅ noOfSessions spread in 1 week (7 days)
							int gapDays = noOfSessions > 0 ? 7 / noOfSessions : 1;

							for (int i = 1; i <= noOfSessions; i++) {
								sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
								sessionNo++;
								currentDate = currentDate.plusDays(gapDays);
							}

						} else if (freqType.equals("month")) {

							// ✅ noOfSessions spread in 1 month (30 days)
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
	// PARSE FREQUENCY TYPE — only day / week / month
	// ========================================================
	private String parseFrequencyType(String frequency) {

		if (frequency == null || frequency.trim().isEmpty()) {
			return "day"; // default
		}

		String lower = frequency.toLowerCase().trim();

		if (lower.contains("month"))
			return "month";
		if (lower.contains("week"))
			return "week";
		return "day"; // default
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
//    @Override
//    public Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId,
//            String patientId, String therapistRecordId) {
//
//        Response response = new Response();
//
//        try {
//
//            PaymentRecord record = repo
//                    .findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistRecordId(
//                            clinicId, branchId, bookingId, patientId, therapistRecordId)
//                    .orElseThrow(() -> new RuntimeException("Payment record not found"));
//
//            List<Object> exerciseList = new ArrayList<>();
//            ObjectMapper mapper = new ObjectMapper();
//
//            for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {
//                if (pkg.getPrograms() == null) continue;
//
//                for (Program program : pkg.getPrograms()) {
//                    if (program.getTherapyData() == null) continue;
//
//                    for (TherapyData therapy : program.getTherapyData()) {
//                        if (therapy.getExercises() == null) continue;
//
//                        for (TherapyExercise exercise : therapy.getExercises()) {
//
//                            List<Object> sessionList = new ArrayList<>();
//
//                            for (Session session : exercise.getSessions()) {
//
//                                Map<String, Object> map = new LinkedHashMap<>();
//                                map.put("sessionId", session.getSessionId());
//                                map.put("sessionNo", session.getSessionNo());
//                                map.put("date", session.getDate());
//                                map.put("paymentStatus", session.getPaymentStatus());
//
//                                try {
//                                    ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
//                                            clinicAdminFeign.getRecordBySession(
//                                                    clinicId, branchId, bookingId, patientId,
//                                                    session.getSessionId());
//
//                                    if (tr != null && tr.getBody() != null
//                                            && tr.getBody().getData() != null) {
//
//                                        TherapistRecordDTO dto = mapper.convertValue(
//                                                tr.getBody().getData(),
//                                                TherapistRecordDTO.class);
//
//                                        map.put("status", "Completed");
//                                        map.put("therapistRecord", dto);
//
//                                    } else {
//                                        map.put("status", session.getStatus());
//                                        map.put("therapistRecord", null);
//                                    }
//
//                                } catch (Exception e) {
//                                    map.put("status", session.getStatus());
//                                    map.put("therapistRecord", null);
//                                }
//
//                                sessionList.add(map);
//                            }
//
//                            Map<String, Object> exerciseData = new LinkedHashMap<>();
//                            exerciseData.put("exerciseId", exercise.getExerciseId());
//                            exerciseData.put("exerciseName", exercise.getExerciseName());
//                            exerciseData.put("sessions", sessionList);
//
//                            exerciseList.add(exerciseData);
//                        }
//                    }
//                }
//            }
//
//            response.setSuccess(true);
//            response.setStatus(200);
//            response.setMessage("All exercises fetched successfully");
//            response.setData(exerciseList);
//
//        } catch (Exception e) {
//            response.setSuccess(false);
//            response.setStatus(500);
//            response.setMessage(e.getMessage());
//        }
//
//        return response;
//    }
	@Override
	public Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId, String patientId,
			String therapistRecordId) {

		Response response = new Response();

		try {

			PaymentRecord record = repo
					.findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistRecordId(clinicId, branchId,
							bookingId, patientId, therapistRecordId)
					.orElseThrow(() -> new RuntimeException("Payment record not found"));

			List<Object> exerciseList = new ArrayList<>();

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
			}

			response.setSuccess(true);
			response.setStatus(200);
			response.setMessage("All exercises fetched successfully");
			response.setData(exerciseList);

		} catch (Exception e) {

			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}

		return response;
	}
//    -------------------------------------check completed record-----------------------------
	@Override
	public Response getCompletedTherapyRecord(
	        String clinicId,
	        String branchId,
	        String therapistRecordId,
	        String sessionId) {

	    Response response = new Response();

	    try {

	        ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
	                clinicAdminFeign.getCompletedTherapyRecord(
	                        clinicId,
	                        branchId,
	                        therapistRecordId,
	                        sessionId);

	        if (tr != null
	                && tr.getBody() != null
	                && tr.getBody().getData() != null) {

	            response.setSuccess(true);
	            response.setStatus(200);
	            response.setMessage("Therapy record fetched successfully");
	            response.setData(tr.getBody().getData());

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
	
	// ========================================================
	// FIND BY CLINIC AND BRANCH
	// ========================================================
	@Override
	public List<PaymentRecordResponse> findByClinicIdAndBranchId(String clinicId, String branchId) {

		List<PaymentRecord> records = repo.findByClinicIdAndBranchId(clinicId, branchId);

		if (records == null || records.isEmpty()) {
			throw new RuntimeException("No payment records found");
		}

		return records.stream().map(this::mapToResponse).toList();
	}
}
//package physiotherapydoctor.serviceImpl;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import lombok.RequiredArgsConstructor;
//import physiotherapydoctor.dto.BookingResponse;
//import physiotherapydoctor.dto.PaymentHistory;
//import physiotherapydoctor.dto.PaymentRequest;
//import physiotherapydoctor.dto.Program;
//import physiotherapydoctor.dto.Response;
//import physiotherapydoctor.dto.ResponseStructure;
//import physiotherapydoctor.dto.Session;
//import physiotherapydoctor.dto.TherapistRecordDTO;
//import physiotherapydoctor.dto.TherapyData;
//import physiotherapydoctor.dto.TherapyExercise;
//import physiotherapydoctor.dto.TherapyWithSessions;
//import physiotherapydoctor.dto.response.ExerciseResponse;
//import physiotherapydoctor.dto.response.PackageResponse;
//import physiotherapydoctor.dto.response.PaymentRecordResponse;
//import physiotherapydoctor.dto.response.ProgramResponse;
//import physiotherapydoctor.dto.response.TherapyResponse;
//import physiotherapydoctor.entity.PaymentRecord;
//import physiotherapydoctor.feign.BookingFeign;
//import physiotherapydoctor.feign.ClinicAdminFeign;
//import physiotherapydoctor.repository.PaymentRepository;
//import physiotherapydoctor.service.PaymentService;
//
//@Service
//@RequiredArgsConstructor
//public class PaymentServiceImpl implements PaymentService {
//
//	private final PaymentRepository repo;
//
//	@Autowired
//	private BookingFeign bookingFeign;
//
//	@Autowired
//	private ClinicAdminFeign clinicAdminFeign;
//
//	// ========================================================
//	// CREATE
//	// ========================================================
//	@Override
//	public PaymentRecordResponse createPayment(PaymentRequest req) {
//
//		if (repo.findByBookingId(req.getBookingId()).isPresent()) {
//			throw new RuntimeException("Already exists, use update");
//		}
//
//		if (req.getAmount() == null || req.getAmount() <= 0) {
//			throw new RuntimeException("Amount must be greater than 0");
//		}
//
//		if (req.getTherapyWithSessions() == null || req.getTherapyWithSessions().isEmpty()) {
//			throw new RuntimeException("therapyWithSessions is required");
//		}
//
//		// ✅ STEP 1: Normalize payload to full Package structure
//		List<TherapyWithSessions> normalized = normalizePayload(req);
//		req.setTherapyWithSessions(normalized);
//
//		PaymentRecord record = new PaymentRecord();
//
//		// ================= BASIC =================
//		record.setClinicId(req.getClinicId());
//		record.setBranchId(req.getBranchId());
//		record.setBookingId(req.getBookingId());
//		record.setPatientId(req.getPatientId());
//		record.setOverallStatus("Pending");
//
//		record.setDoctorId(req.getDoctorId());
//		record.setDoctorName(req.getDoctorName());
//
//		record.setTherapistId(req.getTherapistId());
//		record.setTherapistName(req.getTherapistName());
//		record.setTherapistRecordId(req.getTherapistRecordId());
//
//		record.setServiceType(req.getServiceType());
//
//		// ================= SESSION =================
//		record.setSessionStartDate(req.getSessionStartDate());
//		record.setTotalSessionCount(req.getTotalSessionCount());
//
//		// ================= TOTAL =================
//		double total = calculateTotal(req.getTherapyWithSessions());
//		double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
//		double finalAmount = total - discount;
//
//		record.setTotalAmount(total);
//		record.setDiscountAmount(discount);
//		record.setFinalAmount(finalAmount);
//
//		double amount = req.getAmount();
//
//		// ================= VALIDATIONS =================
////		if (amount > finalAmount) {
////			throw new RuntimeException("Amount exceeds final payable amount: " + finalAmount);
////		}
////
////		if ("FULL".equalsIgnoreCase(req.getPaymentType()) && amount != finalAmount) {
////			throw new RuntimeException("Full payment must be exactly: " + finalAmount);
////		}
//
//		// ================= PAYMENT =================
//		record.setTotalPaid(amount);
//		record.setBalanceAmount(finalAmount - amount);
//		record.setPaymentStatus(getStatus(record));
//
//		// ================= CREATE SESSIONS =================
//		boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
//		record.setSessionTableCreatedStatus(created);
//
//		// ✅ STEP 2: Set normalized data on record BEFORE distribute/status calls
//		record.setTherapyWithSessions(req.getTherapyWithSessions());
//
//		// ✅ AUTO-COUNT total sessions (remove manual req.getTotalSessionCount())
//		record.setTotalSessionCount(countTotalSessions(record));
//		
//		// ✅ STEP 3: Distribute payment across sessions
//		distributePaymentToSessions(record);
//
//		// ================= HISTORY =================
//		record.setPaymentHistory(new ArrayList<>());
//		record.getPaymentHistory().add(buildHistory(req));
//
//		// ================= APPLY LEVEL =================
//		if (req.getPaymentTarget() != null) {
//			applyPaymentLevel(record, req);
//		}
//
//		// ================= STATUS PROPAGATION =================
//		updateStatuses(record);
//
//		// ✅ STEP 4: Save and map to clean response
//		return mapToResponse(repo.save(record));
//	}
//
//	// ========================================================
//	// UPDATE
//	// ========================================================
//	@Override
//	public PaymentRecordResponse updatePayment(PaymentRequest req) {
//
//		PaymentRecord record = repo.findByBookingId(req.getBookingId())
//				.orElseThrow(() -> new RuntimeException("Payment not found"));
//
//		if (req.getTherapyWithSessions() != null) {
//			throw new RuntimeException("Do not send therapyWithSessions in update");
//		}
//
//		if (req.getAmount() == null || req.getAmount() <= 0) {
//			throw new RuntimeException("Amount must be greater than 0");
//		}
//
////		if (req.getPaymentTarget() == null) {
////			throw new RuntimeException("paymentTarget is required");
////		}
//
//		double currentPaid = record.getTotalPaid();
//		double finalAmount = record.getFinalAmount();
//		double remaining = finalAmount - currentPaid;
//
//		// ================= FULL PAYMENT VALIDATION =================
////		if ("FULL".equalsIgnoreCase(req.getPaymentType())) {
////			if (req.getAmount() != remaining) {
////				throw new RuntimeException("Full payment must be exactly remaining amount: " + remaining);
////			}
////		}
//
//		// ================= OVERPAYMENT PREVENTION =================
//		double newPaid = currentPaid + req.getAmount();
//
//		if (newPaid > finalAmount) {
//			throw new RuntimeException("Payment exceeds final amount. Remaining payable: " + remaining);
//		}
//
//		// ================= UPDATE AMOUNT =================
//		record.setTotalPaid(newPaid);
//		record.setBalanceAmount(finalAmount - newPaid);
//		record.setPaymentStatus(getStatus(record));
//
//		// ================= DISTRIBUTE =================
//		distributePaymentToSessions(record);
//
//		// ================= APPLY PAYMENT LEVEL =================
//		applyPaymentLevel(record, req);
//
//		// ================= SESSION COMPLETION =================
//		int completed = countCompleted(record);
//		record.setNoOfSessionCompletedCount(completed);
//		record.setNoOfSessionCompletedStatus(completed >= record.getTotalSessionCount());
//
//		// ================= HISTORY =================
//		record.getPaymentHistory().add(buildHistory(req));
//
//		// ================= STATUS PROPAGATION =================
//		updateStatuses(record);
//
//		return mapToResponse(repo.save(record));
//	}
//
//	// ========================================================
//	// GET BY BOOKING ID
//	// ========================================================
//	@Override
//	public PaymentRecordResponse getByBookingId(String bookingId) {
//
//		PaymentRecord record = repo.findByBookingId(bookingId)
//				.orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));
//
//		int completed = countCompleted(record);
//		record.setNoOfSessionCompletedCount(completed);
//		record.setNoOfSessionCompletedStatus(completed >= record.getTotalSessionCount());
//
//		return mapToResponse(record);
//	}
//
//	// ========================================================
//	// DELETE BY BOOKING ID
//	// ========================================================
//	@Override
//	public void deleteByBookingId(String bookingId) {
//
//		PaymentRecord record = repo.findByBookingId(bookingId)
//				.orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));
//
//		repo.delete(record);
//	}
//
//	// ========================================================
//	// UPDATE SESSION STATUS FROM THERAPIST
//	// ========================================================
//	@Override
//	public void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId) {
//
//		// ✅ Get all records for this therapist
//		List<PaymentRecord> records = repo.findByTherapistRecordId(therapistRecordId);
//
//		if (records == null || records.isEmpty()) {
//			throw new RuntimeException("No payment records found for therapistRecordId: " + therapistRecordId);
//		}
//
//		PaymentRecord targetRecord = null;
//		boolean sessionFound = false;
//
//		// ✅ Search session across all records
//		outer: for (PaymentRecord record : records) {
//
//			List<TherapyWithSessions> packageList = record.getTherapyWithSessions();
//			if (packageList == null || packageList.isEmpty())
//				continue;
//
//			for (TherapyWithSessions pkg : packageList) {
//				if (pkg.getPrograms() == null)
//					continue;
//
//				for (Program program : pkg.getPrograms()) {
//					if (program.getTherapyData() == null)
//						continue;
//
//					for (TherapyData therapy : program.getTherapyData()) {
//						if (therapy.getExercises() == null)
//							continue;
//
//						for (TherapyExercise exercise : therapy.getExercises()) {
//							if (exercise.getSessions() == null)
//								continue;
//
//							for (Session session : exercise.getSessions()) {
//								if (sessionId.equals(session.getSessionId())) {
//									session.setStatus("Completed");
//									targetRecord = record; // ✅ Found which record
//									sessionFound = true;
//									break outer;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//
//		if (!sessionFound || targetRecord == null) {
//			throw new RuntimeException("Session not found with ID: " + sessionId);
//		}
//
//		// ✅ Update only the record that contains the session
//		targetRecord.setOverallStatus(calculateOverallStatus(targetRecord));
//
//		repo.save(targetRecord);
//
//		updateBookingStatus(targetRecord);
//	}
//
//	// ========================================================
//	// NORMALIZE PAYLOAD
//	// ========================================================
//	private List<TherapyWithSessions> normalizePayload(PaymentRequest req) {
//
//		String serviceType = req.getServiceType() != null ? req.getServiceType().toLowerCase() : "package";
//
//		List<TherapyWithSessions> incoming = req.getTherapyWithSessions();
//
//		if (incoming == null || incoming.isEmpty()) {
//			throw new RuntimeException("therapyWithSessions is required");
//		}
//
//		switch (serviceType) {
//
//		case "package":
//			return incoming;
//
//		case "program": {
//			TherapyWithSessions dummyPackage = new TherapyWithSessions();
//			dummyPackage.setPackageId("PKG_AUTO");
//			dummyPackage.setPackageName("Auto Package");
//
//			List<Program> programs = new ArrayList<>();
//			for (TherapyWithSessions item : incoming) {
//				if (item.getProgramId() == null)
//					continue;
//				Program prog = new Program();
//				prog.setProgramId(item.getProgramId());
//				prog.setProgramName(item.getProgramName());
//				prog.setTherapyData(item.getTherapyData());
//				programs.add(prog);
//			}
//
//			if (programs.isEmpty()) {
//				throw new RuntimeException("No valid program data found");
//			}
//
//			dummyPackage.setPrograms(programs);
//			return List.of(dummyPackage);
//		}
//
//		case "therapy": {
//			TherapyWithSessions dummyPackage = new TherapyWithSessions();
//			dummyPackage.setPackageId("PKG_AUTO");
//			dummyPackage.setPackageName("Auto Package");
//
//			Program dummyProgram = new Program();
//			dummyProgram.setProgramId("PROG_AUTO");
//			dummyProgram.setProgramName("Auto Program");
//
//			List<TherapyData> therapyList = new ArrayList<>();
//			for (TherapyWithSessions item : incoming) {
//				if (item.getTherapyId() == null)
//					continue;
//				TherapyData therapy = new TherapyData();
//				therapy.setTherapyId(item.getTherapyId());
//				therapy.setTherapyName(item.getTherapyName());
//				therapy.setExercises(item.getExercises());
//				therapyList.add(therapy);
//			}
//
//			if (therapyList.isEmpty()) {
//				throw new RuntimeException("No valid therapy data found");
//			}
//
//			dummyProgram.setTherapyData(therapyList);
//			dummyPackage.setPrograms(List.of(dummyProgram));
//			return List.of(dummyPackage);
//		}
//
//		case "exercise": {
//			TherapyWithSessions dummyPackage = new TherapyWithSessions();
//			dummyPackage.setPackageId("PKG_AUTO");
//			dummyPackage.setPackageName("Auto Package");
//
//			Program dummyProgram = new Program();
//			dummyProgram.setProgramId("PROG_AUTO");
//			dummyProgram.setProgramName("Auto Program");
//
//			TherapyData dummyTherapy = new TherapyData();
//			dummyTherapy.setTherapyId("THER_AUTO");
//			dummyTherapy.setTherapyName("Auto Therapy");
//
//			List<TherapyExercise> allExercises = new ArrayList<>();
//			for (TherapyWithSessions item : incoming) {
//				if (item.getExercises() != null) {
//					allExercises.addAll(item.getExercises());
//				}
//			}
//
//			if (allExercises.isEmpty()) {
//				throw new RuntimeException("No valid exercise data found");
//			}
//
//			dummyTherapy.setExercises(allExercises);
//			dummyProgram.setTherapyData(List.of(dummyTherapy));
//			dummyPackage.setPrograms(List.of(dummyProgram));
//			return List.of(dummyPackage);
//		}
//
//		default:
//			throw new RuntimeException("Invalid serviceType: " + serviceType);
//		}
//	}
//
//	// ========================================================
//	// APPLY PAYMENT LEVEL
//	// ========================================================
//	private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
//
//		if (req.getPaymentLevel() == null || req.getPaymentTarget() == null)
//			return;
//
//		String level = req.getPaymentLevel().toUpperCase();
//		String status = getStatus(record);
//
//		switch (level) {
//		case "PACKAGE":
//			updatePackageStatus(record, req.getPaymentTarget().getPackageIds(), status);
//			break;
//		case "PROGRAM":
//			updateProgramStatus(record, req.getPaymentTarget().getProgramIds(), status);
//			break;
//		case "THERAPY":
//			updateTherapyStatus(record, req.getPaymentTarget().getTherapyIds(), status);
//			break;
//		case "EXERCISE":
//			updateExerciseStatus(record, req.getPaymentTarget().getExerciseIds(), status);
//			break;
//		case "SESSION":
//			paySessions(record, req.getPaymentTarget().getSessionIds());
//			break;
//		}
//	}
//
//	// ========================================================
//	// DISTRIBUTE PAYMENT TO SESSIONS
//	// ========================================================
//	private void distributePaymentToSessions(PaymentRecord record) {
//
//		if (record.getTherapyWithSessions() == null)
//			return;
//
//		double remaining = record.getTotalPaid();
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (therapy.getExercises() == null)
//						continue;
//					for (var ex : therapy.getExercises()) {
//						if (ex.getSessions() == null)
//							continue;
//						double price = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;
//						for (var s : ex.getSessions()) {
//							if (remaining >= price) {
//								// ✅ Full session price available — mark Paid
//								s.setPaymentStatus("Paid");
//								remaining -= price;
//							} else {
//								// ✅ Not enough — mark Unpaid (no Partial at session level)
//								s.setPaymentStatus("Unpaid");
//							}
//						}
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// PACKAGE STATUS UPDATE
//	// ========================================================
//	private void updatePackageStatus(PaymentRecord record, List<String> ids, String status) {
//
//		if (ids == null)
//			return;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (ids.contains(pkg.getPackageId())) {
//				pkg.setPaymentStatus(status);
//				for (var prog : pkg.getPrograms()) {
//					prog.setPaymentStatus(status);
//					for (var therapy : prog.getTherapyData()) {
//						therapy.setPaymentStatus(status);
//						for (var ex : therapy.getExercises()) {
//							ex.setPaymentStatus(status);
//						}
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// PROGRAM STATUS UPDATE
//	// ========================================================
//	private void updateProgramStatus(PaymentRecord record, List<String> ids, String status) {
//
//		if (ids == null)
//			return;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (ids.contains(prog.getProgramId())) {
//					prog.setPaymentStatus(status);
//					for (var therapy : prog.getTherapyData()) {
//						therapy.setPaymentStatus(status);
//						for (var ex : therapy.getExercises()) {
//							ex.setPaymentStatus(status);
//						}
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// THERAPY STATUS UPDATE
//	// ========================================================
//	private void updateTherapyStatus(PaymentRecord record, List<String> ids, String status) {
//
//		if (ids == null)
//			return;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (ids.contains(therapy.getTherapyId())) {
//						therapy.setPaymentStatus(status);
//						for (var ex : therapy.getExercises()) {
//							ex.setPaymentStatus(status);
//						}
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// EXERCISE STATUS UPDATE
//	// ========================================================
//	private void updateExerciseStatus(PaymentRecord record, List<String> ids, String status) {
//
//		if (ids == null)
//			return;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (therapy.getExercises() == null)
//						continue;
//					for (var ex : therapy.getExercises()) {
//						if (ids.contains(ex.getExerciseId())) {
//							ex.setPaymentStatus(status);
//						}
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// SESSION PAY
//	// ========================================================
//	private void paySessions(PaymentRecord record, List<String> ids) {
//
//		if (ids == null)
//			return;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (therapy.getExercises() == null)
//						continue;
//					for (var ex : therapy.getExercises()) {
//						if (ex.getSessions() == null)
//							continue;
//						for (var s : ex.getSessions()) {
//							if (ids.contains(s.getSessionId())) {
//								s.setPaymentStatus("Paid");
//							}
//						}
//						boolean allPaid = ex.getSessions().stream()
//								.allMatch(x -> "Paid".equalsIgnoreCase(x.getPaymentStatus()));
//						ex.setPaymentStatus(allPaid ? "Paid" : "Paid");
//					}
//				}
//			}
//		}
//	}
//
//	// ========================================================
//	// STATUS PROPAGATION
//	// ========================================================
//private void updateStatuses(PaymentRecord record) {
//
//    if (record.getTherapyWithSessions() == null) return;
//
//    for (var pkg : record.getTherapyWithSessions()) {
//        if (pkg.getPrograms() == null) continue;
//        for (var prog : pkg.getPrograms()) {
//            if (prog.getTherapyData() == null) continue;
//            for (var therapy : prog.getTherapyData()) {
//                if (therapy.getExercises() == null) continue;
//                for (var ex : therapy.getExercises()) {
//
//                    if (ex.getSessions() == null || ex.getSessions().isEmpty()) {
//                        ex.setPaymentStatus("Unpaid");
//                        continue;
//                    }
//
//                    boolean allPaid = ex.getSessions().stream()
//                            .allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));
//
//                    // ✅ Only Paid or Unpaid — no Partial
//                    ex.setPaymentStatus(allPaid ? "Paid" : "Unpaid");
//                }
//
//                // ✅ Propagate to therapy — only Paid or Unpaid
//                boolean allTherapyPaid = therapy.getExercises().stream()
//                        .allMatch(e -> "Paid".equalsIgnoreCase(e.getPaymentStatus()));
//                therapy.setPaymentStatus(allTherapyPaid ? "Paid" : "Unpaid");
//            }
//
//            // ✅ Propagate to program — only Paid or Unpaid
//            boolean allProgPaid = prog.getTherapyData().stream()
//                    .allMatch(t -> "Paid".equalsIgnoreCase(t.getPaymentStatus()));
//            prog.setPaymentStatus(allProgPaid ? "Paid" : "Unpaid");
//        }
//
//        // ✅ Propagate to package — only Paid or Unpaid
//        boolean allPkgPaid = pkg.getPrograms().stream()
//                .allMatch(p -> "Paid".equalsIgnoreCase(p.getPaymentStatus()));
//        pkg.setPaymentStatus(allPkgPaid ? "Paid" : "Unpaid");
//    }
//}
//	// ========================================================
//	// CALCULATE OVERALL STATUS
//	// ========================================================
//	private String calculateOverallStatus(PaymentRecord record) {
//
//		if (record.getTherapyWithSessions() == null)
//			return "Pending";
//
//		boolean allCompleted = true;
//		boolean anyCompleted = false;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (therapy.getExercises() == null)
//						continue;
//					for (var ex : therapy.getExercises()) {
//						if (ex.getSessions() == null)
//							continue;
//						for (var s : ex.getSessions()) {
//							if ("Completed".equalsIgnoreCase(s.getStatus())) {
//								anyCompleted = true;
//							} else {
//								allCompleted = false;
//							}
//						}
//					}
//				}
//			}
//		}
//
//		if (allCompleted && anyCompleted)
//			return "Completed";
//		if (anyCompleted)
//			return "Active";
//		return "Pending";
//	}
//
//	// ========================================================
//	// UPDATE BOOKING STATUS
//	// ========================================================
//	private void updateBookingStatus(PaymentRecord record) {
//
//		if (record.getBookingId() == null || record.getBookingId().trim().isEmpty())
//			return;
//
//		try {
//			BookingResponse request = new BookingResponse();
//			request.setBookingId(record.getBookingId().trim());
//
//			if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {
//				request.setStatus("completed");
//			} else if ("Active".equalsIgnoreCase(record.getOverallStatus())) {
//				request.setStatus("in-progress");
//			} else {
//				request.setStatus("pending");
//			}
//
//			clinicAdminFeign.updateAppointment(request);
//
//			System.out.println("Booking status updated => " + request.getBookingId() + " | " + request.getStatus());
//
//		} catch (Exception e) {
//			System.out.println("Booking status update failed");
//			e.printStackTrace();
//		}
//	}
//
//	// ========================================================
//	// MAP TO RESPONSE
//	// ========================================================
//	private PaymentRecordResponse mapToResponse(PaymentRecord record) {
//
//		PaymentRecordResponse res = new PaymentRecordResponse();
//
//		res.setId(record.getId());
//		res.setClinicId(record.getClinicId());
//		res.setBranchId(record.getBranchId());
//		res.setBookingId(record.getBookingId());
//		res.setPatientId(record.getPatientId());
//		res.setDoctorId(record.getDoctorId());
//		res.setDoctorName(record.getDoctorName());
//		res.setTherapistId(record.getTherapistId());
//		res.setTherapistName(record.getTherapistName());
//		res.setTherapistRecordId(record.getTherapistRecordId());
//		res.setServiceType(record.getServiceType());
//		res.setOverallStatus(record.getOverallStatus());
//		res.setTotalAmount(record.getTotalAmount());
//		res.setDiscountAmount(record.getDiscountAmount());
//		res.setFinalAmount(record.getFinalAmount());
//		res.setTotalPaid(record.getTotalPaid());
//		res.setBalanceAmount(record.getBalanceAmount());
//		res.setPaymentStatus(record.getPaymentStatus());
//		res.setSessionStartDate(record.getSessionStartDate());
//		res.setTotalSessionCount(record.getTotalSessionCount());
//		res.setNoOfSessionCompletedCount(record.getNoOfSessionCompletedCount());
//		res.setNoOfSessionCompletedStatus(record.isNoOfSessionCompletedStatus());
//		res.setSessionTableCreatedStatus(record.isSessionTableCreatedStatus());
//		res.setPaymentHistory(record.getPaymentHistory());
//
//		String serviceType = record.getServiceType() != null ? record.getServiceType().toLowerCase() : "package";
//
//		switch (serviceType) {
//		case "package":
//			res.setTherapyWithSessions(mapPackages(record));
//			break;
//		case "program":
//			res.setTherapyWithSessions(mapPrograms(record));
//			break;
//		case "therapy":
//			res.setTherapyWithSessions(mapTherapies(record));
//			break;
//		case "exercise":
//			res.setTherapyWithSessions(mapExercises(record));
//			break;
//		default:
//			res.setTherapyWithSessions(record.getTherapyWithSessions());
//		}
//
//		return res;
//	}
//
//	// ========================================================
//	// PACKAGE MAPPER
//	// ========================================================
//	private List<PackageResponse> mapPackages(PaymentRecord record) {
//
//		List<PackageResponse> result = new ArrayList<>();
//		if (record.getTherapyWithSessions() == null)
//			return result;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			PackageResponse p = new PackageResponse();
//			p.setPackageId(pkg.getPackageId());
//			p.setPackageName(pkg.getPackageName());
//			p.setTotalPackagePrice(pkg.getTotalPackagePrice());
//			p.setPaymentStatus(pkg.getPaymentStatus());
//			p.setPrograms(mapProgramList(pkg.getPrograms()));
//			result.add(p);
//		}
//		return result;
//	}
//
//	// ========================================================
//	// PROGRAM MAPPER
//	// ========================================================
//	private List<ProgramResponse> mapPrograms(PaymentRecord record) {
//
//	    List<ProgramResponse> result = new ArrayList<>();
//
//	    if (record.getTherapyWithSessions() == null)
//	        return result;
//
//	    for (var pkg : record.getTherapyWithSessions()) {
//
//	        if (pkg.getPrograms() != null) {
//	            result.addAll(mapProgramList(pkg.getPrograms()));
//	        }
//	    }
//
//	    return result;
//	}
//
//	// ========================================================
//	// THERAPY MAPPER
//	// ========================================================
//	private List<TherapyResponse> mapTherapies(PaymentRecord record) {
//
//	    List<TherapyResponse> result = new ArrayList<>();
//
//	    if (record.getTherapyWithSessions() == null)
//	        return result;
//
//	    for (var pkg : record.getTherapyWithSessions()) {
//
//	        if (pkg.getPrograms() == null) continue;
//
//	        for (var prog : pkg.getPrograms()) {
//
//	            if (prog.getTherapyData() != null) {
//	                result.addAll(mapTherapyList(prog.getTherapyData()));
//	            }
//	        }
//	    }
//
//	    return result;
//	}
//
//	// ========================================================
//	// EXERCISE MAPPER
//	// ========================================================
//	private List<ExerciseResponse> mapExercises(PaymentRecord record) {
//
//	    List<ExerciseResponse> result = new ArrayList<>();
//
//	    if (record.getTherapyWithSessions() == null)
//	        return result;
//
//	    for (var pkg : record.getTherapyWithSessions()) {
//
//	        if (pkg.getPrograms() == null) continue;
//
//	        for (var prog : pkg.getPrograms()) {
//
//	            if (prog.getTherapyData() == null) continue;
//
//	            for (var therapy : prog.getTherapyData()) {
//
//	                if (therapy.getExercises() != null) {
//	                    result.addAll(mapExerciseList(therapy.getExercises()));
//	                }
//	            }
//	        }
//	    }
//
//	    return result;
//	}
//
//	// ========================================================
//	// SHARED LIST MAPPERS
//	// ========================================================
//	private List<ProgramResponse> mapProgramList(List<Program> programs) {
//
//		List<ProgramResponse> result = new ArrayList<>();
//		if (programs == null)
//			return result;
//
//		for (var prog : programs) {
//			ProgramResponse p = new ProgramResponse();
//			p.setProgramId(prog.getProgramId());
//			p.setProgramName(prog.getProgramName());
//			p.setTotalProgramPrice(prog.getTotalProgramPrice());
//			p.setPaymentStatus(prog.getPaymentStatus());
//			p.setTherapyData(mapTherapyList(prog.getTherapyData()));
//			result.add(p);
//		}
//		return result;
//	}
//
//	private List<TherapyResponse> mapTherapyList(List<TherapyData> therapies) {
//
//		List<TherapyResponse> result = new ArrayList<>();
//		if (therapies == null)
//			return result;
//
//		for (var t : therapies) {
//			TherapyResponse tr = new TherapyResponse();
//			tr.setTherapyId(t.getTherapyId());
//			tr.setTherapyName(t.getTherapyName());
//			tr.setTotalTherapyPrice(t.getTotalTherapyPrice());
//			tr.setPaymentStatus(t.getPaymentStatus());
//			tr.setExercises(mapExerciseList(t.getExercises()));
//			result.add(tr);
//		}
//		return result;
//	}
//
//	private List<ExerciseResponse> mapExerciseList(List<TherapyExercise> exercises) {
//
//	    List<ExerciseResponse> result = new ArrayList<>();
//	    if (exercises == null) return result;
//
//	    for (TherapyExercise ex : exercises) {
//
//	        ExerciseResponse er = new ExerciseResponse();
//
//	        // ✅ Basic Info
//	        er.setExerciseId(ex.getExerciseId());
//	        er.setExerciseName(ex.getExerciseName());
//
//	        // ✅ Pricing
//	        er.setPricePerSession(ex.getPricePerSession());
//	        er.setNoOfSessions(ex.getNoOfSessions());
//	        er.setDiscountPercentage(ex.getDiscountPercentage());
//	        er.setDiscountAmount(ex.getDiscountAmount());
//	        er.setGst(ex.getGst());
//	        er.setOtherTax(ex.getOtherTax());
//	        er.setTotalExercisePrice(ex.getTotalExercisePrice());
//	        er.setTotalPrice(ex.getTotalPrice());
//
//	        // ✅ Payment
//	        er.setPaymentStatus(ex.getPaymentStatus());
//
//	        // ✅ Exercise Details
//	        er.setRepetitions(ex.getRepetitions());
//	        er.setFrequency(ex.getFrequency());
//	        er.setSets(ex.getSets());
//	        er.setYoutubeUrl(ex.getYoutubeUrl());
//	        er.setNotes(ex.getNotes());
//
//	        // ✅ New Fields
//	        er.setTechnique(ex.getTechnique());
//	        er.setMachine(ex.getMachine());
//	        er.setIntensity(ex.getIntensity());
//	        er.setAssistanceLevel(ex.getAssistanceLevel());
//	        er.setType(ex.getType());
//	        er.setArea(ex.getArea());
//	        er.setMetric(ex.getMetric());
//	        er.setValue(ex.getValue());
//	        er.setUnit(ex.getUnit());
//	        er.setBodyPart(ex.getBodyPart());
//
//	        // ✅ Activity Fields
//	        er.setActivityType(ex.getActivityType());
//	        er.setActivityDuration(ex.getActivityDuration());
//
//	        // ✅ Sessions (direct mapping or map separately if different DTO)
//	        er.setSessions(ex.getSessions());
//
//	        result.add(er);
//	    }
//
//	    return result;
//	}
//
//	// ========================================================
//	// UTILS
//	// ========================================================
//	private String getStatus(PaymentRecord r) {
//		if (r.getTotalPaid() <= 0)
//			return "Unpaid";
//		if (r.getTotalPaid() < r.getFinalAmount())
//			return "Partial";
//		if (Double.compare(r.getTotalPaid(), r.getFinalAmount()) == 0)
//			return "Paid";
//		return "Overpaid";
//	}
//
//	private PaymentHistory buildHistory(PaymentRequest req) {
//		return new PaymentHistory(req.getAmount(), req.getPaymentMode(), req.getPaymentType(), req.getPaymentDate(),
//				req.getPaymentLevel(), req.getDiscountAmount(), req.getDiscountIssuedBy());
//	}
//
//	private double calculateTotal(List<TherapyWithSessions> data) {
//
//	    double total = 0;
//
//	    for (var pkg : data) {
//	        double pkgTotal = 0;
//	        for (var prog : pkg.getPrograms()) {
//	            double progTotal = 0;
//	            for (var therapy : prog.getTherapyData()) {
//	                double therapyTotal = 0;
//	                for (var ex : therapy.getExercises()) {
//
//	                    // ✅ totalSessions = days × timesPerDay
//	                    int totalSessions = ex.getNoOfSessions()
//	                            * parseTimesPerDay(ex.getFrequency());
//
//	                    double exTotal = ex.getPricePerSession() * totalSessions;
//	                    ex.setTotalExercisePrice(exTotal);
//	                    therapyTotal += exTotal;
//	                }
//	                therapy.setTotalTherapyPrice(therapyTotal);
//	                progTotal += therapyTotal;
//	            }
//	            prog.setTotalProgramPrice(progTotal);
//	            pkgTotal += progTotal;
//	        }
//	        pkg.setTotalPackagePrice(pkgTotal);
//	        total += pkgTotal;
//	    }
//
//	    return total;
//	}
//	
//	private int countTotalSessions(PaymentRecord record) {
//
//	    int count = 0;
//
//	    if (record.getTherapyWithSessions() == null) return count;
//
//	    for (var pkg : record.getTherapyWithSessions()) {
//	        if (pkg.getPrograms() == null) continue;
//	        for (var prog : pkg.getPrograms()) {
//	            if (prog.getTherapyData() == null) continue;
//	            for (var therapy : prog.getTherapyData()) {
//	                if (therapy.getExercises() == null) continue;
//	                for (var ex : therapy.getExercises()) {
//	                    if (ex.getSessions() != null) {
//	                        count += ex.getSessions().size();
//	                    }
//	                }
//	            }
//	        }
//	    }
//
//	    return count;
//	}
//	private boolean createSessions(List<TherapyWithSessions> data, String startDate) {
//
//	    boolean created = false;
//
//	    for (var pkg : data) {
//	        for (var prog : pkg.getPrograms()) {
//	            for (var therapy : prog.getTherapyData()) {
//	                for (var ex : therapy.getExercises()) {
//
//	                    List<Session> sessions = new ArrayList<>();
//	                    LocalDate currentDate = LocalDate.parse(startDate);
//	                    int noOfSessions = ex.getNoOfSessions();
//	                    String freqType = parseFrequencyType(ex.getFrequency());
//	                    int sessionNo = 1;
//
//	                    if (freqType.equals("day")) {
//
//	                        // ✅ DAY — 1 session per day
//	                        for (int i = 1; i <= noOfSessions; i++) {
//	                            sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
//	                            sessionNo++;
//	                            currentDate = currentDate.plusDays(1);
//	                        }
//
//	                    } else if (freqType.equals("week")) {
//
//	                        // ✅ WEEK — noOfSessions sessions spread within 1 week (7 days)
//	                        // Gap between sessions = 7 / noOfSessions days
//	                        int gapDays = noOfSessions > 0 ? 7 / noOfSessions : 1;
//
//	                        for (int i = 1; i <= noOfSessions; i++) {
//	                            sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
//	                            sessionNo++;
//	                            currentDate = currentDate.plusDays(gapDays);
//	                        }
//
//	                    } else if (freqType.equals("month")) {
//
//	                        // ✅ MONTH — noOfSessions sessions spread within 1 month (30 days)
//	                        // Gap between sessions = 30 / noOfSessions days
//	                        int gapDays = noOfSessions > 0 ? 30 / noOfSessions : 1;
//
//	                        for (int i = 1; i <= noOfSessions; i++) {
//	                            sessions.add(buildSession(ex.getExerciseId(), sessionNo, currentDate));
//	                            sessionNo++;
//	                            currentDate = currentDate.plusDays(gapDays);
//	                        }
//	                    }
//
//	                    if (!sessions.isEmpty()) created = true;
//	                    ex.setSessions(sessions);
//	                }
//	            }
//	        }
//	    }
//
//	    return created;
//	}	
//	private String parseFrequencyType(String frequency) {
//
//	    if (frequency == null || frequency.trim().isEmpty()) {
//	        return "day"; // default
//	    }
//
//	    String lower = frequency.toLowerCase().trim();
//
//	    if (lower.contains("month")) return "month";
//	    if (lower.contains("week"))  return "week";
//	    return "day"; // default
//	}
//	
////	private boolean createSessions(List<TherapyWithSessions> data, String startDate) {
////
////	    boolean created = false;
////
////	    for (var pkg : data) {
////	        for (var prog : pkg.getPrograms()) {
////	            for (var therapy : prog.getTherapyData()) {
////	                for (var ex : therapy.getExercises()) {
////
////	                    List<Session> sessions = new ArrayList<>();
////
////	                    int timesPerDay = parseTimesPerDay(ex.getFrequency());
////	                    int numberOfDays = ex.getNoOfSessions(); // ✅ noOfSessions = number of DAYS
////
////	                    LocalDate currentDate = LocalDate.parse(startDate);
////	                    int sessionNo = 1;
////
////	                    for (int day = 1; day <= numberOfDays; day++) {
////
////	                        for (int time = 1; time <= timesPerDay; time++) {
////
////	                            String uniqueSessionId = ex.getExerciseId()
////	                                    + "_" + sessionNo
////	                                    + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
////
////	                            sessions.add(new Session(
////	                                    uniqueSessionId,
////	                                    sessionNo,
////	                                    currentDate.toString(),
////	                                    "Pending",
////	                                    "Unpaid"
////	                            ));
////
////	                            sessionNo++;
////	                        }
////
////	                        // ✅ Move to next day after all sessions of current day
////	                        currentDate = currentDate.plusDays(1);
////	                    }
////
////	                    if (!sessions.isEmpty()) created = true;
////	                    ex.setSessions(sessions);
////	                }
////	            }
////	        }
////	    }
////
////	    return created;
////	}
////
////
////	
//	
//	//	private boolean createSessions(List<TherapyWithSessions> data, String startDate) {
////
////	    boolean created = false;
////
////	    for (var pkg : data) {
////	        for (var prog : pkg.getPrograms()) {
////	            for (var therapy : prog.getTherapyData()) {
////	                for (var ex : therapy.getExercises()) {
////
////	                    List<Session> sessions = new ArrayList<>();
////
////	                    int timesPerDay = parseTimesPerDay(ex.getFrequency());
////	                    LocalDate currentDate = LocalDate.parse(startDate);
////	                    int sessionCount = 0;
////
////	                    for (int i = 1; i <= ex.getNoOfSessions(); i++) {
////
////	                        // ✅ Unique sessionId = exerciseId + "_" + sessionNo + "_" + UUID short
////	                        String uniqueSessionId = ex.getExerciseId()
////	                                + "_" + i
////	                                + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
////
////	                        sessions.add(new Session(
////	                                uniqueSessionId,
////	                                i,
////	                                currentDate.toString(),
////	                                "Pending",
////	                                "Unpaid"
////	                        ));
////
////	                        sessionCount++;
////
////	                        if (sessionCount % timesPerDay == 0) {
////	                            currentDate = currentDate.plusDays(1);
////	                        }
////	                    }
////
////	                    if (!sessions.isEmpty()) created = true;
////	                    ex.setSessions(sessions);
////	                }
////	            }
////	        }
////	    }
////
////	    return created;
////	}
////	
////	
////	
//	// ========================================================
////  PARSE FREQUENCY → TIMES PER DAY
////========================================================
//	private int parseTimesPerDay(String frequency) {
//
//		if (frequency == null || frequency.trim().isEmpty()) {
//			return 1; // default 1 session per day
//		}
//
//// Handles: "2 times/day", "3 times/day", "1 time/day", "Once a day", etc.
//		String lower = frequency.toLowerCase().trim();
//
//		try {
//// ✅ Match pattern like "2 times/day" or "3 times/day"
//			if (lower.contains("times/day") || lower.contains("time/day")) {
//				String[] parts = lower.split(" ");
//				return Integer.parseInt(parts[0]);
//			}
//
//// ✅ Match "once a day"
//			if (lower.contains("once")) {
//				return 1;
//			}
//
//// ✅ Match "twice a day"
//			if (lower.contains("twice")) {
//				return 2;
//			}
//
//// ✅ Match "thrice a day"
//			if (lower.contains("thrice")) {
//				return 3;
//			}
//
//		} catch (Exception e) {
//// fallback
//		}
//
//		return 1; // default
//	}
//
//	private int countCompleted(PaymentRecord record) {
//
//		int count = 0;
//
//		if (record.getTherapyWithSessions() == null)
//			return count;
//
//		for (var pkg : record.getTherapyWithSessions()) {
//			if (pkg.getPrograms() == null)
//				continue;
//			for (var prog : pkg.getPrograms()) {
//				if (prog.getTherapyData() == null)
//					continue;
//				for (var therapy : prog.getTherapyData()) {
//					if (therapy.getExercises() == null)
//						continue;
//					for (var ex : therapy.getExercises()) {
//						if (ex.getSessions() == null)
//							continue;
//						for (var s : ex.getSessions()) {
//							if ("Completed".equalsIgnoreCase(s.getStatus())) {
//								count++;
//							}
//						}
//					}
//				}
//			}
//		}
//
//		return count;
//	}
//
//	@Override
//	public Response getExerciseSessionsWithRecords(String clinicId, String branchId, String bookingId,
//	        String patientId, String therapistRecordId) {
//
//	    Response response = new Response();
//
//	    try {
//
//	        PaymentRecord record = repo
//	                .findByClinicIdAndBranchIdAndBookingIdAndPatientIdAndTherapistRecordId(
//	                        clinicId, branchId, bookingId, patientId, therapistRecordId)
//	                .orElseThrow(() -> new RuntimeException("Payment record not found"));
//
//	        List<Object> exerciseList = new ArrayList<>();
//
//	        // ✅ ObjectMapper for safe conversion
//	        ObjectMapper mapper = new ObjectMapper();
//
//	        for (TherapyWithSessions pkg : record.getTherapyWithSessions()) {
//	            if (pkg.getPrograms() == null)
//	                continue;
//
//	            for (Program program : pkg.getPrograms()) {
//	                if (program.getTherapyData() == null)
//	                    continue;
//
//	                for (TherapyData therapy : program.getTherapyData()) {
//	                    if (therapy.getExercises() == null)
//	                        continue;
//
//	                    for (TherapyExercise exercise : therapy.getExercises()) {
//
//	                        List<Object> sessionList = new ArrayList<>();
//
//	                        for (Session session : exercise.getSessions()) {
//
//	                            Map<String, Object> map = new LinkedHashMap<>();
//
//	                            map.put("sessionId", session.getSessionId());
//	                            map.put("sessionNo", session.getSessionNo());
//	                            map.put("date", session.getDate());
//	                            map.put("paymentStatus", session.getPaymentStatus());
//
//	                            try {
//
//	                                // ✅ KEEP original Feign type
//	                                ResponseEntity<ResponseStructure<TherapistRecordDTO>> tr =
//	                                        clinicAdminFeign.getRecordBySession(
//	                                                clinicId, branchId, bookingId, patientId,
//	                                                session.getSessionId());
//
//	                                if (tr != null && tr.getBody() != null && tr.getBody().getData() != null) {
//
//	                                    // 🔥 FIX: force proper conversion (handles LinkedHashMap issue)
//	                                    TherapistRecordDTO dto = mapper.convertValue(
//	                                            tr.getBody().getData(),
//	                                            TherapistRecordDTO.class
//	                                    );
//
//	                                    map.put("status", "Completed");
//	                                    map.put("therapistRecord", dto);
//
//	                                } else {
//	                                    map.put("status", session.getStatus());
//	                                    map.put("therapistRecord", null);
//	                                }
//
//	                            } catch (Exception e) {
//	                                map.put("status", session.getStatus());
//	                                map.put("therapistRecord", null);
//	                            }
//
//	                            sessionList.add(map);
//	                        }
//
//	                        Map<String, Object> exerciseData = new LinkedHashMap<>();
//	                        exerciseData.put("exerciseId", exercise.getExerciseId());
//	                        exerciseData.put("exerciseName", exercise.getExerciseName());
//	                        exerciseData.put("sessions", sessionList);
//
//	                        exerciseList.add(exerciseData);
//	                    }
//	                }
//	            }
//	        }
//
//	        response.setSuccess(true);
//	        response.setStatus(200);
//	        response.setMessage("All exercises fetched successfully");
//	        response.setData(exerciseList);
//
//	    } catch (Exception e) {
//	        response.setSuccess(false);
//	        response.setStatus(500);
//	        response.setMessage(e.getMessage());
//	    }
//
//	    return response;
//	}
//	@Override
//	public List<PaymentRecordResponse> findByClinicIdAndBranchId(
//	        String clinicId,
//	        String branchId) {
//
//	    List<PaymentRecord> records =
//	            repo.findByClinicIdAndBranchId(
//	                    clinicId,
//	                    branchId);
//
//	    if (records == null || records.isEmpty()) {
//
//	        throw new RuntimeException(
//	                "No payment records found");
//	    }
//
//	    return records.stream()
//	            .map(this::mapToResponse)
//	            .toList();
//	}
//}
//
//	
//
//
////package physiotherapydoctor.serviceImpl;
////
////import java.util.ArrayList;
////import java.util.HashMap;
////import java.util.LinkedHashMap;
////import java.util.List;
////import java.util.Map;
////
////import org.springframework.beans.factory.annotation.Autowired;
////import org.springframework.http.ResponseEntity;
////import org.springframework.stereotype.Service;
////
////import lombok.RequiredArgsConstructor;
////import physiotherapydoctor.dto.BookingResponse;
////import physiotherapydoctor.dto.PaymentHistory;
////import physiotherapydoctor.dto.PaymentRequest;
////import physiotherapydoctor.dto.Program;
////import physiotherapydoctor.dto.Response;
////import physiotherapydoctor.dto.ResponseStructure;
////import physiotherapydoctor.dto.Session;
////import physiotherapydoctor.dto.TherapistRecordDTO;
////import physiotherapydoctor.dto.TherapyData;
////import physiotherapydoctor.dto.TherapyExercise;
////import physiotherapydoctor.dto.TherapyWithSessions;
////import physiotherapydoctor.entity.PaymentRecord;
////import physiotherapydoctor.feign.BookingFeign;
////import physiotherapydoctor.feign.ClinicAdminFeign;
////import physiotherapydoctor.repository.PaymentRepository;
////import physiotherapydoctor.service.PaymentService;
////@Service
////@RequiredArgsConstructor
////public class PaymentServiceImpl implements PaymentService {
////
////    private final PaymentRepository repo;
////    
////    @Autowired
////	private BookingFeign bookingFeign;
////    
////    @Autowired
////    private PaymentRepository paymentRepository;
////
////    @Autowired
////    private ClinicAdminFeign clinicAdminFeign;
////    // ================= CREATE =================
////    @Override
////    public PaymentRecord createPayment(PaymentRequest req) {
////
////        if (repo.findByBookingId(req.getBookingId()).isPresent()) {
////            throw new RuntimeException("Already exists, use update");
////        }
////
////        if (req.getAmount() == null || req.getAmount() <= 0) {
////            throw new RuntimeException("Amount must be greater than 0");
////        }
////
////        if (req.getTherapyWithSessions() == null || req.getTherapyWithSessions().isEmpty()) {
////            throw new RuntimeException("therapyWithSessions is required");
////        }
////
////        PaymentRecord record = new PaymentRecord();
////
////        // ================= BASIC =================
////        record.setClinicId(req.getClinicId());
////        record.setBranchId(req.getBranchId());
////        record.setBookingId(req.getBookingId());
////        record.setPatientId(req.getPatientId());
////        record.setOverallStatus("Pending");
////
////        record.setDoctorId(req.getDoctorId());
////        record.setDoctorName(req.getDoctorName());
////
////        record.setTherapistId(req.getTherapistId());
////        record.setTherapistName(req.getTherapistName());
////        record.setTherapistRecordId(req.getTherapistRecordId());
////
////        record.setServiceType(req.getServiceType());
////
////        // ================= SESSION =================
////        record.setSessionStartDate(req.getSessionStartDate());
////        record.setTotalSessionCount(req.getTotalSessionCount());
////
////        // ================= TOTAL =================
////        double total = calculateTotal(req.getTherapyWithSessions());
////        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
////
////        double finalAmount = total - discount;
////
////        record.setTotalAmount(total);
////        record.setDiscountAmount(discount);
////        record.setFinalAmount(finalAmount);
////
////        double amount = req.getAmount();
////
////        // ================= VALIDATIONS =================
////        if (amount > finalAmount) {
////            throw new RuntimeException("Amount exceeds final payable amount: " + finalAmount);
////        }
////
////        if ("FULL".equalsIgnoreCase(req.getPaymentType()) && amount != finalAmount) {
////            throw new RuntimeException("Full payment must be exactly: " + finalAmount);
////        }
////
////        // ================= PAYMENT =================
////        record.setTotalPaid(amount);
////        record.setBalanceAmount(finalAmount - amount);
////        record.setPaymentStatus(getStatus(record));
////
////        // ================= CREATE SESSIONS =================
////        boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
////        record.setSessionTableCreatedStatus(created);
////
////        // 🔥 FIX 1: SET DATA FIRST
////        record.setTherapyWithSessions(req.getTherapyWithSessions());
////
////        // 🔥 FIX 2: THEN DISTRIBUTE
////        distributePaymentToSessions(record);
////
////        // ================= HISTORY =================
////        record.setPaymentHistory(new ArrayList<>());
////        record.getPaymentHistory().add(buildHistory(req));
////
////        // ================= APPLY LEVEL =================
////        if (req.getPaymentTarget() != null) {
////            applyPaymentLevel(record, req);
////        }
////
////        // ================= STATUS PROPAGATION =================
////        updateStatuses(record);
////
////        return repo.save(record);
////    }    //    @Override
//////    public PaymentRecord createPayment(PaymentRequest req) {
//////
//////        if (repo.findByBookingId(req.getBookingId()).isPresent()) {
//////            throw new RuntimeException("Already exists, use update");
//////        }
//////
//////        PaymentRecord record = new PaymentRecord();
//////
//////        // BASIC
//////        record.setClinicId(req.getClinicId());
//////        record.setBranchId(req.getBranchId());
//////        record.setBookingId(req.getBookingId());
//////        record.setPatientId(req.getPatientId());
//////
//////        record.setDoctorId(req.getDoctorId());
//////        record.setDoctorName(req.getDoctorName());
//////
//////        record.setTherapistId(req.getTherapistId());
//////        record.setTherapistName(req.getTherapistName());
//////        record.setTherapistRecordId(req.getTherapistRecordId());
//////
//////        record.setServiceType(req.getServiceType());
//////
//////        // SESSION
//////        record.setSessionStartDate(req.getSessionStartDate());
//////        record.setTotalSessionCount(req.getTotalSessionCount());
//////
//////        // TOTAL
//////        double total = calculateTotal(req.getTherapyWithSessions());
//////        double discount = req.getDiscountAmount() != null ? req.getDiscountAmount() : 0;
//////
//////        record.setTotalAmount(total);
//////        record.setDiscountAmount(discount);
//////        record.setFinalAmount(total - discount);
//////
//////        // PAYMENT
//////        record.setTotalPaid(req.getAmount());
//////        record.setBalanceAmount(record.getFinalAmount() - req.getAmount());
//////        record.setPaymentStatus(getStatus(record));
//////
//////        // CREATE SESSIONS
//////        boolean created = createSessions(req.getTherapyWithSessions(), req.getSessionStartDate());
//////        record.setSessionTableCreatedStatus(created);
//////
//////        record.setTherapyWithSessions(req.getTherapyWithSessions());
//////
//////        // HISTORY
//////        record.setPaymentHistory(new ArrayList<>());
//////        record.getPaymentHistory().add(buildHistory(req));
//////
//////        // STATUS UPDATE
//////        updateStatuses(record);
//////
//////        return repo.save(record);
//////    }
////
////    // ================= UPDATE =================
////    @Override
////    public PaymentRecord updatePayment(PaymentRequest req) {
////
////        PaymentRecord record = repo.findByBookingId(req.getBookingId())
////                .orElseThrow(() -> new RuntimeException("Payment not found"));
////
////        // ❌ Prevent sending full structure in update
////        if (req.getTherapyWithSessions() != null) {
////            throw new RuntimeException("Do not send full data in update");
////        }
////
////        if (req.getAmount() == null || req.getAmount() <= 0) {
////            throw new RuntimeException("Amount must be greater than 0");
////        }
////
////        if (req.getPaymentTarget() == null) {
////            throw new RuntimeException("paymentTarget is required");
////        }
////
////        double currentPaid = record.getTotalPaid();
////        double finalAmount = record.getFinalAmount();
////        double remaining = finalAmount - currentPaid;
////
////        // ================= 🔥 FULL PAYMENT VALIDATION =================
////        if ("FULL".equalsIgnoreCase(req.getPaymentType())) {
////
////            if (req.getAmount() != remaining) {
////                throw new RuntimeException(
////                        "Full payment must be exactly remaining amount: " + remaining
////                );
////            }
////        }
////
////        // ================= 🔥 OVERPAYMENT PREVENTION =================
////        double newPaid = currentPaid + req.getAmount();
////
////        if (newPaid > finalAmount) {
////            throw new RuntimeException(
////                    "Payment exceeds final amount. Remaining payable: " + remaining
////            );
////        }
////
////        // ================= UPDATE AMOUNT =================
////        record.setTotalPaid(newPaid);
////        record.setBalanceAmount(finalAmount - newPaid);
////        record.setPaymentStatus(getStatus(record));
////     // 🔥 NEW LINE
////        distributePaymentToSessions(record);
////        // ================= APPLY PAYMENT LEVEL =================
////        applyPaymentLevel(record, req);
////
////        // ================= SESSION COMPLETION =================
////        int completed = countCompleted(record);
////
////        record.setNoOfSessionCompletedCount(completed);
////        record.setNoOfSessionCompletedStatus(
////                completed >= record.getTotalSessionCount()
////        );
////
////        // ================= HISTORY =================
////        record.getPaymentHistory().add(buildHistory(req));
////
////        // ================= STATUS PROPAGATION =================
////        updateStatuses(record);
////
////        return repo.save(record);
////    }
////    
//////    @Override
//////    public PaymentRecord updatePayment(PaymentRequest req) {
//////
//////        PaymentRecord record = repo.findByBookingId(req.getBookingId())
//////                .orElseThrow(() -> new RuntimeException("Not found"));
//////
//////        if (req.getTherapyWithSessions() != null) {
//////            throw new RuntimeException("Do not send full data in update");
//////        }
//////
//////        double newPaid = record.getTotalPaid() + req.getAmount();
//////
//////        record.setTotalPaid(newPaid);
//////        record.setBalanceAmount(record.getFinalAmount() - newPaid);
//////        record.setPaymentStatus(getStatus(record));
//////
//////        applyPaymentLevel(record, req);
//////
//////        int completed = countCompleted(record);
//////        record.setNoOfSessionCompletedCount(completed);
//////        record.setNoOfSessionCompletedStatus(
//////                completed >= record.getTotalSessionCount()
//////        );
//////
//////        record.getPaymentHistory().add(buildHistory(req));
//////
//////        updateStatuses(record);
//////
//////        return repo.save(record);
//////    }
////
////    // ================= APPLY LEVEL =================
////    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
////
////        if (req.getPaymentLevel() == null || req.getPaymentTarget() == null) return;
////
////        String level = req.getPaymentLevel().toUpperCase();
////        String status = getStatus(record); // Unpaid / Partial / Paid
////
////        switch (level) {
////
////            case "PACKAGE":
////                updatePackageStatus(record, req.getPaymentTarget().getPackageIds(), status);
////                break;
////
////            case "PROGRAM":
////                updateProgramStatus(record, req.getPaymentTarget().getProgramIds(), status);
////                break;
////
////            case "THERAPY":
////                updateTherapyStatus(record, req.getPaymentTarget().getTherapyIds(), status);
////                break;
////
////            case "EXERCISE":
////                updateExerciseStatus(record, req.getPaymentTarget().getExerciseIds(), status);
////                break;
////
////            case "SESSION":
////                paySessions(record, req.getPaymentTarget().getSessionIds());
////                break;
////        }
////    }
////    private void distributePaymentToSessions(PaymentRecord record) {
////
////        if (record.getTherapyWithSessions() == null) return; // 🔥 FIX
////
////        double remaining = record.getTotalPaid();
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            if (pkg.getPrograms() == null) continue;
////
////            for (var prog : pkg.getPrograms()) {
////                if (prog.getTherapyData() == null) continue;
////
////                for (var therapy : prog.getTherapyData()) {
////                    if (therapy.getExercises() == null) continue;
////
////                    for (var ex : therapy.getExercises()) {
////
////                        if (ex.getSessions() == null) continue;
////
////                        double price = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;
////
////                        for (var s : ex.getSessions()) {
////
////                            if (remaining <= 0) {
////                                s.setPaymentStatus("Unpaid");
////                            } else if (remaining >= price) {
////                                s.setPaymentStatus("Paid");
////                                remaining -= price;
////                            } else {
////                                s.setPaymentStatus("Partial");
////                                remaining = 0;
////                            }
////                        }
////                    }
////                }
////            }
////        }
////    }    private static class SessionWrapper {
////        Session session;
////        double price;
////
////        SessionWrapper(Session s, double p) {
////            this.session = s;
////            this.price = p;
////        }
////    }
////    //    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
////    
////
//////
//////        if (req.getPaymentLevel() == null || req.getPaymentTarget() == null) return;
//////
//////        // 🔥 Only allow marking Paid when FULL payment is done
//////        boolean isFullPaid = Double.compare(
//////                record.getTotalPaid(),
//////                record.getFinalAmount()
//////        ) == 0;
//////
//////        if (!isFullPaid) {
//////            return; // ❌ DO NOTHING for partial payment
//////        }
//////
//////        String level = req.getPaymentLevel().toUpperCase();
//////
//////        switch (level) {
//////
//////            case "SESSION":
//////                paySessions(record, req.getPaymentTarget().getSessionIds());
//////                break;
//////
//////            case "EXERCISE":
//////                payExercises(record, req.getPaymentTarget().getExerciseIds());
//////                break;
//////
//////            case "THERAPY":
//////                payTherapies(record, req.getPaymentTarget().getTherapyIds());
//////                break;
//////
//////            case "PROGRAM":
//////                payPrograms(record, req.getPaymentTarget().getProgramIds());
//////                break;
//////
//////            case "PACKAGE":
//////                payPackages(record, req.getPaymentTarget().getPackageIds());
//////                break;
//////        }
//////    }
//////    private void applyPaymentLevel(PaymentRecord record, PaymentRequest req) {
//////
//////        if (req.getPaymentLevel() == null) return;
//////
//////        String level = req.getPaymentLevel().toUpperCase();
//////
//////        switch (level) {
//////
//////            case "SESSION":
//////                paySessions(record, req.getPaymentTarget().getSessionIds());
//////                break;
//////
//////            case "EXERCISE":
//////                payExercises(record, req.getPaymentTarget().getExerciseIds());
//////                break;
//////
//////            case "THERAPY":
//////                payTherapies(record, req.getPaymentTarget().getTherapyIds());
//////                break;
//////
//////            case "PROGRAM":
//////                payPrograms(record, req.getPaymentTarget().getProgramIds());
//////                break;
//////
//////            case "PACKAGE":
//////                payPackages(record, req.getPaymentTarget().getPackageIds());
//////                break;
//////
//////            default:
//////                throw new RuntimeException("Invalid payment level");
//////        }
//////    }
////
////    // ================= PACKAGE =================
////    private void updatePackageStatus(PaymentRecord record, List<String> ids, String status) {
////
////        if (ids == null) return;
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            if (ids.contains(pkg.getPackageId())) {
////
////                pkg.setPaymentStatus(status);
////
////                for (var prog : pkg.getPrograms()) {
////                    prog.setPaymentStatus(status);
////
////                    for (var therapy : prog.getTherapyData()) {
////                        therapy.setPaymentStatus(status);
////
////                        for (var ex : therapy.getExercises()) {
////                            ex.setPaymentStatus(status);
////                        }
////                    }
////                }
////            }
////        }
////    }
////    //    private void payPackages(PaymentRecord record, List<String> ids) {
//////
//////        for (var pkg : record.getTherapyWithSessions()) {
//////            if (ids.contains(pkg.getPackageId())) {
//////
//////                pkg.setPaymentStatus("Paid");
//////
//////                for (var prog : pkg.getPrograms()) {
//////                    prog.setPaymentStatus("Paid");
//////
//////                    for (var therapy : prog.getTherapyData()) {
//////                        therapy.setPaymentStatus("Paid");
//////
//////                        for (var ex : therapy.getExercises()) {
//////                            ex.setPaymentStatus("Paid");
//////
//////                            for (var s : ex.getSessions()) {
//////                                s.setPaymentStatus("Paid");
//////                            }
//////                        }
//////                    }
//////                }
//////            }
//////        }
//////    }
////
////    // ================= PROGRAM =================
////    private void updateProgramStatus(PaymentRecord record, List<String> ids, String status) {
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            for (var prog : pkg.getPrograms()) {
////
////                if (ids.contains(prog.getProgramId())) {
////
////                    prog.setPaymentStatus(status);
////
////                    for (var therapy : prog.getTherapyData()) {
////                        therapy.setPaymentStatus(status);
////
////                        for (var ex : therapy.getExercises()) {
////                            ex.setPaymentStatus(status);
////                        }
////                    }
////                }
////            }
////        }
////    }
////    // ================= THERAPY =================
////    private void updateTherapyStatus(PaymentRecord record, List<String> ids, String status) {
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            for (var prog : pkg.getPrograms()) {
////                for (var therapy : prog.getTherapyData()) {
////
////                    if (ids.contains(therapy.getTherapyId())) {
////
////                        therapy.setPaymentStatus(status);
////
////                        for (var ex : therapy.getExercises()) {
////                            ex.setPaymentStatus(status);
////                        }
////                    }
////                }
////            }
////        }
////    }
////    // ================= EXERCISE =================
////    private void updateExerciseStatus(PaymentRecord record, List<String> ids, String status) {
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            for (var prog : pkg.getPrograms()) {
////                for (var therapy : prog.getTherapyData()) {
////                    for (var ex : therapy.getExercises()) {
////
////                        if (ids.contains(ex.getExerciseId())) {
////                            ex.setPaymentStatus(status);
////                        }
////                    }
////                }
////            }
////        }
////    }
////
////    // ================= SESSION =================
////    private void paySessions(PaymentRecord record, List<String> ids) {
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            for (var prog : pkg.getPrograms()) {
////                for (var therapy : prog.getTherapyData()) {
////                    for (var ex : therapy.getExercises()) {
////
////                        for (var s : ex.getSessions()) {
////                            if (ids.contains(s.getSessionId())) {
////                                s.setPaymentStatus("Paid");
////                            }
////                        }
////
////                        boolean allPaid = ex.getSessions().stream()
////                                .allMatch(x -> "Paid".equalsIgnoreCase(x.getPaymentStatus()));
////
////                        ex.setPaymentStatus(allPaid ? "Paid" : "Partial");
////                    }
////                }
////            }
////        }
////    }
////
////    // ================= STATUS PROPAGATION =================
////    private void updateStatuses(PaymentRecord record) {
////
////        if (record.getTherapyWithSessions() == null) return; // 🔥 FIX
////
////        for (var pkg : record.getTherapyWithSessions()) {
////
////            if (pkg.getPrograms() == null) continue;
////
////            for (var prog : pkg.getPrograms()) {
////
////                if (prog.getTherapyData() == null) continue;
////
////                for (var therapy : prog.getTherapyData()) {
////
////                    if (therapy.getExercises() == null) continue;
////
////                    for (var ex : therapy.getExercises()) {
////
////                        if (ex.getSessions() == null || ex.getSessions().isEmpty()) {
////                            ex.setPaymentStatus("Unpaid");
////                            continue;
////                        }
////
////                        boolean allPaid = ex.getSessions().stream()
////                                .allMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));
////
////                        boolean anyPaid = ex.getSessions().stream()
////                                .anyMatch(s -> "Paid".equalsIgnoreCase(s.getPaymentStatus()));
////
////                        if (allPaid) ex.setPaymentStatus("Paid");
////                        else if (anyPaid) ex.setPaymentStatus("Partial");
////                        else ex.setPaymentStatus("Unpaid");
////                    }
////                }
////            }
////        }
////    }
////    // ================= UTIL =================
////    
////    private String getStatus(PaymentRecord r) {
////
////        if (r.getTotalPaid() <= 0) return "Unpaid";
////
////        if (r.getTotalPaid() < r.getFinalAmount()) return "Partial";
////
////        if (Double.compare(r.getTotalPaid(), r.getFinalAmount()) == 0) return "Paid";
////
////        return "Overpaid"; // safety fallback
////    }
////
////    private PaymentHistory buildHistory(PaymentRequest req) {
////        return new PaymentHistory(
////                req.getAmount(),
////                req.getPaymentMode(),
////                req.getPaymentType(),
////                req.getPaymentDate(),
////                req.getPaymentLevel(),
////                req.getDiscountAmount(),
////                req.getDiscountIssuedBy()
////        );
////    }
////
////    private double calculateTotal(List<TherapyWithSessions> data) {
////
////        double total = 0;
////
////        for (var pkg : data) {
////
////            double pkgTotal = 0;
////
////            for (var prog : pkg.getPrograms()) {
////
////                double progTotal = 0;
////
////                for (var therapy : prog.getTherapyData()) {
////
////                    double therapyTotal = 0;
////
////                    for (var ex : therapy.getExercises()) {
////
////                        double exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
////
////                        ex.setTotalExercisePrice(exTotal);
////                        therapyTotal += exTotal;
////                    }
////
////                    // ✅ FIX
////                    therapy.setTotalTherapyPrice(therapyTotal);
////
////                    progTotal += therapyTotal;
////                }
////
////                // ✅ FIX
////                prog.setTotalProgramPrice(progTotal);
////
////                pkgTotal += progTotal;
////            }
////
////            // ✅ FIX
////            pkg.setTotalPackagePrice(pkgTotal);
////
////            total += pkgTotal;
////        }
////
////        return total;
////    }
////
////    private boolean createSessions(List<TherapyWithSessions> data, String startDate) {
////
////        boolean created = false;
////
////        for (var pkg : data) {
////            for (var prog : pkg.getPrograms()) {
////                for (var therapy : prog.getTherapyData()) {
////                    for (var ex : therapy.getExercises()) {
////
////                        List<Session> sessions = new ArrayList<>();
////
////                        for (int i = 1; i <= ex.getNoOfSessions(); i++) {
////                            sessions.add(new Session(
////                                    ex.getExerciseId() + "_" + i,
////                                    i,
////                                    startDate,
////                                    "Pending",
////                                    "Unpaid"
////                            ));
////                        }
////
////                        if (!sessions.isEmpty()) created = true;
////
////                        ex.setSessions(sessions);
////                    }
////                }
////            }
////        }
////
////        return created;
////    }
////
////    private int countCompleted(PaymentRecord record) {
////
////        int count = 0;
////
////        for (var pkg : record.getTherapyWithSessions()) {
////            for (var prog : pkg.getPrograms()) {
////                for (var therapy : prog.getTherapyData()) {
////                    for (var ex : therapy.getExercises()) {
////                        for (var s : ex.getSessions()) {
////                            if ("Completed".equalsIgnoreCase(s.getStatus())) {
////                                count++;
////                            }
////                        }
////                    }
////                }
////            }
////        }
////
////        return count;
////    }
////
////    @Override
////    public PaymentRecord getByBookingId(String bookingId) {
////
////        PaymentRecord record = repo.findByBookingId(bookingId)
////                .orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));
////
////        // ✅ Recalculate completed sessions
////        int completed = countCompleted(record);
////
////        record.setNoOfSessionCompletedCount(completed);
////        record.setNoOfSessionCompletedStatus(
////                completed >= record.getTotalSessionCount()
////        );
////
////        return record;
////    }
////    @Override
////    public void deleteByBookingId(String bookingId) {
////
////        PaymentRecord record = repo.findByBookingId(bookingId)
////                .orElseThrow(() -> new RuntimeException("Payment not found for bookingId: " + bookingId));
////
////        repo.delete(record);
////    }
////    @Override
////    public void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId) {
////
////        PaymentRecord record = repo.findByTherapistRecordId(therapistRecordId)
////                .orElseThrow(() -> new RuntimeException("Payment record not found"));
////
////        List<TherapyWithSessions> packageList = record.getTherapyWithSessions();
////
////        if (packageList == null || packageList.isEmpty()) {
////            throw new RuntimeException("No sessions found");
////        }
////
////        boolean sessionFound = false;
////
////        for (TherapyWithSessions packageData : packageList) {
////
////            List<Program> programList = packageData.getPrograms();
////
////            if (programList == null || programList.isEmpty()) {
////                continue;
////            }
////
////            for (Program program : programList) {
////
////                List<TherapyData> therapyList = program.getTherapyData();
////
////                if (therapyList == null || therapyList.isEmpty()) {
////                    continue;
////                }
////
////                for (TherapyData therapy : therapyList) {
////
////                    List<TherapyExercise> exerciseList = therapy.getExercises();
////
////                    if (exerciseList == null || exerciseList.isEmpty()) {
////                        continue;
////                    }
////
////                    for (TherapyExercise exercise : exerciseList) {
////
////                        List<Session> sessionList = exercise.getSessions();
////
////                        if (sessionList == null || sessionList.isEmpty()) {
////                            continue;
////                        }
////
////                        for (Session session : sessionList) {
////
////                            if (sessionId.equals(session.getSessionId())) {
////                                session.setStatus("Completed");
////                                sessionFound = true;
////                                break;
////                            }
////                        }
////
////                        if (sessionFound) {
////                            break;
////                        }
////                    }
////
////                    if (sessionFound) {
////                        break;
////                    }
////                }
////
////                if (sessionFound) {
////                    break;
////                }
////            }
////
////            if (sessionFound) {
////                break;
////            }
////        }
////
////        if (!sessionFound) {
////            throw new RuntimeException("Session not found with ID: " + sessionId);
////        }
////
////        record.setOverallStatus(calculateOverallStatus(record));
////
////        repo.save(record);
////
////        updateBookingStatus(record);
////    }
////
////    private String calculateOverallStatus(PaymentRecord record) {
////
////        List<TherapyWithSessions> packageList = record.getTherapyWithSessions();
////
////        if (packageList == null || packageList.isEmpty()) {
////            return "Pending";
////        }
////
////        boolean allCompleted = true;
////        boolean anyCompleted = false;
////
////        for (TherapyWithSessions packageData : packageList) {
////
////            List<Program> programList = packageData.getPrograms();
////
////            if (programList == null || programList.isEmpty()) {
////                continue;
////            }
////
////            for (Program program : programList) {
////
////                List<TherapyData> therapyList = program.getTherapyData();
////
////                if (therapyList == null || therapyList.isEmpty()) {
////                    continue;
////                }
////
////                for (TherapyData therapy : therapyList) {
////
////                    List<TherapyExercise> exerciseList = therapy.getExercises();
////
////                    if (exerciseList == null || exerciseList.isEmpty()) {
////                        continue;
////                    }
////
////                    for (TherapyExercise exercise : exerciseList) {
////
////                        List<Session> sessionList = exercise.getSessions();
////
////                        if (sessionList == null || sessionList.isEmpty()) {
////                            continue;
////                        }
////
////                        for (Session session : sessionList) {
////
////                            if ("Completed".equalsIgnoreCase(session.getStatus())) {
////                                anyCompleted = true;
////                            } else {
////                                allCompleted = false;
////                            }
////                        }
////                    }
////                }
////            }
////        }
////
////        if (allCompleted) {
////            return "Completed";
////        }
////
////        if (anyCompleted) {
////            return "Active";
////        }
////
////        return "Pending";
////    }
////
////    private void updateBookingStatus(PaymentRecord record) {
////
////        if (record.getBookingId() == null || record.getBookingId().trim().isEmpty()) {
////            return;
////        }
////
////        try {
////
////            BookingResponse request = new BookingResponse();
////            request.setBookingId(record.getBookingId().trim());
////
////            // ✅ Map payment overall status to booking status
////            if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {
////                request.setStatus("completed");
////            } else if ("Active".equalsIgnoreCase(record.getOverallStatus())) {
////                request.setStatus("in-progress");
////            } else {
////                request.setStatus("pending");
////            }
////
////            clinicAdminFeign.updateAppointment(request);
////
////            System.out.println("Booking status updated successfully => "
////                    + request.getBookingId() + " | " + request.getStatus());
////
////        } catch (Exception e) {
////            System.out.println("Booking status update failed");
////            e.printStackTrace();
////        }
////    }
//
////}