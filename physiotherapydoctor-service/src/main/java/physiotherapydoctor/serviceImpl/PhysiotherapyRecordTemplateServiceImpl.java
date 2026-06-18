package physiotherapydoctor.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.Exercise;
import physiotherapydoctor.dto.ExerciseCalculationsForTemplate;
import physiotherapydoctor.dto.PackageCalculationForTemplate;
import physiotherapydoctor.dto.PhysiotherapyRecordTemplateDTO;
import physiotherapydoctor.dto.Program;
import physiotherapydoctor.dto.ProgramAndTherophyAndExcercisesInfoForTemplate;
import physiotherapydoctor.dto.ProgramCalculationsForTemplate;
import physiotherapydoctor.dto.ProgramDataForPackage;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.Session;
import physiotherapydoctor.dto.TemplateSummaryDTO;
import physiotherapydoctor.dto.TheraphyInfo;
import physiotherapydoctor.dto.TherapyCalculationsForTemplate;
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
import physiotherapydoctor.entity.PhysiotherapyRecordTemplate;
import physiotherapydoctor.repository.PaymentRepository;
import physiotherapydoctor.repository.PhysiotherapyRecordTemplateRepository;
import physiotherapydoctor.service.PhysiotherapyRecordTemplateService;
import physiotherapydoctor.service.S3Service;

@Service
@RequiredArgsConstructor
public class PhysiotherapyRecordTemplateServiceImpl implements PhysiotherapyRecordTemplateService {

	private final PhysiotherapyRecordTemplateRepository repository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Override
	public Response create(PhysiotherapyRecordTemplateDTO dto) {

		Response response = new Response();

		if (dto == null) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Request body is null");
			response.setStatus(400);
			return response;
		}

		calculateTherapyPrices(dto.getTherapySessions());

		PhysiotherapyRecordTemplate entity = mapToEntity(dto);

		LocalDateTime now = LocalDateTime.now();

		String createdDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String createdTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

		entity.setCreatedAt(createdDate);
		entity.setCreatedTime(createdTime);
		entity.setUpdatedAt(createdDate);

		PhysiotherapyRecordTemplate saved = repository.save(entity);

		List<Map<String, Object>> cleanSessions = transformTherapySessions(saved.getTherapySessions());

		saved.setTherapySessions((List) cleanSessions);

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

