package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.AssignTherapistPatientListDTO;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.Exercise;
import physiotherapydoctor.dto.ExerciseCalculations;
import physiotherapydoctor.dto.PackageCalculation;
import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.Program;
import physiotherapydoctor.dto.ProgramAndTherophyAndExcercisesInfo;
import physiotherapydoctor.dto.ProgramCalculations;
import physiotherapydoctor.dto.ProgramDataForPackage;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.TheraphyInfo;
import physiotherapydoctor.dto.TherapyCalculations;
import physiotherapydoctor.dto.TherapyData;
import physiotherapydoctor.dto.TherapyExercise;
import physiotherapydoctor.dto.TherapySession;
import physiotherapydoctor.dto.TherapyinfoForPackage;
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

	    Response response = new Response();

	    if (dto == null) {
	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("Request body is null");
	        response.setStatus(400);
	        return response;
	    }

	    calculateTherapyPrices(dto.getTherapySessions());

	    PhysiotherapyRecord entity = mapToEntity(dto);

	    // ✅ ID
	    entity.setTherapistRecordId(dto.getTherapistRecordId());

	    // ✅ STATUS
	    entity.setOverallStatus("Pending");

	    // ✅ DATE
	    String now = java.time.LocalDateTime.now()
	            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

	    entity.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : now);
	    entity.setUpdatedAt(now);

	    // ✅ SAVE
	    PhysiotherapyRecord saved = repository.save(entity);

	    // ✅ BOOKING UPDATE
	    if (dto.getBookingId() != null && !dto.getBookingId().isEmpty()) {
	        try {
	            ResponseStructure<BookingResponse> res =
	                    bookingFeign.getBookingById(dto.getBookingId());

	            if (res != null && res.getData() != null) {
	                BookingResponse oldBooking = res.getData();

	                BookingResponse updateRequest = new BookingResponse();
	                updateRequest.setBookingId(oldBooking.getBookingId());
	                updateRequest.setStatus("Active");
	                updateRequest.setName(oldBooking.getName());
	                updateRequest.setMobileNumber(oldBooking.getMobileNumber());

	                bookingFeign.updateAppointment(updateRequest);
	            }

	        } catch (Exception e) {
	            System.out.println("Booking update failed: " + e.getMessage());
	        }
	    }

	    // 🔥🔥 IMPORTANT: TRANSFORM RESPONSE
	    List<Map<String, Object>> cleanSessions =
	            transformTherapySessions(saved.getTherapySessions());

	    saved.setTherapySessions((List) cleanSessions);

	    response.setSuccess(true);
	    response.setData(saved);
	    response.setMessage("Record created successfully");
	    response.setStatus(201);

	    return response;
	}private List<Map<String, Object>> transformTherapySessions(List<TherapySession> sessions) {

	    if (sessions == null) return null;

	    List<Map<String, Object>> result = new ArrayList<>();

	    for (TherapySession s : sessions) {

	        Map<String, Object> obj = new LinkedHashMap<>();

	        // ✅ Always include
	        obj.put("serviceType", s.getServiceType());
	        obj.put("totalPrice", s.getTotalPrice());

	        switch (s.getServiceType().toLowerCase()) {

	            case "package":
	                obj.put("packageId", s.getPackageId());
	                obj.put("packageName", s.getPackageName());
	                obj.put("programs", s.getPrograms());
	                break;

	            case "program":
	                obj.put("programId", s.getProgramId());
	                obj.put("programName", s.getProgramName());
	                obj.put("therapyData", s.getTherapyData());
	                break;

	            case "therapy":
	                obj.put("therapyId", s.getTherapyId());
	                obj.put("therapyName", s.getTherapyName());
	                obj.put("exercises", s.getExercises());
	                break;

	            case "exercise":
	                obj.put("exercises", s.getExercises());
	                break;
	        }

	        // 🔥 Remove null fields
	        obj.values().removeIf(Objects::isNull);

	        result.add(obj);
	    }

	    return result;
	}
	private void calculateTherapyPrices(List<TherapySession> sessions) {

	    if (sessions == null) return;

	    for (TherapySession session : sessions) {

	        // PACKAGE
	        if (session.getPrograms() != null) {
	            for (Program p : session.getPrograms()) {

	                if (p.getTherapyData() != null) {
	                    for (TherapyData t : p.getTherapyData()) {

	                        double total = 0;

	                        if (t.getExercises() != null) {
	                            for (TherapyExercise ex : t.getExercises()) {
	                                if (ex.getTotalPrice() != null) {
	                                    total += ex.getTotalPrice();
	                                }
	                            }
	                        }

	                        t.setTotalPrice(total); // ✅ FIX
	                    }
	                }
	            }
	        }

	        // PROGRAM
	        if (session.getTherapyData() != null) {
	            for (TherapyData t : session.getTherapyData()) {

	                double total = 0;

	                if (t.getExercises() != null) {
	                    for (TherapyExercise ex : t.getExercises()) {
	                        if (ex.getTotalPrice() != null) {
	                            total += ex.getTotalPrice();
	                        }
	                    }
	                }

	                t.setTotalPrice(total); // ✅ FIX
	            }
	        }
	    }
	}
