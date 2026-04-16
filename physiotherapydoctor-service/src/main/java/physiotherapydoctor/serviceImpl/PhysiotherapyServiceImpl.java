package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.AssignTherapistPatientListDTO;
import physiotherapydoctor.dto.DoctorProgram;
import physiotherapydoctor.dto.DoctorTherapyData;
import physiotherapydoctor.dto.DoctorTherapyExercise;
import physiotherapydoctor.dto.DoctorTherapySession;
import physiotherapydoctor.dto.ExcerciseDTO;
import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.ProgramAndTherophyAndExcercisesInfo;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TherapyData;
import physiotherapydoctor.dto.TherapyExercise;
import physiotherapydoctor.dto.TherapySession;
import physiotherapydoctor.dto.TherophyDataDto;
import physiotherapydoctor.dto.TreatmentPlan;
import physiotherapydoctor.entity.PhysiotherapyRecord;
import physiotherapydoctor.feign.BookingFeign;
import physiotherapydoctor.repository.PhysiotherapydoctorRespository;
import physiotherapydoctor.service.PhysiotherapyService;

@Service
@RequiredArgsConstructor
public class PhysiotherapyServiceImpl implements PhysiotherapyService {

	private final PhysiotherapydoctorRespository repository;

	@Autowired
	private BookingFeign bookingFeign;

@Override
public Response create(PhysiotherapyRecordDTO dto) {

    try {
        if (dto == null) {
            return buildResponse(false, null, "Request body is null", 400);
        }

        // ✅ Normalize Doctor Sessions
        if (dto.getTherapySessions() != null) {
            dto.getTherapySessions().forEach(this::normalizeSession);
        }

        PhysiotherapyRecord entity = mapToEntity(dto);

        entity.setCreatedAt(dto.getCreatedAt());
  
        entity.setOverallStatus(dto.getOverallStatus());

        PhysiotherapyRecord saved = repository.save(entity);

        return buildResponse(true, saved, "Record created successfully", 201);

    } catch (Exception e) {
        return buildResponse(false, null, e.getMessage(), 500);
    }
}
private void normalizeSession(DoctorTherapySession session) {

    if (session == null || session.getServiceType() == null) {
        return;
    }

    switch (session.getServiceType().toLowerCase()) {

        case "package":
            if (session.getPrograms() != null) {
                for (DoctorProgram program : session.getPrograms()) {
                    normalizeProgram(program);
                }
            }
            break;

        case "program":
            if (session.getTherapyData() != null) {
                for (DoctorTherapyData therapy : session.getTherapyData()) {
                    normalizeTherapy(therapy);
                }
            }
            break;

        case "therapy":
        case "exercise":
            // ✅ KEEP AS IS (your requirement)
            break;

        default:
            break;
    }
}
private void normalizeProgram(DoctorProgram program) {

    if (program == null) return;

    if (program.getTherapyData() != null) {
        for (DoctorTherapyData therapy : program.getTherapyData()) {
            normalizeTherapy(therapy);
        }
    }
}
private void normalizeTherapy(DoctorTherapyData therapy) {

    if (therapy == null) return;

    if (therapy.getExercises() != null) {
        for (DoctorTherapyExercise ex : therapy.getExercises()) {
            // no validation needed (your requirement)
        }
    }
}
	// =========================================================
	// 🔵 THERAPY → PROGRAM
	// =========================================================
	private void convertTherapyToProgram(TherapySession session) {

		TherapyData therapy = new TherapyData();
		therapy.setTherapyId(session.getTherapyId());
		therapy.setTherapyName(session.getTherapyName());
		therapy.setExercises(session.getExercises());
		therapy.setTotalTherapyPrice(session.getTotalPrice());

		List<TherapyData> therapyList = new ArrayList<>();
		therapyList.add(therapy);

		// ✅ MUST CHANGE TYPE
		session.setServiceType(session.getServiceType());

		// ✅ Safe ID generation
		session.setProgramId(
				 session.getProgramId());

		session.setProgramName(session.getProgramName());

		session.setTherapyData(therapyList);

		// ✅ CLEANUP
		session.setTherapyId(null);
		session.setTherapyName(null);
		session.setExercises(null);
	}

