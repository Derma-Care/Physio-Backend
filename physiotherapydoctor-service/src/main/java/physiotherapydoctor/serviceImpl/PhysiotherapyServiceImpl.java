package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.AssignTherapistPatientListDTO;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.Complaints;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.DoctorsDTO;
import physiotherapydoctor.dto.Exercise;
import physiotherapydoctor.dto.ExerciseCalculations;
import physiotherapydoctor.dto.Investigation;
import physiotherapydoctor.dto.PackageCalculation;
import physiotherapydoctor.dto.PatientHistoryResponse;
import physiotherapydoctor.dto.PhysiotherapyRecordDTO;
import physiotherapydoctor.dto.Program;
import physiotherapydoctor.dto.ProgramAndTherophyAndExcercisesInfo;
import physiotherapydoctor.dto.ProgramCalculations;
import physiotherapydoctor.dto.ProgramDataForPackage;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.dto.SessionForBooking;
import physiotherapydoctor.dto.TheraphyInfo;
import physiotherapydoctor.dto.TherapistRecordDetails;
import physiotherapydoctor.dto.TherapyCalculations;
import physiotherapydoctor.dto.TherapyData;
import physiotherapydoctor.dto.TherapyExercise;
import physiotherapydoctor.dto.TherapySession;
import physiotherapydoctor.dto.TherapyWithSessions;
import physiotherapydoctor.dto.TherapyinfoForPackage;
import physiotherapydoctor.dto.TherophyDataDto;
import physiotherapydoctor.dto.TreatmentPlan;
import physiotherapydoctor.dto.VisitDetailsDTO;
import physiotherapydoctor.dto.VisitDetailsDTO.PhysiotherapyDoctorData;
import physiotherapydoctor.entity.PaymentRecord;
import physiotherapydoctor.entity.PhysiotherapyRecord;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.repository.PhysiotherapydoctorRespository;
import physiotherapydoctor.service.PhysiotherapyService;
import physiotherapydoctor.service.S3Service;
import physiotherapydoctor.util.ExtractFeignMessage;

@Service
@RequiredArgsConstructor
public class PhysiotherapyServiceImpl implements PhysiotherapyService {

	private final PhysiotherapydoctorRespository repository;

	@Autowired
	private BookingFeignClient bookingFeign;

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private S3Service s3Service;

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

	    LocalDateTime now = LocalDateTime.now();

	    String createdDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	    String createdTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

	    entity.setCreatedAt(createdDate);
	    entity.setCreatedTime(createdTime);
	    entity.setUpdatedAt(createdDate);

	    boolean allowBookingUpdate = true;

	    if (dto.getBookingId() != null && !dto.getBookingId().isEmpty()) {

	        try {

	            ResponseEntity<ResponseStructure<BookingResponse>> bookingRes =
	                    bookingFeign.getBookedService(dto.getBookingId());

	            if (bookingRes != null
	                    && bookingRes.getBody() != null
	                    && bookingRes.getBody().getData() != null) {

	                String bookingStatus = bookingRes.getBody().getData().getStatus();

	                if ("Due for Investigation".equalsIgnoreCase(bookingStatus)
	                        || "Investigation Done".equalsIgnoreCase(bookingStatus)) {

	                    entity.setUptoInvestigation(true);
	                    allowBookingUpdate = false;
	                }
	            }

	        } catch (Exception e) {
	            System.out.println("Error while fetching booking status : " + e.getMessage());
	        }
	    }

	    Integer updatedFreeLeft = null;

	    if (dto.getBookingId() != null
	            && !dto.getBookingId().isEmpty()
	            && dto.getPatientInfo() != null
	            && dto.getPatientInfo().getPatientId() != null) {

	        try {

	            ResponseEntity<ResponseStructure<BookingResponse>> bookingResponse =
	                    bookingFeign.getBookedService(dto.getBookingId());

	            if (bookingResponse != null && bookingResponse.getBody().getData() != null) {

	                Integer freeLeft = bookingResponse.getBody().getData().getFreeFollowUpsLeft();

	                // Count BEFORE save
	                long visitCount = repository.countByBookingIdAndPatientInfoPatientId(
	                        dto.getBookingId(),
	                        dto.getPatientInfo().getPatientId());

	                System.out.println("Current Visit Count : " + visitCount);
	                System.out.println("Current Free FollowUps : " + freeLeft);

	                // First visit -> don't decrease
	                if (visitCount == 0) {
	                    updatedFreeLeft = freeLeft;
	                }
	                // Follow-up visits -> decrease
	                else if (freeLeft != null && freeLeft > 0) {
	                    updatedFreeLeft = freeLeft - 1;
	                } else {
	                    updatedFreeLeft = freeLeft;
	                }

	                System.out.println("Updated Free FollowUps : " + updatedFreeLeft);
	            }

	        } catch (Exception e) {
	            System.out.println("Booking fetch failed: " + e.getMessage());
	        }
	    }

	    // Save record AFTER calculating visit count
	    PhysiotherapyRecord saved = repository.save(entity);

	    // Update Clinic Admin Booking
	    if (dto.getBookingId() != null
	            && !dto.getBookingId().isEmpty()
	            && allowBookingUpdate) {

	        try {

	            BookingResponse updateRequest = new BookingResponse();
	            updateRequest.setBookingId(dto.getBookingId());
	            updateRequest.setStatus("in-progress");
	            updateRequest.setFreeFollowUpsLeft(updatedFreeLeft);

	            System.out.println(
	                    "Updating Clinic Admin Booking. Free FollowUps Left : "
	                            + updatedFreeLeft);

	            clinicAdminFeign.updateAppointment(updateRequest);

	        } catch (Exception e) {
	            System.out.println("Clinic Admin Booking update failed : " + e.getMessage());
	        }
	    }