//	@Override
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

	// ---------------- MAPPER ----------------
	private PhysiotherapyRecord mapToEntity(PhysiotherapyRecordDTO dto) {

	    PhysiotherapyRecord entity = new PhysiotherapyRecord();

	    if (dto == null)
	        return entity;

	    // =========================
	    // ✅ BASIC DETAILS
	    // =========================
	    entity.setBookingId(dto.getBookingId());
	    entity.setClinicId(dto.getClinicId());
	    entity.setBranchId(dto.getBranchId());
	    entity.setOverallStatus(dto.getOverallStatus());

	    // =========================
	    // ✅ PATIENT INFO
	    // =========================
	    if (dto.getPatientInfo() != null) {
	        entity.setPatientInfo(dto.getPatientInfo());
	    }

	    // =========================
	    // ✅ COMPLAINTS
	    // =========================
	    if (dto.getComplaints() != null) {
	        entity.setComplaints(dto.getComplaints());
	    }

	    // =========================
	    // 🔥 INVESTIGATION (MISSING FIX)
	    // =========================
	    if (dto.getInvestigation() != null) {
	        entity.setInvestigation(dto.getInvestigation());
	    }

	    // =========================
	    // ✅ ASSESSMENT
	    // =========================
	    if (dto.getAssessment() != null) {
	        entity.setAssessment(dto.getAssessment());
	    }

	    // =========================
	    // ✅ DIAGNOSIS
	    // =========================
	    if (dto.getDiagnosis() != null) {
	        entity.setDiagnosis(dto.getDiagnosis());
	    }

	    // =========================
	    // ✅ TREATMENT PLAN
	    // =========================
	    if (dto.getTreatmentPlan() != null) {
	        entity.setTreatmentPlan(dto.getTreatmentPlan());
	    }

	    // =========================
	    // ✅ THERAPY SESSIONS
	    // =========================
	    if (dto.getTherapySessions() != null && !dto.getTherapySessions().isEmpty()) {
	        entity.setTherapySessions(dto.getTherapySessions());
	    }

	    // =========================
	    // ✅ EXERCISE PLAN
	    // =========================
	    if (dto.getExercisePlan() != null) {
	        entity.setExercisePlan(dto.getExercisePlan());
	    }

	    // =========================
	    // ✅ FOLLOW UP
	    // =========================
	    if (dto.getFollowUp() != null) {
	        entity.setFollowUp(dto.getFollowUp());
	    }

	    return entity;
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

    List<PhysiotherapyRecord> records =
            repository.findByClinicIdAndBranchIdAndTreatmentPlanTherapistId(
                    clinicId, branchId, therapistId);

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
            if (record.getOverallStatus() == null ||
                !record.getOverallStatus().equalsIgnoreCase(statusFilter)) {
                continue;
            }
        }

        if (record.getTherapySessions() == null) continue;
        if (record.getPatientInfo() == null) continue;

        for (TherapySession session : record.getTherapySessions()) {

            if (session.getProgramId() == null && session.getProgramName() == null) {
                continue;
            }

            // 🔥 UPDATED KEY (FIXED ISSUE)
            String key = record.getTherapistRecordId() + "_" +
                         (session.getProgramId() != null ? session.getProgramId() : "NA");

            if (map.containsKey(key)) continue;

            AssignTherapistPatientListDTO dto = new AssignTherapistPatientListDTO();

            // ✅ BASIC
            dto.setBookingId(record.getBookingId());
            dto.setTherapistRecordId(record.getTherapistRecordId());
            dto.setClinicId(record.getClinicId());
            dto.setBranchId(record.getBranchId());

            // ✅ PATIENT INFO
            dto.setPatientId(record.getPatientInfo().getPatientId());
            dto.setPatientName(
                    record.getPatientInfo().getPatientName() != null
                            ? record.getPatientInfo().getPatientName()
                            : "Unknown"
            );
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
            dto.setProgramId(
                    session.getProgramId() != null ? session.getProgramId() : "N/A"
            );
            dto.setProgramName(session.getProgramName());
            dto.setSerivceType(
                    session.getServiceType() != null ? session.getServiceType() : "N/A"
            );

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
	//	@Override
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
	
	
public Response getProgramAndTherapyInfo(String clinicId, String branchId,
            String patientId, String bookingId) {
Response response = new Response();

// Step 1: Fetch PhysiotherapyRecord using existing method
Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, patientId, bookingId);

if (!fetchedResponse.isSuccess()) {
return fetchedResponse;
}

List<PhysiotherapyRecord> records = (List<PhysiotherapyRecord>) fetchedResponse.getData();

List<ProgramAndTherophyAndExcercisesInfo> resultList = new ArrayList<>();

for (PhysiotherapyRecord record : records) {

List<TherapySession> therapySessions = record.getTherapySessions();

if (therapySessions == null || therapySessions.isEmpty()) {
continue;
}

for (TherapySession session : therapySessions) {

ProgramAndTherophyAndExcercisesInfo info = new ProgramAndTherophyAndExcercisesInfo();
TreatmentPlan plan = record.getTreatmentPlan();
// Step 2: Map basic fields from record and session
//info.setId(session.getId());
info.setDoctorName(plan.getDoctorName());
info.setDoctorId(plan.getDoctorId());
info.setTherapistName(plan.getTherapistName());
info.setTherapistId(plan.getTherapistId());
info.setBookingId(record.getBookingId());
info.setTherapistRecordId(record.getTherapistRecordId());
info.setPatientId(
record.getPatientInfo() != null ? record.getPatientInfo().getPatientId() : null
);
info.setProgramId(session.getProgramId());
info.setProgramName(session.getProgramName());
info.setClinicId(record.getClinicId());
info.setBranchId(record.getBranchId());

// Step 3: Build TherophyDataDto list with all calculations
List<TherapyData> therapyDataList = session.getTherapyData();

// Program-level accumulators
int programCostTotal        = 0;
int programSessionCountTotal = 0;
int therapyCount            = 0;

List<TherophyDataDto> therophyDataDtos = new ArrayList<>();

if (therapyDataList != null && !therapyDataList.isEmpty()) {

for (TherapyData therapyData : therapyDataList) {

TherophyDataDto therapyDto = new TherophyDataDto();
therapyDto.setTherapyId(therapyData.getTherapyId());
therapyDto.setTherapyName(therapyData.getTherapyName());

// Therapy-level accumulators
int therapySessionCountTotal = 0; // → noOfSessionCount per therapy
int exerciseIdCount          = 0; // → noExerciseIdCount per therapy
int therapyCostTotal         = 0; // → therapyCost per therapy

List<Exercise> exerciseDtos = new ArrayList<>();

List<TherapyExercise> exercises = therapyData.getExercises();

if (exercises != null && !exercises.isEmpty()) {

for (TherapyExercise exercise : exercises) {

Exercise exerciseDTO = new Exercise();

// Map fields from TherapyExercise → ExcerciseDTO
exerciseDTO.setExerciseId(exercise.getTherapyExercisesId());
exerciseDTO.setExerciseName(exercise.getName());
exerciseDTO.setSets(exercise.getSets());
exerciseDTO.setRepetitions(exercise.getRepetitions());
exerciseDTO.setNotes(exercise.getNotes());
exerciseDTO.setVideoUrl(exercise.getVideoUrl());

// Parse frequency → frequancy field
Integer frequencyVal = null;
if (exercise.getFrequency() != null && !exercise.getFrequency().isBlank()) {
  try {
      frequencyVal = Integer.parseInt(exercise.getFrequency().trim());
  } catch (NumberFormatException e) {
      frequencyVal = 0;
  }
}
exerciseDTO.setFrequancy(frequencyVal);

// Parse noOfSessions from session field
Integer noOfSessions = null;
if (exercise.getSession() != null && !exercise.getSession().isBlank()) {
  try {
      noOfSessions = Integer.parseInt(exercise.getSession().trim());
  } catch (NumberFormatException e) {
      noOfSessions = 0;
  }
}
exerciseDTO.setNoOfSessions(noOfSessions);

// Parse pricePerSession from totalPrice
Double pricePerSession = (double) exercise.getTotalPrice();
exerciseDTO.setPricePerSession(Integer.valueOf(String.valueOf(pricePerSession)));

// ✅ Calculate totalSessionCost = noOfSessions * pricePerSession
double totalSessionCost = (noOfSessions != null ? noOfSessions : 0) * pricePerSession;
exerciseDTO.setTotalSessionCost(totalSessionCost);

exerciseDtos.add(exerciseDTO);

// ✅ Therapy-level accumulation
therapySessionCountTotal += (noOfSessions != null ? noOfSessions : 0);
exerciseIdCount++;                  // count each exercise
therapyCostTotal += totalSessionCost;
}
}

// ✅ Set therapy-level calculated fields
therapyDto.setNoOfSessionCount(therapySessionCountTotal);
therapyDto.setNoExerciseIdCount(exerciseIdCount);
therapyDto.setTherapyCost(therapyCostTotal);
therapyDto.setExercises(exerciseDtos);

therophyDataDtos.add(therapyDto);

// ✅ Program-level accumulation
programCostTotal         += therapyCostTotal;
programSessionCountTotal += therapySessionCountTotal;
therapyCount++;
}
}

// ✅ Set program-level calculated fields
info.setProgramCost(programCostTotal);
info.setNoOfSessionCount(programSessionCountTotal);
info.setNoTherapyCount(therapyCount);
info.setTherophyData(therophyDataDtos);

resultList.add(info);
}
}

if (resultList.isEmpty()) {
response.setSuccess(false);
response.setMessage("No therapy session data found");
response.setStatus(404);
return response;
}

response.setSuccess(true);
response.setData(resultList);
response.setMessage("Program and therapy info fetched successfully");
response.setStatus(200);

return response;
}