	// =========================================================
	// 🔴 EXERCISE → PROGRAM
	// =========================================================
	private void convertExerciseToProgram(TherapySession session) {

		TherapyData therapy = new TherapyData();
		therapy.setTherapyId(session.getTherapyId());
		therapy.setTherapyName(session.getTherapyName());
		therapy.setExercises(session.getExercises());
		therapy.setTotalTherapyPrice(session.getTotalPrice());

		List<TherapyData> therapyList = new ArrayList<>();
		therapyList.add(therapy);

		// ✅ MUST CHANGE TYPE
		session.setServiceType(session.getServiceType());

		session.setProgramId(
				session.getProgramId() );

		session.setProgramName(session.getProgramName());

		session.setTherapyData(therapyList);

		// ✅ CLEANUP
		session.setExercises(null);
	}

	// =========================================================
	// 🟢 COMMON METHODS
	// =========================================================
	private PhysiotherapyRecord mapToEntity(PhysiotherapyRecordDTO dto) {

		PhysiotherapyRecord entity = new PhysiotherapyRecord();

//        entity.setTherapistRecordId(dto.getTherapistRecordId());
		entity.setBookingId(dto.getBookingId());
		entity.setClinicId(dto.getClinicId());
		entity.setBranchId(dto.getBranchId());

		entity.setPatientInfo(dto.getPatientInfo());
		entity.setComplaints(dto.getComplaints());
		entity.setInvestigation(dto.getInvestigation());
		entity.setAssessment(dto.getAssessment());
		entity.setDiagnosis(dto.getDiagnosis());
		entity.setTreatmentPlan(dto.getTreatmentPlan());

		entity.setTherapySessions(dto.getTherapySessions());

		entity.setExercisePlan(dto.getExercisePlan());
		entity.setFollowUp(dto.getFollowUp());

		entity.setPerceptionPdf(dto.getPerceptionPdf());

		return entity;
	}

	private Response buildResponse(boolean success, Object data, String message, int status) {
		Response res = new Response();
		res.setSuccess(success);
		res.setData(data);
		res.setMessage(message);
		res.setStatus(status);
		return res;
	}

//	private void calculateTherapyPrices(List<TherapySession> sessions) {
//
//		if (sessions == null)
//			return;
//
//		for (TherapySession session : sessions) {
//
//			if (session.getPrograms() != null) {
//
//				for (Program p : session.getPrograms()) {
//
//					double programTotal = 0; // ✅ ADD THIS
//
//					if (p.getTherapyData() != null) {
//
//						for (TherapyData t : p.getTherapyData()) {
//
//							double therapyTotal = 0;
//
//							if (t.getExercises() != null) {
//
//								for (TherapyExercise ex : t.getExercises()) {
//
//									double exerciseTotal = 0;
//
//									if (ex.getTotalExercisePrice() != null) {
//										exerciseTotal = ex.getTotalExercisePrice();
//									} else if (ex.getPricePerSession() != null && ex.getNoOfSessions() != null) {
//										exerciseTotal = ex.getPricePerSession() * ex.getNoOfSessions();
//									}
//
//									ex.setTotalExercisePrice(exerciseTotal);
//									therapyTotal += exerciseTotal;
//								}
//							}
//
//							t.setTotalPrice(therapyTotal);
//
//							// ✅ ADD THIS
//							programTotal += therapyTotal;
//						}
//					}
//
//					// ✅ VERY IMPORTANT (THIS FIXES YOUR ISSUE)
//					p.setTotalPrice(programTotal);
//				}
//			}
//
//			// PROGRAM
//			if (session.getTherapyData() != null) {
//				for (TherapyData t : session.getTherapyData()) {
//
//					double total = 0;
//
//					if (t.getExercises() != null) {
//						for (TherapyExercise ex : t.getExercises()) {
//							if (ex.getTotalExercisePrice() != null) {
//								total += ex.getTotalExercisePrice();
//							}
//						}
//					}
//
//					t.setTotalPrice(total); // ✅ FIX
//				}
//			}
//		}
//	}