	    // Update Booking Service
	    if (dto.getBookingId() != null
	            && !dto.getBookingId().isEmpty()
	            && allowBookingUpdate) {

	        try {

	            BookingResponse updateRequest = new BookingResponse();
	            updateRequest.setBookingId(dto.getBookingId());
	            updateRequest.setStatus("Active");
	            updateRequest.setFreeFollowUpsLeft(updatedFreeLeft);

	            System.out.println(
	                    "Updating Booking Service. Free FollowUps Left : "
	                            + updatedFreeLeft);

	            bookingFeign.updateAppointment(updateRequest);

	        } catch (Exception e) {
	            System.out.println("Booking Service update failed : " + e.getMessage());
	        }
	    }

	    List<Map<String, Object>> cleanSessions =
	            transformTherapySessions(saved.getTherapySessions());

	    saved.setTherapySessions((List) cleanSessions);

	    if (saved.getPrescriptionPdf() != null
	            && !saved.getPrescriptionPdf().isEmpty()) {

	        try {
	            String presignedUrl =
	                    s3Service.generateSignedUrl(saved.getPrescriptionPdf());

	            saved.setPrescriptionPdf(presignedUrl);

	        } catch (Exception e) {
	            System.out.println(
	                    "Presigned URL generation failed : " + e.getMessage());
	        }
	    }

	    response.setSuccess(true);
	    response.setData(saved);
	    response.setMessage("Record created successfully");
	    response.setStatus(201);