@Override
public ResponseEntity<Response> getCalculations(String clinicId,
                                                String branchId,
                                                String patientId,
                                                String bookingId) {
    try {
        Response fetchedResponse = getByWithoutTherapistRecordId(
                clinicId, branchId, patientId, bookingId
        );

        if (fetchedResponse == null || fetchedResponse.getData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Response(false, null, "Record not found", 404));
        }

        // ✅ SAFE CAST HANDLING
        PhysiotherapyRecord record = extractRecord(fetchedResponse.getData());

        if (record == null || record.getTherapySessions() == null
                || record.getTherapySessions().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT)
                    .body(new Response(false, null, "No therapy sessions found", 204));
        }

        List<Object> result = new ArrayList<>();

        for (TherapySession session : record.getTherapySessions()) {

            String serviceType = session.getServiceType();
            if (serviceType == null) continue;

            switch (serviceType.toLowerCase()) {

                case "package":
                    result.add(handlePackage(record, session));
                    break;

                case "program":
                    result.add(handleProgram(record, session));
                    break;

                case "therapy":
                    result.add(handleTherapy(record, session));
                    break;

                case "exercise":
                    result.add(handleExercise(record, session));
                    break;

                default:
                    throw new RuntimeException("Invalid service type: " + serviceType);
            }
        }

        return ResponseEntity.ok(
                new Response(true, result, "Calculations fetched successfully", 200)
        );

    } catch (RuntimeException ex) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new Response(false, null, ex.getMessage(), 400));

    } catch (Exception ex) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Response(false, null, "Something went wrong", 500));
    }
} 