	// @Override

//	public Response create(PhysiotherapyRecordDTO dto) {
//
//		Response response = new Response();
//
//		if (dto == null) {
//			response.setSuccess(false);
//			response.setData(null);
//			response.setMessage("Request body is null");
//			response.setStatus(400);
//			return response;
//		}
//
//		PhysiotherapyRecord dtoData = mapToEntity(dto);
//
//		// ✅ Set ID
//		dtoData.setTherapistRecordId(dto.getTherapistRecordId());
//

//
////	    // ✅ Set session status
////	    if (dtoData.getTherapySessions() != null) {
////	        for (TherapySession s : dtoData.getTherapySessions()) {
////	            if (s.getStatus() == null || s.getStatus().isEmpty()) {
////	                s.setStatus("Pending");
////	            }
////	        }
////	    }
//
//		// ✅ Set overall status
//		dtoData.setOverallStatus("Pending");
//
//		// ✅ DATE FIX (STRING FORMAT - NO CHANGE)
//		String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//		// if frontend sends → use it, else auto-generate
//		dtoData.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : now);
//		dtoData.setUpdatedAt(now);
//
//		// ✅ Save record
//		PhysiotherapyRecord saved = repository.save(dtoData);
//
//		// ✅ BOOKING UPDATE (same logic, cleaned)
//		if (dto.getBookingId() != null && !dto.getBookingId().isEmpty()) {
//
//			try {
//				ResponseStructure<BookingResponse> res = bookingFeign.getBookingById(dto.getBookingId());
//
//				if (res != null && res.getData() != null) {
//
//					BookingResponse oldBooking = res.getData();
//
//					BookingResponse updateRequest = new BookingResponse();
//					updateRequest.setBookingId(oldBooking.getBookingId());
//					updateRequest.setStatus("Active");
//
//					// optional fields
//					updateRequest.setName(oldBooking.getName());
//					updateRequest.setMobileNumber(oldBooking.getMobileNumber());
//
//					bookingFeign.updateAppointment(updateRequest);
//				}
//
//			} catch (Exception e) {
//				System.out.println("Booking update failed: " + e.getMessage());
//			}
//		}
//
//		response.setSuccess(true);
//		response.setData(saved);
//		response.setMessage("Record created successfully");
//		response.setStatus(201);
//
//		return response;
//	}