	    return response;
	}

	private List<Map<String, Object>> transformTherapySessions(List<TherapySession> sessions) {

		if (sessions == null)
			return null;

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

		if (sessions == null)
			return;

		for (TherapySession session : sessions) {

			// ================= PACKAGE =================
			if (session.getPrograms() != null) {

				double packageTotal = 0;

				for (Program p : session.getPrograms()) {

					double programTotal = 0;

					if (p.getTherapyData() != null) {

						for (TherapyData t : p.getTherapyData()) {

							double therapyTotal = 0;

							if (t.getExercises() != null) {
								for (TherapyExercise ex : t.getExercises()) {
									double exTotal = 0;
									if (ex.getTotalExercisePrice() != null) {
										exTotal = ex.getTotalExercisePrice();
									} else if (ex.getPricePerSession() != null && ex.getNoOfSessions() != null) {
										exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
									}
									ex.setTotalExercisePrice(exTotal);
									therapyTotal += exTotal;
								}
							}

							t.setTotalTherapyPrice(therapyTotal);
							programTotal += therapyTotal;
						}
					}

					p.setTotalProgramPrice(programTotal);
					packageTotal += programTotal;
				}

				// ✅ Set package total
				session.setTotalPackageCost(packageTotal);
				session.setTotalPrice(packageTotal);
			}

			// ================= PROGRAM =================
			if (session.getTherapyData() != null) {

				double programTotal = 0;

				for (TherapyData t : session.getTherapyData()) {

					double therapyTotal = 0;

					if (t.getExercises() != null) {
						for (TherapyExercise ex : t.getExercises()) {
							double exTotal = 0;
							if (ex.getTotalExercisePrice() != null) {
								exTotal = ex.getTotalExercisePrice();
							} else if (ex.getPricePerSession() != null && ex.getNoOfSessions() != null) {
								exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
							}
							ex.setTotalExercisePrice(exTotal);
							therapyTotal += exTotal;
						}
					}

					t.setTotalTherapyPrice(therapyTotal);
					programTotal += therapyTotal;
				}

				// ✅ Set program total
				session.setTotalProgramCost(programTotal);
				session.setTotalPrice(programTotal);
			}

			// ================= THERAPY =================
			if (session.getExercises() != null && session.getPrograms() == null && session.getTherapyData() == null) {

				double therapyTotal = 0;

				for (TherapyExercise ex : session.getExercises()) {
					double exTotal = 0;
					if (ex.getTotalExercisePrice() != null) {
						exTotal = ex.getTotalExercisePrice();
					} else if (ex.getPricePerSession() != null && ex.getNoOfSessions() != null) {
						exTotal = ex.getPricePerSession() * ex.getNoOfSessions();
					}
					ex.setTotalExercisePrice(exTotal);
					therapyTotal += exTotal;
				}

				// ✅ Set therapy total
				session.setTotalTherapyCost(therapyTotal);
				session.setTotalPrice(therapyTotal);
			}
		}
	}

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

		PhysiotherapyRecord record = optional.get();
		if (record.getPrescriptionPdf() != null && !record.getPrescriptionPdf().isBlank()) {
			record.setPrescriptionPdf(s3Service.generateSignedUrl(record.getPrescriptionPdf()));
		}

		response.setSuccess(true);
		response.setData(record);
		response.setMessage("Success");
		response.setStatus(200);

		return response;
	}

	@Override
	public String getByBookingId(String id) {

		try {
			// Response response = new Response();

			Optional<PhysiotherapyRecord> optional = repository.findByBookingId(id);

			if (optional.isEmpty()) {

				return null;
			}
			return optional.get().getPrescriptionPdf();
		} catch (Exception e) {
			/// System.out.println(e.getMessage());
			return null;
		}
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

			// // generate sessionId for new sessions
			// generateSessionIds(dto.getTherapySessions());

			existing.setTherapySessions(dto.getTherapySessions());
		}

		// ✅ HOME EXERCISE UPDATE
		if (dto.getExercisePlan() != null) {
			existing.setExercisePlan(dto.getExercisePlan());
		}

		if (dto.getPrescriptionPdf() != null) {
			existing.setPrescriptionPdf(dto.getPrescriptionPdf());
		}

		if (dto.getFollowUp() != null) {
			existing.setFollowUp(dto.getFollowUp());
		}
		if (dto.getRecoverySupport() != null) {
			existing.setRecoverySupport(dto.getRecoverySupport());
		}
		// ✅ DATE FIX (STRING FORMAT - AUTO UPDATE)
		String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		existing.setUpdatedAt(now);
		// ✅ Call booking update API here
		if (existing.getBookingId() != null && !existing.getBookingId().isEmpty()) {

			try {

				BookingResponse updateRequest = new BookingResponse();
				updateRequest.setBookingId(existing.getBookingId());
				updateRequest.setStatus("in-progress");

				clinicAdminFeign.updateAppointment(updateRequest);
				existing.setUptoInvestigation(false);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		PhysiotherapyRecord updated = repository.save(existing);

		if (updated.getPrescriptionPdf() != null && !updated.getPrescriptionPdf().isEmpty()) {

			updated.setPrescriptionPdf(s3Service.generateSignedUrl(updated.getPrescriptionPdf()));
		}

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
		// entity.setOverallStatus(dto.getOverallStatus());

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
		    Complaints complaints = dto.getComplaints();

		    // ✅ Strip signed URL → store only S3 key
		    if (complaints.getPainAssessmentImage() != null && !complaints.getPainAssessmentImage().isBlank()) {
		        complaints.setPainAssessmentImage(extractS3Key(complaints.getPainAssessmentImage()));
		    }

		    entity.setComplaints(complaints);
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
		if (dto.getRecoverySupport() != null) {
			entity.setRecoverySupport(dto.getRecoverySupport());
		}

		entity.setPrescriptionPdf(dto.getPrescriptionPdf());
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

	    PhysiotherapyRecord rec = record.get();

	    // ✅ Refresh prescriptionPdf
	    if (rec.getPrescriptionPdf() != null && !rec.getPrescriptionPdf().isBlank()) {
	        rec.setPrescriptionPdf(s3Service.generateSignedUrl(rec.getPrescriptionPdf()));
	    }

	    // ✅ Refresh painAssessmentImage — extractS3Key handles both old full URLs and plain keys in DB
	    if (rec.getComplaints() != null) {
	        String painImage = rec.getComplaints().getPainAssessmentImage();
	        if (painImage != null && !painImage.isBlank()) {
	            rec.getComplaints().setPainAssessmentImage(
	                s3Service.generateSignedUrl(extractS3Key(painImage))
	            );
	        }
	    }

	    response.setSuccess(true);
	    response.setData(rec);
	    response.setMessage("Record fetched successfully");
	    response.setStatus(200);

	    return response;
	}
	
	private String extractS3Key(String input) {
	    if (input == null || input.isBlank()) return input;
	    if (!input.startsWith("http")) return input;
	    try {
	        String path = new java.net.URI(input).getPath(); // "/part-images/abc.png"
	        return path.startsWith("/") ? path.substring(1) : path; // "part-images/abc.png"
	    } catch (Exception e) {
	        return input;
	    }
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
///System.out.println(records);
		if (records == null || records.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No records found");
			response.setStatus(404);
			return response;
		}

		records.forEach(r -> {
			if (r.getPrescriptionPdf() != null && !r.getPrescriptionPdf().isBlank()) {
				r.setPrescriptionPdf(s3Service.generateSignedUrl(r.getPrescriptionPdf()));
			}
		});

		response.setSuccess(true);
		response.setData(records);
		response.setMessage("Records fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getAssignedPatients(String clinicId, String branchId, String therapistId, Integer overallStatus) {

		Response response = new Response();

		// ✅ FETCH DATA
		List<PhysiotherapyRecord> records = repository.findByClinicIdAndBranchIdAndTreatmentPlanTherapistId(clinicId,
				branchId, therapistId);

		if (records == null || records.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No assigned patients found");
			response.setStatus(404);
			return response;
		}

		// ✅ KEEP ONLY LATEST DATA (same bookingId -> last record only)
		Map<String, PhysiotherapyRecord> latestRecordsMap = new LinkedHashMap<>();

		for (PhysiotherapyRecord record : records) {
			latestRecordsMap.put(record.getBookingId(), record);
		}

		records = new ArrayList<>(latestRecordsMap.values());

		// ✅ STATUS MAP
		Map<Integer, String> statusMap = Map.of(1, "pending", 2, "in-progress", 3, "completed");

		String expectedStatus = statusMap.get(overallStatus);

		Map<String, AssignTherapistPatientListDTO> map = new LinkedHashMap<>();

		for (PhysiotherapyRecord record : records) {

			// ✅ GET STATUS FROM PAYMENT TABLE
			String dbStatus = null;

			Optional<PaymentRecord> paymentOpt = paymentRepository.findByBookingId(record.getBookingId());

			if (paymentOpt.isPresent()) {
				dbStatus = paymentOpt.get().getOverallStatus();
			}

			// ✅ STATUS FILTER
			if (expectedStatus != null) {

				if (dbStatus == null)
					continue;

				String status = dbStatus.trim().toLowerCase().replace("_", "-");

				// ✅ ACTIVE = IN-PROGRESS
				if (status.equals("active")) {
					status = "in-progress";
				}

				if (!status.equals(expectedStatus))
					continue;
			}

			// ✅ SKIP ONLY IF NO SESSIONS
			if (record.getTherapySessions() == null || record.getTherapySessions().isEmpty())
				continue;

			if (record.getPatientInfo() == null)
				continue;

			for (TherapySession session : record.getTherapySessions()) {

				// ✅ UNIQUE KEY (SAFE)
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
				dto.setProgramName(session.getProgramName() != null ? session.getProgramName() : "N/A");
				dto.setSerivceType(session.getServiceType() != null ? session.getServiceType() : "N/A");

				// ✅ STATUS FROM PAYMENT
				dto.setOverallStatus(dbStatus);

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

	// ===================== GET SESSIONS BY DATE =====================

	public Response getProgramAndTherapyInfo(String clinicId, String branchId, String patientId, String bookingId) {
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
				info.setPatientId(record.getPatientInfo() != null ? record.getPatientInfo().getPatientId() : null);
				info.setProgramId(session.getProgramId());
				info.setProgramName(session.getProgramName());
				info.setClinicId(record.getClinicId());
				info.setBranchId(record.getBranchId());

// Step 3: Build TherophyDataDto list with all calculations
				List<TherapyData> therapyDataList = session.getTherapyData();

// Program-level accumulators
				int programCostTotal = 0;
				int programSessionCountTotal = 0;
				int therapyCount = 0;

				List<TherophyDataDto> therophyDataDtos = new ArrayList<>();

				if (therapyDataList != null && !therapyDataList.isEmpty()) {

					for (TherapyData therapyData : therapyDataList) {

						TherophyDataDto therapyDto = new TherophyDataDto();
						therapyDto.setTherapyId(therapyData.getTherapyId());
						therapyDto.setTherapyName(therapyData.getTherapyName());

// Therapy-level accumulators
						int therapySessionCountTotal = 0; // → noOfSessionCount per therapy
						int exerciseIdCount = 0; // → noExerciseIdCount per therapy
						int therapyCostTotal = 0; // → therapyCost per therapy

						List<Exercise> exerciseDtos = new ArrayList<>();

						List<TherapyExercise> exercises = therapyData.getExercises();

						if (exercises != null && !exercises.isEmpty()) {

							for (TherapyExercise exercise : exercises) {

								Exercise exerciseDTO = new Exercise();

// Map fields from TherapyExercise → ExcerciseDTO
								exerciseDTO.setExerciseId(exercise.getExerciseId());
								exerciseDTO.setExerciseName(exercise.getExerciseName());
								exerciseDTO.setSets(exercise.getSets());
								exerciseDTO.setRepetitions(exercise.getRepetitions());
								exerciseDTO.setNotes(exercise.getNotes());
								exerciseDTO.setYoutubeUrl(exercise.getYoutubeUrl());

// Parse frequency → frequancy field
								String frequencyVal = null;
								if (exercise.getFrequency() != null && !exercise.getFrequency().isBlank()) {
									try {
										frequencyVal = exercise.getFrequency();
									} catch (NumberFormatException e) {
										frequencyVal = null;
									}
								}
								exerciseDTO.setFrequency(frequencyVal);

// Parse noOfSessions from session field
								Integer noOfSessions = null;
								if (exercise.getNoOfSessions() != null) {
									try {
										noOfSessions = exercise.getNoOfSessions();
									} catch (NumberFormatException e) {
										noOfSessions = 0;
									}
								}
								exerciseDTO.setNoOfSessions(noOfSessions);

// Parse pricePerSession from totalPrice
								Double pricePerSession = (double) exercise.getPricePerSession();
								exerciseDTO.setPricePerSession(noOfSessions);

// ✅ Calculate totalSessionCost = noOfSessions * pricePerSession
								double totalSessionCost = (noOfSessions != null ? noOfSessions : 0) * pricePerSession;
								exerciseDTO.setTotalSessionCost(totalSessionCost);

								exerciseDtos.add(exerciseDTO);

// ✅ Therapy-level accumulation
								therapySessionCountTotal += (noOfSessions != null ? noOfSessions : 0);
								exerciseIdCount++; // count each exercise
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
						programCostTotal += therapyCostTotal;
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
	public ResponseEntity<Response> getCalculations(String clinicId, String branchId, String patientId,
			String bookingId) {
		try {
			Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, patientId, bookingId);

			if (fetchedResponse == null || fetchedResponse.getData() == null) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(false, null, "Record not found", 200));
			}

			List<PhysiotherapyRecord> records = extractRecords(fetchedResponse.getData());

			if (records == null || records.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(new Response(false, null, "No records found", 200));
			}

			List<Object> result = new ArrayList<>();

			for (PhysiotherapyRecord record : records) {

				if (record.getTherapySessions() == null || record.getTherapySessions().isEmpty()) {
					continue;
				}

				for (TherapySession session : record.getTherapySessions()) {

					String serviceType = session.getServiceType();

					if (serviceType == null || serviceType.isBlank()) {
						continue;
					}

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
			}

			if (result.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(new Response(false, null, "No calculations available", 204));
			}

			return ResponseEntity.ok(new Response(true, result, "Calculations fetched successfully", 200));

		} catch (IllegalArgumentException ex) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, null, ex.getMessage(), 400));

		} catch (RuntimeException ex) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Response(false, null, ex.getMessage(), 400));

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

		for (ProgramDataForPackage t : programList) {
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
		dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
		dto.setTherapistName(record.getTreatmentPlan().getTherapistName());
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
		dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
		dto.setTherapistName(record.getTreatmentPlan().getTherapistName());
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
		dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
		dto.setTherapistName(record.getTreatmentPlan().getTherapistName());
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

//private Integer parseInteger(String value) {
//    try {
//        return value != null ? Integer.parseInt(value) : 0;
//    } catch (Exception e) {
//        return 0;
//    }
//}

	private List<PhysiotherapyRecord> extractRecords(Object data) {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		try {
			return objectMapper.convertValue(data, new TypeReference<List<PhysiotherapyRecord>>() {
			});
		} catch (Exception e) {
			throw new RuntimeException("Unable to convert data to List<PhysiotherapyRecord>", e);
		}
	}

	@Override
	public Response getByClinicBranchAndBooking(String clinicId, String branchId, String bookingId) {

		Response response = new Response();

		// ✅ Validation
		if (clinicId == null || clinicId.isEmpty() || branchId == null || branchId.isEmpty() || bookingId == null
				|| bookingId.isEmpty()) {

			response.setSuccess(false);
			response.setData(null);
			response.setMessage("clinicId, branchId and bookingId are required");
			response.setStatus(400);
			return response;
		}

		// ✅ Fetch from DB
		List<PhysiotherapyRecord> record = repository.findByClinicIdAndBranchIdAndBookingId(clinicId, branchId,
				bookingId);

		if (record == null || record.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("No record found");
			response.setStatus(404);
			return response;
		}

		record.forEach(r -> {
			if (r.getPrescriptionPdf() != null && !r.getPrescriptionPdf().isBlank()) {
				r.setPrescriptionPdf(s3Service.generateSignedUrl(r.getPrescriptionPdf()));
			}
		});

		response.setSuccess(true);
		response.setData(record);
		response.setMessage("Records fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getPatientHistory(String patientId) {

		Response response = new Response();

		try {

			List<PhysiotherapyRecord> records = repository.findByPatientInfoPatientId(patientId);

			if (records == null || records.isEmpty()) {
				response.setSuccess(false);
				response.setStatus(404);
				response.setMessage("No records found");
				return response;
			}

			List<PatientHistoryResponse> result = new ArrayList<>();

			for (PhysiotherapyRecord record : records) {

				PatientHistoryResponse dto = new PatientHistoryResponse();

				dto.setBookingId(record.getBookingId());

				if (record.getPatientInfo() != null) {
					dto.setPatientId(record.getPatientInfo().getPatientId());
					dto.setPatientName(record.getPatientInfo().getPatientName());
				}

				if (record.getTreatmentPlan() != null) {
					dto.setDoctorId(record.getTreatmentPlan().getDoctorId());
					dto.setDoctorName(record.getTreatmentPlan().getDoctorName());
					dto.setTherapistId(record.getTreatmentPlan().getTherapistId());
					dto.setTherapistName(record.getTreatmentPlan().getTherapistName());
				}

				dto.setBookingDate(record.getCreatedAt());
				dto.setBookingTime(record.getCreatedTime());

				List<TherapistRecordDetails> detailsList = new ArrayList<>();

				if (record.getTherapySessions() != null) {

					for (TherapySession session : record.getTherapySessions()) {

						TherapistRecordDetails details = new TherapistRecordDetails();

						details.setTherapistRecordId(record.getTherapistRecordId());

						details.setServiceType(session.getServiceType());

						if (session.getPackageId() != null) {
							details.setPackageId(session.getPackageId());
							details.setPackageName(session.getPackageName());
						}

						if (session.getProgramId() != null) {
							details.setProgramId(session.getProgramId());
							details.setProgramName(session.getProgramName());
						}

						if (session.getTherapyId() != null) {
							details.setTherapyId(session.getTherapyId());
							details.setTherapyName(session.getTherapyName());
						}

						if (session.getExercises() != null && !session.getExercises().isEmpty()) {

							TherapyExercise ex = session.getExercises().get(0);

							details.setExerciseId(ex.getExerciseId());
							details.setExerciseName(ex.getExerciseName());
						}

						detailsList.add(details);
					}
				}

				dto.setTherapistRecordId(detailsList);
				result.add(dto);
			}

			response.setSuccess(true);
			response.setStatus(200);
			response.setMessage("Patient history fetched successfully");
			response.setData(result);

		} catch (Exception e) {

			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage("Error : " + e.getMessage());
		}

		return response;
	}

	@Override
	public ResponseEntity<List<SessionForBooking>> getSessionsByBookingIdAndDate(String bookingId, String date) {

		try {
			Optional<PaymentRecord> optional = paymentRepository.findByBookingId(bookingId);

			if (optional.isEmpty()) {
				return ResponseEntity.ok(null);
			}

			PaymentRecord record = optional.get();
			// System.out.println(record);
			List<SessionForBooking> matchedSessions = new ArrayList<>();

			if (record.getTherapyWithSessions() == null) {
				return ResponseEntity.ok(null);
			}

			for (TherapyWithSessions therapy : record.getTherapyWithSessions()) {
				handlePrograms(therapy.getPrograms(), date, matchedSessions);
			}

			return matchedSessions.isEmpty() ? ResponseEntity.ok(null) : ResponseEntity.ok(matchedSessions);

		} catch (Exception e) {
			// System.out.println(e.getMessage());
			return ResponseEntity.status(500).body(null);
		}
	}

	private void handlePrograms(List<Program> programs, String date, List<SessionForBooking> result) {

		if (programs == null)
			return;

		for (Program program : programs) {
			handleTherapyData(program.getTherapyData(), date, result);
		}
	}

	private void handleTherapyData(List<TherapyData> therapyDataList, String date, List<SessionForBooking> result) {

		if (therapyDataList == null)
			return;

		for (TherapyData td : therapyDataList) {
			handleExercises(td.getExercises(), date, result);
		}
	}

	private void handleExercises(List<TherapyExercise> exercises, String date, List<SessionForBooking> result) {

		if (exercises == null)
			return;

		for (TherapyExercise ex : exercises) {

			if (ex.getSessions() == null)
				continue;

			for (Session session : ex.getSessions()) {

				if (date.equals(session.getDate())) {

					SessionForBooking bookingSession = new SessionForBooking();

					bookingSession.setSessionId(session.getSessionId());
					bookingSession.setSessionNo(session.getSessionNo());
					bookingSession.setDate(session.getDate());
					bookingSession.setStatus(session.getStatus());
					bookingSession.setPaymentStatus(session.getPaymentStatus());

					// from parent exercise
					bookingSession.setExerciseId(ex.getExerciseId());
					bookingSession.setExerciseName(ex.getExerciseName());

					result.add(bookingSession);
				}
			}
		}
	}

	@Override
	public ResponseEntity<?> getInProgressBookingsByIds(String patientId, String bookingId) {
		Response response = new Response();
		try {
			return bookingFeign.getInProgressAppointmentByPatientIdAndBookingId(patientId, bookingId);
		} catch (FeignException e) {
			response.setStatus(e.status());
			response.setMessage(e.getMessage());
			response.setSuccess(false);
			return ResponseEntity.status(response.getStatus()).body(response);
		}
	}

	@Override
	public Response getVisitHistory(String patientId, String bookingId) {

		Response response = new Response();

		try {

			List<PhysiotherapyRecord> records = repository.findByPatientInfoPatientIdAndBookingId(patientId, bookingId);

			if (records == null || records.isEmpty()) {
				response.setSuccess(true);
				response.setData(null);
				response.setMessage("No visit history found");
				response.setStatus(200);
				return response;
			}

			ObjectMapper mapper = new ObjectMapper();

			mapper.setDefaultPropertyInclusion(
					JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

			List<Map<String, Object>> result = new ArrayList<>();

			for (int i = 0; i < records.size(); i++) {

				PhysiotherapyRecord record = records.get(i);

				// ✅ Sign prescriptionPdf before converting to map
				if (record.getPrescriptionPdf() != null && !record.getPrescriptionPdf().isBlank()) {
					try {
						record.setPrescriptionPdf(s3Service.generateSignedUrl(record.getPrescriptionPdf()));
					} catch (Exception e) {
						System.out.println("prescriptionPdf sign error: " + e.getMessage());
					}
				}
				Map<String, Object> map = new LinkedHashMap<>();
				map.put("visitNumber", "Visit " + (i + 1));
				map.put("visitDate", record.getCreatedAt());
				map.put("visitTime", record.getCreatedTime());

				map.put("physiotherapyDoctorData",
						mapper.convertValue(record, new TypeReference<Map<String, Object>>() {
						}));

				result.add(map);
			}

			response.setSuccess(true);
			response.setData(result);
			response.setMessage("Visit history fetched successfully");
			response.setStatus(200);

			return response;

		} catch (Exception e) {

			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Something went wrong");
			response.setStatus(500);
			return response;
		}
	}

	public Response getFirstVisitHistory(String doctorId, String patientId, String bookingId, String clinicId,
			String branchId) {

		Response response = new Response();

		try {

			List<PhysiotherapyRecord> records = repository
					.findByTreatmentPlanDoctorIdAndPatientInfoPatientIdAndBookingIdAndClinicIdAndBranchId(doctorId,
							patientId, bookingId, clinicId, branchId);

			if (records == null || records.isEmpty()) {

				response.setSuccess(true);
				response.setData(null);
				response.setMessage("No visit history found");
				response.setStatus(200);

				return response;
			}

			ObjectMapper mapper = new ObjectMapper();

			mapper.registerModule(new JavaTimeModule());

			mapper.setDefaultPropertyInclusion(
					JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

			List<Map<String, Object>> visitHistory = new ArrayList<>();

			for (int i = 0; i < records.size(); i++) {

				PhysiotherapyRecord record = records.get(i);
				// ✅ Sign prescriptionPdf before converting to map
				if (record.getPrescriptionPdf() != null && !record.getPrescriptionPdf().isBlank()) {
					try {
						record.setPrescriptionPdf(s3Service.generateSignedUrl(record.getPrescriptionPdf()));
					} catch (Exception e) {
						System.out.println("prescriptionPdf sign error: " + e.getMessage());
					}
				}
				if (record.getTreatmentPlan() == null) {
					continue;
				}
				if (record.getTreatmentPlan() != null) {
					Map<String, Object> result = new LinkedHashMap<>();

					result.put("visitNumber", "Visit " + (i + 1));
					result.put("visitDate", record.getCreatedAt());
					result.put("visitTime", record.getCreatedTime());

					result.put("physiotherapyDoctorData",
							mapper.convertValue(record, new TypeReference<Map<String, Object>>() {
							}));
					visitHistory.add(result);
					break;
				}
			}
			response.setSuccess(true);
			response.setData(visitHistory);
			response.setMessage("Visit history fetched successfully");
			response.setStatus(200);
		} catch (Exception e) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Something went wrong");
			response.setStatus(500);
		}
		return response;
	}

	public Response getVisitHistoryByDoctor(String doctorId, String patientId, String bookingId) {

		Response response = new Response();

		try {

			List<PhysiotherapyRecord> records = repository
					.findByTreatmentPlanDoctorIdAndPatientInfoPatientIdAndBookingIdOrderByCreatedAtAsc(doctorId,
							patientId, bookingId);

			if (records == null || records.isEmpty()) {

				response.setSuccess(true);
				response.setData(null);
				response.setMessage("No visit history found");
				response.setStatus(200);

				return response;
			}

			ObjectMapper mapper = new ObjectMapper();

			mapper.setDefaultPropertyInclusion(
					JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));

			List<VisitDetailsDTO> result = new ArrayList<>();

			for (int i = 0; i < records.size(); i++) {

				PhysiotherapyRecord record = records.get(i);
				// ✅ Sign prescriptionPdf before converting to map
//				if (record.getPrescriptionPdf() != null && !record.getPrescriptionPdf().isBlank()) {
//					try {
//						record.setPrescriptionPdf(s3Service.generateSignedUrl(record.getPrescriptionPdf()));
//					} catch (Exception e) {
//						System.out.println("prescriptionPdf sign error: " + e.getMessage());
//					}
//				}
				VisitDetailsDTO map = new VisitDetailsDTO();

				map.setVisitNumber(String.valueOf(i + 1));
				map.setVisitDate(record.getCreatedAt());
				map.setVisitTime(record.getCreatedTime());
				PhysiotherapyDoctorData data = mapToPhysiotherapyDoctorData(record, s3Service);
				map.setPhysiotherapyDoctorData(data);
				result.add(map);
			}

			response.setSuccess(true);
			response.setData(result);
			response.setMessage("Visit history fetched successfully");
			response.setStatus(200);

			return response;

		} catch (Exception e) {

			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Something went wrong");
			response.setStatus(500);

			return response;
		}
	}

	public static PhysiotherapyDoctorData mapToPhysiotherapyDoctorData(PhysiotherapyRecord entity,
			S3Service s3Service) {

		if (entity == null) {
			return null;
		}

		PhysiotherapyDoctorData dto = new PhysiotherapyDoctorData();

		dto.setTherapistRecordId(entity.getTherapistRecordId());
		dto.setBookingId(entity.getBookingId());
		dto.setClinicId(entity.getClinicId());
		dto.setBranchId(entity.getBranchId());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getUpdatedAt());
		dto.setPrescriptionPdf(entity.getPrescriptionPdf() != null && !entity.getPrescriptionPdf().isBlank()
				? s3Service.generateSignedUrl(entity.getPrescriptionPdf())
				: entity.getPrescriptionPdf());
		dto.setCreatedTime(entity.getCreatedTime());

		// PatientInfo Mapping
		if (entity.getPatientInfo() != null) {

			VisitDetailsDTO.PatientInfo patientDto = new VisitDetailsDTO.PatientInfo();

			patientDto.setPatientId(entity.getPatientInfo().getPatientId());
			patientDto.setPatientName(entity.getPatientInfo().getPatientName());
			patientDto.setMobileNumber(entity.getPatientInfo().getMobileNumber());
			patientDto.setAge(entity.getPatientInfo().getAge());
			patientDto.setSex(entity.getPatientInfo().getSex());

			dto.setPatientInfo(patientDto);
		}

		return dto;
	}

	@Override
	public ResponseEntity<?> getTodaysAppointments(String clinicId, String doctorId) {
		Response res = new Response();
		try {
			return bookingFeign.getTodayDoctorAppointmentsByDoctorId(clinicId, doctorId);
		} catch (FeignException ex) {
			res.setStatus(ex.status());
			res.setMessage(ExtractFeignMessage.clearMessage(ex));
			res.setSuccess(false);
			return ResponseEntity.status(ex.status()).body(res);
		}
	}

//---------------------------------------Doctor Login apis---------------------------------------
	private Response validateChangePasswordRequest(String username, ChangeDoctorPasswordDTO updateDTO) {
		if (username == null || username.isBlank()) {
			return Response.builder().success(false).status(400).message("Username must not be empty").build();
		}

		if (updateDTO == null) {
			return Response.builder().success(false).status(400).message("Request body is missing").build();
		}

		if (updateDTO.getCurrentPassword() == null || updateDTO.getCurrentPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("Current password must not be empty").build();
		}

		if (updateDTO.getNewPassword() == null || updateDTO.getNewPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("New password must not be empty").build();
		}

		if (updateDTO.getConfirmPassword() == null || updateDTO.getConfirmPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("Confirm password must not be empty").build();
		}

		if (!updateDTO.getNewPassword().equals(updateDTO.getConfirmPassword())) {
			return Response.builder().success(false).status(400)
					.message("New password and confirm password do not match").build();
		}

		if (updateDTO.getNewPassword().length() < 6) {
			return Response.builder().success(false).status(400).message("Password must be at least 6 characters")
					.build();
		}

		return null;
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public List<String> getTodayFollowUpBookingIds() {

		String todayDate = LocalDate.now().format(FORMATTER);

		List<PhysiotherapyRecord> records = repository.findByFollowUpNextVisitDate(todayDate);
		// System.out.println(records);
		if (!records.isEmpty()) {
			return records.stream().map(PhysiotherapyRecord::getBookingId).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO) {
		Response validationResponse = validateChangePasswordRequest(username, updateDTO);
		if (validationResponse != null) {
			return validationResponse;
		}

		try {

			return clinicAdminFeign.changePassword(username, updateDTO);

		} catch (Exception ex) {

			return Response.builder().success(false).status(500).message("Failed to change password ").build();
		}
	}

	@Override
	public Response login(DoctorLoginDTO loginDTO) {
		try {
			// Call Clinic Admin Service
			return clinicAdminFeign.login(loginDTO);

		} catch (FeignException fe) {

			// If downstream service returned a proper JSON response,
			// deserialize and return it directly.
			try {
				String errorJson = fe.contentUTF8();

				if (errorJson != null && !errorJson.trim().isEmpty()) {
					return objectMapper.readValue(errorJson, Response.class);
				}

			} catch (Exception e) {
				// Ignore parsing errors and handle with custom response below
			}

			// Build a clean user-friendly response
			Response response = new Response();
			response.setSuccess(false);
			response.setData(null);
			response.setStatus(fe.status());

			switch (fe.status()) {
			case 400:
				response.setMessage("Invalid request.");
				break;

			case 401:
				response.setMessage("Invalid username or password.");
				break;

			case 403:
				response.setMessage("Access denied.");
				break;

			case 404:
				response.setMessage("Doctor account not found.");
				break;

			case 500:
				response.setMessage("Clinic Admin service encountered an internal error.");
				break;

			default:
				response.setMessage("Unable to process login request. Please try again.");
				break;
			}

			return response;

		} catch (Exception e) {
			Response response = new Response();
			response.setSuccess(false);
			response.setData(null);
			response.setStatus(500);
			response.setMessage("An unexpected error occurred while processing login.");
			return response;
		}
	}

	@Override
	public Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO) {
		if (doctorId == null || doctorId.isBlank()) {
			return Response.builder().success(false).status(400).message("Doctor ID must not be empty").build();
		} else {
			DoctorsDTO dto = new DoctorsDTO();
			dto.setDoctorAvailabilityStatus(availabilityDTO.getDoctorAvailabilityStatus());
			ResponseEntity<Response> res = clinicAdminFeign.updateDoctorById(doctorId, dto);
			int status = res.getBody().getStatus();
			if (status == 200) {
				return Response.builder().success(true).status(200).message("Doctor status updated").build();

			} else {
				return Response.builder().success(false).status(200).message("Doctor not found").build();

			}
		}
	}

	private List<Exercise> mapExercises(List<TherapyExercise> source) {

		if (source == null)
			return new ArrayList<>();

		return source.stream().map(te -> {
			Exercise ex = new Exercise();

			// ✅ Basic Info
			ex.setExerciseId(te.getExerciseId());
			ex.setExerciseName(te.getExerciseName());

			// ✅ Session & Frequency
			ex.setNoOfSessions(te.getNoOfSessions());
			ex.setFrequency(te.getFrequency()); // FIX spelling (was frequancy)

			ex.setSets(te.getSets());
			ex.setRepetitions(te.getRepetitions());

			// ✅ Media & Notes
			ex.setYoutubeUrl(te.getYoutubeUrl());
			ex.setNotes(te.getNotes());

			// ✅ Pricing
			ex.setPricePerSession(te.getPricePerSession() != null ? te.getPricePerSession().intValue() : 0);

			ex.setDiscountPercentage(te.getDiscountPercentage());
			ex.setDiscountAmount(te.getDiscountAmount());
			ex.setGst(te.getGst());
			ex.setOtherTax(te.getOtherTax());

			ex.setTotalExercisePrice(te.getTotalExercisePrice());
			ex.setTotalPrice(te.getTotalPrice());

			// ✅ Payment
			ex.setPaymentStatus(te.getPaymentStatus());

			// ✅ New Fields
			ex.setTechnique(te.getTechnique());
			ex.setMachine(te.getMachine());
			ex.setIntensity(te.getIntensity());
			ex.setAssistanceLevel(te.getAssistanceLevel());
			ex.setType(te.getType());
			ex.setArea(te.getArea());
			ex.setMetric(te.getMetric());
			ex.setValue(te.getValue());
			ex.setUnit(te.getUnit());
			ex.setBodyPart(te.getBodyPart());

			// ✅ Activity Fields
			ex.setActivityType(te.getActivityType());
			ex.setActivityDuration(te.getActivityDuration());

			// ✅ Sessions Mapping (IMPORTANT)
			// ex.setSessions(mapSessions(te.getSessions()));

			return ex;
		}).toList();
	}

	private double calculateExerciseCost(Exercise ex) {

		int sessions = ex.getNoOfSessions() != null ? ex.getNoOfSessions() : 0;
		int price = ex.getTotalPrice() != 0.0 ? (int) ex.getTotalPrice() : 0;

		return sessions * price;
	}

	@Override
	public ResponseEntity<?> getDoctorAppointmentsonStatus(String clinicId, String branchId, String doctorId,
			String status) {
		Response res = new Response();
		try {
			return bookingFeign.getDoctorAppointmentsonStatus(clinicId, branchId, doctorId, status);
		} catch (FeignException ex) {
			res.setStatus(ex.status());
			res.setMessage(ExtractFeignMessage.clearMessage(ex));
			res.setSuccess(false);
			return ResponseEntity.status(ex.status()).body(res);
		}
	}

	public Response getInvestigations(String bookingId, String patientId) {

		Response response = new Response();

		try {

			List<PhysiotherapyRecord> records = repository.findByBookingIdAndPatientInfoPatientId(bookingId, patientId);

			if (records == null || records.isEmpty()) {
				response.setSuccess(false);
				response.setStatus(404);
				response.setMessage("No records found");
				return response;
			}

			List<Investigation> investigations = records.stream().map(PhysiotherapyRecord::getInvestigation)
					.filter(Objects::nonNull).toList();

			response.setSuccess(true);
			response.setStatus(200);
			response.setMessage("Investigations fetched successfully");
			response.setData(investigations);

		} catch (Exception e) {
			response.setSuccess(false);
			response.setStatus(500);
			response.setMessage(e.getMessage());
		}

		return response;
	}
}