private PackageCalculation handlePackage(PhysiotherapyRecord record, TherapySession session) {

    PackageCalculation dto = new PackageCalculation();

    dto.setServiceType("package");
    dto.setBookingId(record.getBookingId());
    dto.setTherapistRecordId(record.getTherapistRecordId());
    dto.setClinicId(record.getClinicId());
    dto.setBranchId(record.getBranchId());
    dto.setPatientId(record.getPatientInfo().getPatientId());
    dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
    dto.setDoctorName(record.getTreatmentPlan().getDoctorName());
    dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
    dto.setTherapistName(record.getTreatmentPlan().getTherapistName());
    int totalPackageCost = 0;
    List<ProgramDataForPackage> programList = new ArrayList<>();
    dto.setPackageName(session.getPackageName());
	dto.setPackageId(session.getPackageId());
    for (Program program : session.getPrograms()) {
        ProgramDataForPackage programDTO = new ProgramDataForPackage();
        programDTO.setProgramId(program.getProgramId());
        programDTO.setProgramName(program.getProgramName());

        double programTotal = 0;
        List<TherapyinfoForPackage> therapyList = new ArrayList<>();

        for (TherapyData therapy : program.getTherapyData()) {

            TherapyinfoForPackage therapyDTO = new TherapyinfoForPackage();
            therapyDTO.setTherapyId(therapy.getTherapyId());
            therapyDTO.setTherapyName(therapy.getTherapyName());

            double therapyTotal = 0;
            List<Exercise> exercises = mapExercises(therapy.getExercises());

            for (Exercise ex : exercises) {
                double total = calculateExerciseCost(ex);
                ex.setTotalSessionCost(total);
                therapyTotal += total;
            }

            therapyDTO.setExercises(exercises);
            therapyDTO.setTotalPrice(therapyTotal);

            programTotal += therapyTotal;
            therapyList.add(therapyDTO);
        }

        programDTO.setTherapyData(therapyList);
        programDTO.setTotalPrice(programTotal);

        programList.add(programDTO);
    }

    dto.setTherapySessions(programList);
    
    for(ProgramDataForPackage t : programList) {
    	totalPackageCost += t.getTotalPrice();
    }
    dto.setTotal(totalPackageCost);

    return dto;
}