	// ✅ GET BY ID
	@Override
	public Response getById(String id) {

		Response response = new Response();

		if (id == null || id.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("ID is required");
			response.setStatus(400);
			return response;
		}

		Optional<PhysiotherapyRecord> optional = repository.findById(id);

		if (optional.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Record not found");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(optional.get());
		response.setMessage("Success");
		response.setStatus(200);

		return response;
	}

	// ✅ GET ALL
	@Override
	public Response getAll() {

		Response response = new Response();

		List<PhysiotherapyRecord> list = repository.findAll();

		if (list.isEmpty()) {
			response.setSuccess(false);
			response.setData(list);
			response.setMessage("No records found");
			response.setStatus(204);
			return response;
		}

		response.setSuccess(true);
		response.setData(list);
		response.setMessage("Success");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response update(String id, PhysiotherapyRecordDTO dto) {

		Response response = new Response();

		if (id == null || id.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("ID is required");
			response.setStatus(400);
			return response;
		}

		if (dto == null) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Request body is null");
			response.setStatus(400);
			return response;
		}

		Optional<PhysiotherapyRecord> optional = repository.findById(id);

		if (optional.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Record not found");
			response.setStatus(404);
			return response;
		}

		PhysiotherapyRecord existing = optional.get();

		// 🔥 NULL SAFE UPDATE
		if (dto.getAssessment() != null) {
			existing.setAssessment(dto.getAssessment());
		}

		if (dto.getDiagnosis() != null) {
			existing.setDiagnosis(dto.getDiagnosis());
		}

		if (dto.getTreatmentPlan() != null) {
			existing.setTreatmentPlan(dto.getTreatmentPlan());
		}

		// ✅ IMPORTANT: handle sessions properly
		if (dto.getTherapySessions() != null) {

//	        // generate sessionId for new sessions
//	        generateSessionIds(dto.getTherapySessions());

			existing.setTherapySessions(dto.getTherapySessions());
		}

		// ✅ HOME EXERCISE UPDATE
		if (dto.getExercisePlan() != null) {
			existing.setExercisePlan(dto.getExercisePlan());
		}

//	    if (dto.getProgressNotes() != null) {
//	        existing.setProgressNotes(dto.getProgressNotes());
//	    }

		if (dto.getFollowUp() != null) {
			existing.setFollowUp(dto.getFollowUp());
		}

//	    if (dto.getProgressAnalytics() != null) {
//	        existing.setProgressAnalytics(dto.getProgressAnalytics());
//	    }

		if (dto.getOverallStatus() != null) {
			existing.setOverallStatus(dto.getOverallStatus());
		}

		// ✅ DATE FIX (STRING FORMAT - AUTO UPDATE)
		String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		existing.setUpdatedAt(now);

		PhysiotherapyRecord updated = repository.save(existing);

		response.setSuccess(true);
		response.setData(updated);
		response.setMessage("Updated successfully");
		response.setStatus(200);

		return response;
	}

	// ✅ DELETE
	@Override
	public Response delete(String id) {

		Response response = new Response();

		if (id == null || id.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("ID is required");
			response.setStatus(400);
			return response;
		}

		if (!repository.existsById(id)) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Record not found");
			response.setStatus(404);
			return response;
		}

		repository.deleteById(id);

		response.setSuccess(true);
		response.setData(null);
		response.setMessage("Deleted successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getByMultipleFields(String clinicId, String branchId, String patientId, String bookingId,
			String therapistRecordId) {

		Response response = new Response();

		if (clinicId == null || branchId == null || patientId == null || bookingId == null
				|| therapistRecordId == null) {

			response.setSuccess(false);
			response.setMessage("All fields are required");
			response.setStatus(400);
			return response;
		}

		Optional<PhysiotherapyRecord> record = repository
				.findByClinicIdAndBranchIdAndPatientInfoPatientIdAndBookingIdAndTherapistRecordId(clinicId, branchId,
						patientId, bookingId, therapistRecordId);

		if (record.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("Record not found");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(record.get());
		response.setMessage("Record fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getByWithoutTherapistRecordId(String clinicId, String branchId, String patientId,
			String bookingId) {

		Response response = new Response();

		if (clinicId == null || branchId == null || patientId == null || bookingId == null) {
			response.setSuccess(false);
			response.setMessage("All fields are required");
			response.setStatus(400);
			return response;
		}

		List<PhysiotherapyRecord> records = repository
				.findByClinicIdAndBranchIdAndPatientInfoPatientIdAndBookingId(clinicId, branchId, patientId, bookingId);

		if (records == null || records.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No records found");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(records);
		response.setMessage("Records fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getAssignedPatients(String clinicId, String branchId, String therapistId, Integer overallStatus) {

		Response response = new Response();

		List<PhysiotherapyRecord> records = repository.findByClinicIdAndBranchIdAndTreatmentPlanTherapistId(clinicId,
				branchId, therapistId);

		if (records == null || records.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No assigned patients found");
			response.setStatus(404);
			return response;
		}

		// 🔥 STATUS MAPPING (1 → Pending, 2 → Active, 3 → Completed)
		String statusFilter = null;

		if (overallStatus != null) {
			switch (overallStatus) {
			case 1:
				statusFilter = "Pending";
				break;
			case 2:
				statusFilter = "Active";
				break;
			case 3:
				statusFilter = "Completed";
				break;
			}
		}

		// 🔥 MAP FOR UNIQUE RECORDS
		Map<String, AssignTherapistPatientListDTO> map = new LinkedHashMap<>();

		for (PhysiotherapyRecord record : records) {

			// ✅ STATUS FILTER
			if (statusFilter != null) {
				if (record.getOverallStatus() == null || !record.getOverallStatus().equalsIgnoreCase(statusFilter)) {
					continue;
				}
			}

			if (record.getTherapySessions() == null)
				continue;
			if (record.getPatientInfo() == null)
				continue;

			for (DoctorTherapySession session : record.getTherapySessions()) {

				if (session.getProgramId() == null && session.getProgramName() == null) {
					continue;
				}

				// 🔥 UPDATED KEY (FIXED ISSUE)
				String key = record.getTherapistRecordId() + "_"
						+ (session.getProgramId() != null ? session.getProgramId() : "NA");

				if (map.containsKey(key))
					continue;

				AssignTherapistPatientListDTO dto = new AssignTherapistPatientListDTO();

				// ✅ BASIC
				dto.setBookingId(record.getBookingId());
				dto.setTherapistRecordId(record.getTherapistRecordId());
				dto.setClinicId(record.getClinicId());
				dto.setBranchId(record.getBranchId());

				// ✅ PATIENT INFO
				dto.setPatientId(record.getPatientInfo().getPatientId());
				dto.setPatientName(
						record.getPatientInfo().getPatientName() != null ? record.getPatientInfo().getPatientName()
								: "Unknown");
				dto.setMobileNumber(record.getPatientInfo().getMobileNumber());
				dto.setAge(record.getPatientInfo().getAge());
				dto.setSex(record.getPatientInfo().getSex());

				// ✅ TREATMENT PLAN
				if (record.getTreatmentPlan() != null) {
					dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
					dto.setTherapistName(record.getTreatmentPlan().getTherapistName());

					dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
					dto.setDoctorName(record.getTreatmentPlan().getDoctorName());
				}

				// ✅ SESSION DATA
				dto.setProgramId(session.getProgramId() != null ? session.getProgramId() : "N/A");
				dto.setProgramName(session.getProgramName());
				dto.setSerivceType(session.getServiceType() != null ? session.getServiceType() : "N/A");

				// 🔥 ADD STATUS ALSO (IMPORTANT FOR UI)
				dto.setOverallStatus(record.getOverallStatus());

				map.put(key, dto);
			}
		}

		List<AssignTherapistPatientListDTO> dtoList = new ArrayList<>(map.values());

		if (dtoList.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No patients found for given status");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(dtoList);
		response.setMessage("Assigned patients fetched successfully");
		response.setStatus(200);

		return response;
	}
	// @Override
//	public Response getTherapistDashboard(String clinicId, String branchId, String therapistId) {
//
//		Response response = new Response();
//
//		List<PhysiotherapyRecord> records = repository.findByClinicIdAndBranchIdAndTreatmentPlanTherapistId(clinicId,
//				branchId, therapistId);
//
//		if (records.isEmpty()) {
//			response.setSuccess(false);
//			response.setMessage("No records found");
//			response.setStatus(404);
//			return response;
//		}
//
//		LocalDate today = LocalDate.now();
//		LocalDate weekStart = today.minusDays(7);
//		LocalDate monthStart = today.minusDays(30);
//
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//
//		int todayCount = 0, weekCount = 0, monthCount = 0;
//		long todayMinutes = 0, weekMinutes = 0, monthMinutes = 0;
//
//		for (PhysiotherapyRecord record : records) {
//
//			if (record.getTherapySessions() == null)
//				continue;
//
//			boolean countedToday = false;
//			boolean countedWeek = false;
//			boolean countedMonth = false;
//
//			for (TherapySession session : record.getTherapySessions()) {
//
//				if (session.getSessionDate() == null)
//					continue;
//
//				LocalDate sessionDate = parseDate(session.getSessionDate(), formatter);
//
//				if (sessionDate == null) continue; // ✅ ADD THIS
//				long duration = parseDuration(session.getDuration());
//
//				// ✅ TODAY
//				if (sessionDate.equals(today)) {
//					if (!countedToday) {
//						todayCount++; // count patient once
//						countedToday = true;
//					}
//					todayMinutes += duration;
//				}
//
//				// ✅ WEEK
//				if (!sessionDate.isBefore(weekStart)) {
//					if (!countedWeek) {
//						weekCount++;
//						countedWeek = true;
//					}
//					weekMinutes += duration;
//				}
//
//				// ✅ MONTH
//				if (!sessionDate.isBefore(monthStart)) {
//					if (!countedMonth) {
//						monthCount++;
//						countedMonth = true;
//					}
//					monthMinutes += duration;
//				}
//			}
//		}
//
//		TherapistDashboardResponse dashboard = new TherapistDashboardResponse();
//		dashboard.setTodayPatientCount(todayCount);
//		dashboard.setTodayWorkingMinutes(todayMinutes);
//
//		dashboard.setWeeklyPatientCount(weekCount);
//		dashboard.setWeeklyWorkingMinutes(weekMinutes);
//
//		dashboard.setMonthlyPatientCount(monthCount);
//		dashboard.setMonthlyWorkingMinutes(monthMinutes);
//
//		dashboard.setRecords(records);
//
//		response.setSuccess(true);
//		response.setData(dashboard);
//		response.setMessage("Dashboard fetched successfully");
//		response.setStatus(200);
//
//		return response;
//	}

	private LocalDate parseDate(String date, DateTimeFormatter formatter) {
		try {
			if (date == null || date.isEmpty())
				return null;

			String cleanDate = date.length() >= 10 ? date.substring(0, 10) : date;
			return LocalDate.parse(cleanDate, formatter);

		} catch (Exception e) {
			return null; // 🔥 SAFE
		}
	}

	private long parseDuration(String duration) {

		if (duration == null || duration.isEmpty())
			return 0;

		duration = duration.toLowerCase().trim();

		try {
			long value = Long.parseLong(duration.replaceAll("[^0-9]", ""));

			// support: "1 hour", "2 hrs"
			if (duration.contains("hour") || duration.contains("hr")) {
				return value * 60;
			}

			return value; // minutes

		} catch (Exception e) {
			return 0;
		}
	}
}
//	private void generateSessionIds(List<TherapySession> sessions) {
//
//		if (sessions == null || sessions.isEmpty())
//			return;
//
//		for (TherapySession session : sessions) {
//
//			// ✅ Generate UNIQUE sessionId
//			session.setSessionId("SES-" + System.currentTimeMillis());
//
//			// small delay to avoid same millis
//			try {
//				Thread.sleep(1);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
//
//			// ✅ Auto set status if null
//			if (session.getStatus() == null || session.getStatus().isEmpty()) {
//				session.setStatus("Pending");
//			}
//		}
//	}

//	public void updateSessionStatusFromTherapist(String therapistRecordId, String sessionId) {
//
//		PhysiotherapyRecord record = repository.findByTherapistRecordId(therapistRecordId)
//				.orElseThrow(() -> new RuntimeException("Record not found"));
//
//		List<TherapySession> sessions = record.getTherapySessions();
//
//		if (sessions == null || sessions.isEmpty()) {
//			throw new RuntimeException("No sessions found");
//		}
//
//		boolean sessionFound = false;
//
//		for (TherapySession session : sessions) {
//
//			// ✅ MATCH sessionId safely
//			if (sessionId.equals(session.getSessionId())) {
//
//				// ✅ Update status
//				session.setStatus("Completed");
//				sessionFound = true;
//				break;
//			}
//		}
//
//		if (!sessionFound) {
//			throw new RuntimeException("Session not found with ID: " + sessionId);
//		}
//
//		// ✅ UPDATE OVERALL STATUS
//		record.setOverallStatus(calculateOverallStatus(sessions));
//
//		repository.save(record);
//		// ======================================================
//		// 🔥 ADD THIS BLOCK (BOOKING UPDATE)
//		// ======================================================
//		if (record.getBookingId() != null && !record.getBookingId().isEmpty()) {
//
//			try {
//				ResponseStructure<BookingResponse> res = bookingFeign.getBookingById(record.getBookingId());
//
//				if (res != null && res.getData() != null) {
//
//					BookingResponse updateRequest = new BookingResponse();
//					updateRequest.setBookingId(record.getBookingId());
//
//					// ✅ CORE LOGIC
//					if ("Completed".equalsIgnoreCase(record.getOverallStatus())) {
//						updateRequest.setStatus("Completed"); // 🔥 Active → Completed
//					} else {
//						updateRequest.setStatus("Active");
//					}
//
//					bookingFeign.updateAppointment(updateRequest);
//				}
//			} catch (Exception e) {
//			}
//		}
//	}
//
//	private String calculateOverallStatus(List<TherapySession> sessions) {
//
//		if (sessions == null || sessions.isEmpty()) {
//			return "Pending";
//		}
//
//		boolean allCompleted = true;
//		boolean anyCompleted = false;
//
//		for (TherapySession s : sessions) {
//
//			if ("Completed".equalsIgnoreCase(s.getStatus())) {
//				anyCompleted = true;
//			} else {
//				allCompleted = false;
//			}
//		}
//
//		if (allCompleted)
//			return "Completed";
//		if (anyCompleted)
//			return "Active";
//
//		return "Pending";
//	}

//	@Override
//	public Response getProgramAndTherapyInfo(String clinicId, String branchId, String patientId, String bookingId) {
//
//		Response response = new Response();
//
//		Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, patientId, bookingId);
//
//		if (!fetchedResponse.isSuccess()) {
//			return fetchedResponse;
//		}
//
//		List<PhysiotherapyRecord> records = (List<PhysiotherapyRecord>) fetchedResponse.getData();
//
//		List<ProgramAndTherophyAndExcercisesInfo> resultList = new ArrayList<>();
//
//		for (PhysiotherapyRecord record : records) {
//
//			List<DoctorTherapySession> therapySessions = record.getTherapySessions();
//
//			if (therapySessions == null || therapySessions.isEmpty())
//				continue;
//
//			for (DoctorTherapySession session : therapySessions) {
//
//				ProgramAndTherophyAndExcercisesInfo info = new ProgramAndTherophyAndExcercisesInfo();
//
//				TreatmentPlan plan = record.getTreatmentPlan();
//
//				// ✅ BASIC
//				info.setDoctorName(plan != null ? plan.getDoctorName() : null);
//				info.setDoctorId(plan != null ? plan.getDoctorId() : null);
//				info.setTherapistName(plan != null ? plan.getTherapistName() : null);
//				info.setTherapistId(plan != null ? plan.getTherapistId() : null);
//
//				info.setBookingId(record.getBookingId());
//				info.setTherapistRecordId(record.getTherapistRecordId());
//				info.setPatientId(record.getPatientInfo() != null ? record.getPatientInfo().getPatientId() : null);
//
//				info.setProgramId(session.getProgramId());
//				info.setProgramName(session.getProgramName());
//
//				info.setClinicId(record.getClinicId());
//				info.setBranchId(record.getBranchId());
//
//				List<DoctorTherapyData> therapyDataList = session.getTherapyData();
//
//				int programCostTotal = 0;
//				int programSessionCountTotal = 0;
//				int therapyCount = 0;
//
//				List<TherophyDataDto> therapyDtos = new ArrayList<>();
//
//				if (therapyDataList != null) {
//
//					for (DoctorTherapyData therapy : therapyDataList) {
//
//						TherophyDataDto therapyDto = new TherophyDataDto();
//
//						therapyDto.setTherapyId(therapy.getTherapyId());
//						therapyDto.setTherapyName(therapy.getTherapyName());
//
//						int therapySessionCount = 0;
//						int exerciseCount = 0;
//						int therapyCost = 0;
//
//						List<ExcerciseDTO> exerciseDtos = new ArrayList<>();
//
//						if (therapy.getExercises() != null) {
//
//							for (DoctorTherapyExercise ex : therapy.getExercises()) {
//
//								ExcerciseDTO exDto = new ExcerciseDTO();
//
//								exDto.setExerciseId(ex.getTherapyExercisesId());;
//								exDto.setExerciseName(ex.getName());
//								exDto.setSets(ex.getSets());
//								exDto.setRepetitions(ex.getRepetitions());
//								exDto.setNotes(ex.getNotes());
//								exDto.setVideoUrl(ex.getVideoUrl());
//
//								// ✅ FREQUENCY FIX
//								int frequency = 0;
//								if (ex.getFrequency() != null) {
//									try {
//										String f = ex.getFrequency().replaceAll("[^0-9]", "");
//										frequency = Integer.parseInt(f);
//									} catch (Exception e) {
//										frequency = 0;
//									}
//								}
//								exDto.setFrequancy(frequency);
//
//								// ✅ NEW MODEL
//								String noOfSessions = ex.getSession() != null ? ex.getSession() : 0;
//								String price = ex.getSession() != null ? ex.getSession() : 0.0;
//
//								double totalCost;
//
//								if (ex.getTotalExercisePrice() != null) {
//									totalCost = ex.getTotalExercisePrice();
//								} else {
//									totalCost = noOfSessions * price;
//								}
//
//								exDto.setNoOfSessions(noOfSessions);
//								exDto.setPricePerSession(price);
//								exDto.setTotalSessionCost(totalCost);
//
//								exerciseDtos.add(exDto);
//
//								// ✅ accumulate
//								therapySessionCount += noOfSessions;
//								exerciseCount++;
//								therapyCost += totalCost;
//							}
//						}
//
//						therapyDto.setNoOfSessionCount(therapySessionCount);
//						therapyDto.setNoExerciseIdCount(exerciseCount);
//						therapyDto.setTherapyCost(therapyCost);
//						therapyDto.setExercises(exerciseDtos);
//
//						therapyDtos.add(therapyDto);
//
//						// program level
//						programCostTotal += therapyCost;
//						programSessionCountTotal += therapySessionCount;
//						therapyCount++;
//					}
//				}
//
//				info.setProgramCost(programCostTotal);
//				info.setNoOfSessionCount(programSessionCountTotal);
//				info.setNoTherapyCount(therapyCount);
//				info.setTherophyData(therapyDtos);
//
//				resultList.add(info);
//			}
//		}
//
//		if (resultList.isEmpty()) {
//			response.setSuccess(false);
//			response.setMessage("No therapy session data found");
//			response.setStatus(404);
//			return response;
//		}
//
//		response.setSuccess(true);
//		response.setData(resultList);
//		response.setMessage("Program and therapy info fetched successfully");
//		response.setStatus(200);
//
//		return response;
//	}
//}