	@Override
	public Response getTemplatesByClinicId(String clinicId) {

		Response response = new Response();

		List<PhysiotherapyRecordTemplate> templates = repository.findByClinicId(clinicId);

		if (templates == null || templates.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("No templates found");
			response.setStatus(404);
			return response;
		}

		List<TemplateSummaryDTO> result = new ArrayList<>();

		for (PhysiotherapyRecordTemplate template : templates) {

			TemplateSummaryDTO dto = new TemplateSummaryDTO();

			dto.setTemplateRecordId(template.getTemplateRecordId());

			if (template.getDiagnosis() != null) {
				dto.setPhysioDiagnosis(template.getDiagnosis().getPhysioDiagnosis());
			}

			result.add(dto);
		}

		response.setSuccess(true);
		response.setData(result);
		response.setMessage("Templates fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getTemplateByClinicIdAndTemplateId(String clinicId, String templateRecordId) {

		Response response = new Response();

		Optional<PhysiotherapyRecordTemplate> template = repository.findByClinicIdAndTemplateRecordId(clinicId,
				templateRecordId);

		if (template.isEmpty()) {

			response.setSuccess(false);
			response.setMessage("Template not found");
			response.setStatus(404);

			return response;
		}

		response.setSuccess(true);
		response.setData(template.get());
		response.setMessage("Template fetched successfully");
		response.setStatus(200);

		return response;
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

		Optional<PhysiotherapyRecordTemplate> optional = repository.findById(id);

		if (optional.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Template not found");
			response.setStatus(404);
			return response;
		}

		PhysiotherapyRecordTemplate record = optional.get();
		response.setSuccess(true);
		response.setData(record);
		response.setMessage("Success");
		response.setStatus(200);

		return response;
	}

	// ✅ GET ALL
	@Override
	public Response getAll() {

		Response response = new Response();

		List<PhysiotherapyRecordTemplate> list = repository.findAll();

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
	public Response update(String id, PhysiotherapyRecordTemplateDTO dto) {

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

		Optional<PhysiotherapyRecordTemplate> optional = repository.findById(id);

		if (optional.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("Template not found");
			response.setStatus(404);
			return response;
		}

		PhysiotherapyRecordTemplate existing = optional.get();

		if (dto.getDiagnosis() != null) {
			existing.setDiagnosis(dto.getDiagnosis());
		}

		if (dto.getTreatmentPlan() != null) {
			existing.setTreatmentPlan(dto.getTreatmentPlan());
		}

		// ✅ IMPORTANT: handle sessions properly
		if (dto.getTherapySessions() != null) {

			existing.setTherapySessions(dto.getTherapySessions());
		}

		// ✅ HOME EXERCISE UPDATE
		if (dto.getExercisePlan() != null) {
			existing.setExercisePlan(dto.getExercisePlan());
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

		PhysiotherapyRecordTemplate updated = repository.save(existing);

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
			response.setMessage("Template not found");
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
	private PhysiotherapyRecordTemplate mapToEntity(PhysiotherapyRecordTemplateDTO dto) {

		PhysiotherapyRecordTemplate entity = new PhysiotherapyRecordTemplate();

		if (dto == null)
			return entity;

		// =========================
		// ✅ BASIC DETAILS
		// =========================
		entity.setBookingId(dto.getBookingId());
		entity.setClinicId(dto.getClinicId());
		entity.setBranchId(dto.getBranchId());

		// =========================
		// 🔥 INVESTIGATION (MISSING FIX)
		// =========================
		if (dto.getInvestigation() != null) {
			entity.setInvestigation(dto.getInvestigation());
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
		return entity;
	}

	@Override
	public Response getByMultipleFields(String clinicId, String branchId, String bookingId, String templateRecordId) {

		Response response = new Response();

		if (clinicId == null || branchId == null || bookingId == null || templateRecordId == null) {

			response.setSuccess(false);
			response.setMessage("All fields are required");
			response.setStatus(400);
			return response;
		}

		Optional<PhysiotherapyRecordTemplate> record = repository
				.findByClinicIdAndBranchIdAndBookingIdAndTemplateRecordId(clinicId, branchId, bookingId,
						templateRecordId);

		if (record.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("Template not found");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(record.get());
		response.setMessage("Template fetched successfully");
		response.setStatus(200);

		return response;
	}

	@Override
	public Response getByWithoutTherapistRecordId(String clinicId, String branchId, String bookingId) {

		Response response = new Response();

		if (clinicId == null || branchId == null || bookingId == null) {
			response.setSuccess(false);
			response.setMessage("All fields are required");
			response.setStatus(400);
			return response;
		}

		List<PhysiotherapyRecordTemplate> records = repository.findByClinicIdAndBranchIdAndBookingId(clinicId, branchId,
				bookingId);
///System.out.println(records);
		if (records == null || records.isEmpty()) {
			response.setSuccess(false);
			response.setMessage("No template record found");
			response.setStatus(404);
			return response;
		}
		response.setSuccess(true);
		response.setData(records);
		response.setMessage("Template record fetched successfully");
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
		Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, bookingId);

		if (!fetchedResponse.isSuccess()) {
			return fetchedResponse;
		}

		List<PhysiotherapyRecordTemplate> records = (List<PhysiotherapyRecordTemplate>) fetchedResponse.getData();

		List<ProgramAndTherophyAndExcercisesInfoForTemplate> resultList = new ArrayList<>();

		for (PhysiotherapyRecordTemplate record : records) {

			List<TherapySession> therapySessions = record.getTherapySessions();

			if (therapySessions == null || therapySessions.isEmpty()) {
				continue;
			}

			for (TherapySession session : therapySessions) {

				ProgramAndTherophyAndExcercisesInfoForTemplate info = new ProgramAndTherophyAndExcercisesInfoForTemplate();
				TreatmentPlan plan = record.getTreatmentPlan();

				info.setDoctorName(plan.getDoctorName());
				info.setDoctorId(plan.getDoctorId());
				info.setTherapistName(plan.getTherapistName());
				info.setTherapistId(plan.getTherapistId());
				info.setBookingId(record.getBookingId());
				info.setTemplateRecordId(record.getTemplateRecordId());
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
	public ResponseEntity<Response> getCalculations(String clinicId, String branchId, String bookingId) {
		try {
			Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, bookingId);

			if (fetchedResponse == null || fetchedResponse.getData() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new Response(false, null, "Template not found", 404));
			}

			List<PhysiotherapyRecordTemplate> records = extractRecords(fetchedResponse.getData());

			if (records == null || records.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(new Response(false, null, "No records found", 204));
			}

			List<Object> result = new ArrayList<>();

			for (PhysiotherapyRecordTemplate record : records) {

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

	private PackageCalculationForTemplate handlePackage(PhysiotherapyRecordTemplate record, TherapySession session) {

		PackageCalculationForTemplate dto = new PackageCalculationForTemplate();

		dto.setServiceType("package");
		dto.setBookingId(record.getBookingId());
		dto.setTemplateRecordId(record.getTemplateRecordId());
		dto.setClinicId(record.getClinicId());
		dto.setBranchId(record.getBranchId());

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

	private ProgramCalculationsForTemplate handleProgram(PhysiotherapyRecordTemplate record, TherapySession session) {

		ProgramCalculationsForTemplate dto = new ProgramCalculationsForTemplate();

		dto.setServiceType("program");
		dto.setBookingId(record.getBookingId());
		dto.setTemplateRecordId(record.getTemplateRecordId());
		dto.setClinicId(record.getClinicId());
		dto.setBranchId(record.getBranchId());
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

	private TherapyCalculationsForTemplate handleTherapy(PhysiotherapyRecordTemplate record, TherapySession session) {

		TherapyCalculationsForTemplate dto = new TherapyCalculationsForTemplate();

		dto.setServiceType("therapy");
		dto.setBookingId(record.getBookingId());
		dto.setTemplateRecordId(record.getTemplateRecordId());
		dto.setClinicId(record.getClinicId());
		dto.setBranchId(record.getBranchId());

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

	private ExerciseCalculationsForTemplate handleExercise(PhysiotherapyRecordTemplate record, TherapySession session) {

		ExerciseCalculationsForTemplate dto = new ExerciseCalculationsForTemplate();

		dto.setServiceType("exercise");
		dto.setBookingId(record.getBookingId());
		dto.setTemplateRecordId(record.getTemplateRecordId());
		dto.setClinicId(record.getClinicId());
		dto.setBranchId(record.getBranchId());
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

	private List<PhysiotherapyRecordTemplate> extractRecords(Object data) {

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		try {
			return objectMapper.convertValue(data, new TypeReference<List<PhysiotherapyRecordTemplate>>() {
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
		List<PhysiotherapyRecordTemplate> record = repository.findByClinicIdAndBranchIdAndBookingId(clinicId, branchId,
				bookingId);

		if (record == null || record.isEmpty()) {
			response.setSuccess(false);
			response.setData(null);
			response.setMessage("No Template found");
			response.setStatus(404);
			return response;
		}

		response.setSuccess(true);
		response.setData(record);
		response.setMessage("Records fetched successfully");
		response.setStatus(200);

		return response;
	}

	public ResponseEntity<List<Session>> getSessionsByBookingIdAndDate(String bookingId, String date) {

		try {
			Optional<PaymentRecord> optional = paymentRepository.findByBookingId(bookingId);

			if (optional.isEmpty()) {
				return ResponseEntity.ok(null);
			}

			PaymentRecord record = optional.get();
			// System.out.println(record);
			List<Session> matchedSessions = new ArrayList<>();

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

	private void handlePrograms(List<Program> programs, String date, List<Session> result) {

		if (programs == null)
			return;

		for (Program program : programs) {
			handleTherapyData(program.getTherapyData(), date, result);
		}
	}

	private void handleTherapyData(List<TherapyData> therapyDataList, String date, List<Session> result) {

		if (therapyDataList == null)
			return;

		for (TherapyData td : therapyDataList) {
			handleExercises(td.getExercises(), date, result);
		}
	}

	private void handleExercises(List<TherapyExercise> exercises, String date, List<Session> result) {

		if (exercises == null)
			return;

		for (TherapyExercise ex : exercises) {

			if (ex.getSessions() == null)
				continue;

			for (Session session : ex.getSessions()) {

				if (date.equals(session.getDate())) {
					result.add(session);
				}
			}
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

}