private ProgramCalculations handleProgram(PhysiotherapyRecord record, TherapySession session) {

    ProgramCalculations dto = new ProgramCalculations();

    dto.setServiceType("program");
    dto.setBookingId(record.getBookingId());
    dto.setTherapistRecordId(record.getTherapistRecordId());
    dto.setClinicId(record.getClinicId());
    dto.setBranchId(record.getBranchId());
    dto.setPatientId(record.getPatientInfo().getPatientId());

    dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
    dto.setDoctorName(record.getTreatmentPlan().getDoctorName());

    dto.setProgramId(session.getProgramId());
    dto.setProgramName(session.getProgramName());

    double programTotal = 0;

    List<TheraphyInfo> therapyList = new ArrayList<>();

    for (TherapyData therapy : session.getTherapyData()) {

        TheraphyInfo therapyDTO = new TheraphyInfo();

        therapyDTO.setTherapyId(therapy.getTherapyId());
        therapyDTO.setTherapyName(therapy.getTherapyName());

        double therapyTotal = 0;
        List<Exercise> exercises = mapExercises(therapy.getExercises());

        for (Exercise ex : exercises) {
            double total = calculateExerciseCost(ex);
            ex.setTotalSessionCost(total);
            therapyTotal += total;
        }

        therapyDTO.setExercises(exercises);
        therapyDTO.setTotalPrice(therapyTotal);

        programTotal += therapyTotal;
        therapyList.add(therapyDTO);
    }

    dto.setTherapyData(therapyList);
    dto.setTotalPrice((int) programTotal);

    return dto;
}

private TherapyCalculations handleTherapy(PhysiotherapyRecord record, TherapySession session) {

    TherapyCalculations dto = new TherapyCalculations();

    dto.setServiceType("therapy");
    dto.setBookingId(record.getBookingId());
    dto.setTherapistRecordId(record.getTherapistRecordId());
    dto.setClinicId(record.getClinicId());
    dto.setBranchId(record.getBranchId());
    dto.setPatientId(record.getPatientInfo().getPatientId());

    dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
    dto.setDoctorName(record.getTreatmentPlan().getDoctorName());

    dto.setTherapyId(session.getTherapyId());
    dto.setTherapyName(session.getTherapyName());

    List<Exercise> exercises = mapExercises(session.getExercises());

    double total = 0;

    for (Exercise ex : exercises) {
        double cost = calculateExerciseCost(ex);
        ex.setTotalSessionCost(cost);
        total += cost;
    }

    dto.setExercises(exercises);
    dto.setTotalPrice((int) total);

    return dto;
}

private ExerciseCalculations handleExercise(PhysiotherapyRecord record, TherapySession session) {

    ExerciseCalculations dto = new ExerciseCalculations();

    dto.setServiceType("exercise");
    dto.setBookingId(record.getBookingId());
    dto.setTherapistRecordId(record.getTherapistRecordId());
    dto.setClinicId(record.getClinicId());
    dto.setBranchId(record.getBranchId());
    dto.setPatientId(record.getPatientInfo().getPatientId());

    dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
    dto.setDoctorName(record.getTreatmentPlan().getDoctorName());

    List<Exercise> exercises = mapExercises(session.getExercises());

    double total = 0;

    for (Exercise ex : exercises) {
        double cost = calculateExerciseCost(ex);
        ex.setTotalSessionCost(cost);
        total += cost;
    }

    dto.setExercises(exercises);
    dto.setTotalPrice((int) total);

    return dto;
}

private List<Exercise> mapExercises(List<TherapyExercise> source) {

    if (source == null) return new ArrayList<>();

    return source.stream().map(te -> {
        Exercise ex = new Exercise();

        ex.setExerciseId(te.getTherapyExercisesId());
        ex.setExerciseName(te.getName());
        ex.setSets(te.getSets());
        ex.setRepetitions(te.getRepetitions());
        ex.setNotes(te.getNotes());
        ex.setVideoUrl(te.getVideoUrl());

        // Convert session & frequency safely
        ex.setNoOfSessions(parseInteger(te.getSession()));
        ex.setFrequancy(parseInteger(te.getFrequency()));

        ex.setPricePerSession(te.getTotalPrice() != null ? te.getTotalPrice().intValue() : 0);

        return ex;
    }).toList();
}

private double calculateExerciseCost(Exercise ex) {

    int sessions = ex.getNoOfSessions() != null ? ex.getNoOfSessions() : 0;
    int price = ex.getPricePerSession() != null ? ex.getPricePerSession() : 0;

    return sessions * price;
}

private Integer parseInteger(String value) {
    try {
        return value != null ? Integer.parseInt(value) : 0;
    } catch (Exception e) {
        return 0;
    }
}

@SuppressWarnings("unchecked")
private PhysiotherapyRecord extractRecord(Object data) {

    if (data instanceof PhysiotherapyRecord) {
        return (PhysiotherapyRecord) data;
    }

    if (data instanceof List<?>) {
        List<?> list = (List<?>) data;

        if (!list.isEmpty() && list.get(0) instanceof PhysiotherapyRecord) {
            return (PhysiotherapyRecord) list.get(0);
        }
    }

    throw new RuntimeException("Invalid data format: expected PhysiotherapyRecord");
}
}


