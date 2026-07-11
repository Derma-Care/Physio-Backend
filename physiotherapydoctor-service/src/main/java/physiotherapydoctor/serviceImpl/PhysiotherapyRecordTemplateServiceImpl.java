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
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PhysiotherapyRecordTemplateServiceImpl implements PhysiotherapyRecordTemplateService {

	private final PhysiotherapyRecordTemplateRepository repository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "createFallback")
	@Secured("ROLE_DOCTOR")
	public Response create(PhysiotherapyRecordTemplateDTO dto) {

	    log.info("Create physiotherapy record template request received");

	    Response response = new Response();

	    if (dto == null) {

	        log.warn("PhysiotherapyRecordTemplateDTO is null");

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage("Request body is null");
	        response.setStatus(400);

	        return response;
	    }

	    try {

	        log.debug("Calculating therapy prices");

	        calculateTherapyPrices(dto.getTherapySessions());

	        log.debug("Mapping DTO to entity");

	        PhysiotherapyRecordTemplate entity =
	                mapToEntity(dto);

	        LocalDateTime now = LocalDateTime.now();

	        String createdDate =
	                now.format(
	                        DateTimeFormatter.ofPattern("yyyy-MM-dd"));

	        String createdTime =
	                now.format(
	                        DateTimeFormatter.ofPattern("hh:mm a"));

	        entity.setCreatedAt(createdDate);
	        entity.setCreatedTime(createdTime);
	        entity.setUpdatedAt(createdDate);

	        log.debug(
	                "Saving physiotherapy record template. createdDate={}, createdTime={}",
	                createdDate,
	                createdTime);

	        PhysiotherapyRecordTemplate saved =
	                repository.save(entity);

	        log.info(
	                "Physiotherapy record template saved successfully. id={}",
	                saved.getTemplateRecordId());

	        List<Map<String, Object>> cleanSessions =
	                transformTherapySessions(
	                        saved.getTherapySessions());

	        saved.setTherapySessions((List) cleanSessions);

	        response.setSuccess(true);
	        response.setData(saved);
	        response.setMessage("Record created successfully");
	        response.setStatus(201);

	        log.info("Physiotherapy record template created successfully");

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while creating physiotherapy record template. Error={}",
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}

	private List<Map<String, Object>> transformTherapySessions(
	        List<TherapySession> sessions) {

	    log.debug("Transforming therapy sessions");

	    if (sessions == null) {

	        log.debug("Therapy sessions list is null");

	        return null;
	    }

	    log.debug(
	            "Total therapy sessions received={}",
	            sessions.size());

	    List<Map<String, Object>> result =
	            new ArrayList<>();

	    for (TherapySession s : sessions) {

	        log.debug(
	                "Processing serviceType={}",
	                s.getServiceType());

	        Map<String, Object> obj =
	                new LinkedHashMap<>();

	        obj.put("serviceType", s.getServiceType());
	        obj.put("totalPrice", s.getTotalPrice());

	        switch (s.getServiceType().toLowerCase()) {

	        case "package":

	            log.debug(
	                    "Transforming package. packageId={}, packageName={}",
	                    s.getPackageId(),
	                    s.getPackageName());

	            obj.put("packageId", s.getPackageId());
	            obj.put("packageName", s.getPackageName());
	            obj.put("programs", s.getPrograms());

	            break;

	        case "program":

	            log.debug(
	                    "Transforming program. programId={}, programName={}",
	                    s.getProgramId(),
	                    s.getProgramName());

	            obj.put("programId", s.getProgramId());
	            obj.put("programName", s.getProgramName());
	            obj.put("therapyData", s.getTherapyData());

	            break;

	        case "therapy":

	            log.debug(
	                    "Transforming therapy. therapyId={}, therapyName={}",
	                    s.getTherapyId(),
	                    s.getTherapyName());

	            obj.put("therapyId", s.getTherapyId());
	            obj.put("therapyName", s.getTherapyName());
	            obj.put("exercises", s.getExercises());

	            break;

	        case "exercise":

	            log.debug("Transforming exercise level data");

	            obj.put("exercises", s.getExercises());

	            break;

	        default:

	            log.warn(
	                    "Unknown serviceType encountered={}",
	                    s.getServiceType());
	        }

	        obj.values().removeIf(Objects::isNull);

	        result.add(obj);
	    }

	    log.debug(
	            "Therapy session transformation completed. transformedCount={}",
	            result.size());

	    return result;
	}
	private void calculateTherapyPrices(List<TherapySession> sessions) {

	    log.debug("Calculating therapy prices");

	    if (sessions == null) {

	        log.warn("Therapy sessions list is null. Skipping price calculation");

	        return;
	    }

	    log.debug("Total therapy sessions received={}", sessions.size());

	    for (TherapySession session : sessions) {

	        log.debug(
	                "Processing serviceType={}",
	                session.getServiceType());

	        // ================= PACKAGE =================
	        if (session.getPrograms() != null) {

	            double packageTotal = 0;

	            log.debug(
	                    "Calculating package price. Program count={}",
	                    session.getPrograms().size());

	            for (Program p : session.getPrograms()) {

	                double programTotal = 0;

	                log.debug(
	                        "Processing programId={}, programName={}",
	                        p.getProgramId(),
	                        p.getProgramName());

	                if (p.getTherapyData() != null) {

	                    for (TherapyData t : p.getTherapyData()) {

	                        double therapyTotal = 0;

	                        log.debug(
	                                "Processing therapyId={}, therapyName={}",
	                                t.getTherapyId(),
	                                t.getTherapyName());

	                        if (t.getExercises() != null) {

	                            for (TherapyExercise ex : t.getExercises()) {

	                                double exTotal = 0;

	                                if (ex.getTotalExercisePrice() != null) {

	                                    exTotal =
	                                            ex.getTotalExercisePrice();

	                                } else if (ex.getPricePerSession() != null
	                                        && ex.getNoOfSessions() != null) {

	                                    exTotal =
	                                            ex.getPricePerSession()
	                                                    * ex.getNoOfSessions();
	                                }

	                                ex.setTotalExercisePrice(exTotal);

	                                therapyTotal += exTotal;

	                                log.debug(
	                                        "ExerciseId={}, ExercisePrice={}",
	                                        ex.getExerciseId(),
	                                        exTotal);
	                            }
	                        }

	                        t.setTotalTherapyPrice(therapyTotal);

	                        log.debug(
	                                "TherapyId={} TotalTherapyPrice={}",
	                                t.getTherapyId(),
	                                therapyTotal);

	                        programTotal += therapyTotal;
	                    }
	                }

	                p.setTotalProgramPrice(programTotal);

	                log.debug(
	                        "ProgramId={} TotalProgramPrice={}",
	                        p.getProgramId(),
	                        programTotal);

	                packageTotal += programTotal;
	            }

	            session.setTotalPackageCost(packageTotal);
	            session.setTotalPrice(packageTotal);

	            log.info(
	                    "Package total calculated. TotalPackageCost={}",
	                    packageTotal);
	        }

	        // ================= PROGRAM =================
	        if (session.getTherapyData() != null) {

	            double programTotal = 0;

	            log.debug(
	                    "Calculating program price. Therapy count={}",
	                    session.getTherapyData().size());

	            for (TherapyData t : session.getTherapyData()) {

	                double therapyTotal = 0;

	                log.debug(
	                        "Processing therapyId={}, therapyName={}",
	                        t.getTherapyId(),
	                        t.getTherapyName());

	                if (t.getExercises() != null) {

	                    for (TherapyExercise ex : t.getExercises()) {

	                        double exTotal = 0;

	                        if (ex.getTotalExercisePrice() != null) {

	                            exTotal =
	                                    ex.getTotalExercisePrice();

	                        } else if (ex.getPricePerSession() != null
	                                && ex.getNoOfSessions() != null) {

	                            exTotal =
	                                    ex.getPricePerSession()
	                                            * ex.getNoOfSessions();
	                        }

	                        ex.setTotalExercisePrice(exTotal);

	                        therapyTotal += exTotal;

	                        log.debug(
	                                "ExerciseId={}, ExercisePrice={}",
	                                ex.getExerciseId(),
	                                exTotal);
	                    }
	                }

	                t.setTotalTherapyPrice(therapyTotal);

	                log.debug(
	                        "TherapyId={} TotalTherapyPrice={}",
	                        t.getTherapyId(),
	                        therapyTotal);

	                programTotal += therapyTotal;
	            }

	            session.setTotalProgramCost(programTotal);
	            session.setTotalPrice(programTotal);

	            log.info(
	                    "Program total calculated. TotalProgramCost={}",
	                    programTotal);
	        }

	        // ================= THERAPY =================
	        if (session.getExercises() != null
	                && session.getPrograms() == null
	                && session.getTherapyData() == null) {

	            double therapyTotal = 0;

	            log.debug(
	                    "Calculating therapy price. Exercise count={}",
	                    session.getExercises().size());

	            for (TherapyExercise ex : session.getExercises()) {

	                double exTotal = 0;

	                if (ex.getTotalExercisePrice() != null) {

	                    exTotal =
	                            ex.getTotalExercisePrice();

	                } else if (ex.getPricePerSession() != null
	                        && ex.getNoOfSessions() != null) {

	                    exTotal =
	                            ex.getPricePerSession()
	                                    * ex.getNoOfSessions();
	                }

	                ex.setTotalExercisePrice(exTotal);

	                therapyTotal += exTotal;

	                log.debug(
	                        "ExerciseId={}, ExercisePrice={}",
	                        ex.getExerciseId(),
	                        exTotal);
	            }

	            session.setTotalTherapyCost(therapyTotal);
	            session.setTotalPrice(therapyTotal);

	            log.info(
	                    "Therapy total calculated. TotalTherapyCost={}",
	                    therapyTotal);
	        }

	        log.debug(
	                "Completed price calculation for serviceType={}, TotalPrice={}",
	                session.getServiceType(),
	                session.getTotalPrice());
	    }

	    log.info("Therapy price calculation completed successfully");
	}
	
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTemplatesByClinicIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getTemplatesByClinicId(String clinicId) {

	    log.info("Fetching templates by clinicId={}", clinicId);

	    Response response = new Response();

	    try {

	        List<PhysiotherapyRecordTemplate> templates =
	                repository.findByClinicId(clinicId);

	        if (templates == null || templates.isEmpty()) {

	            log.warn(
	                    "No templates found for clinicId={}",
	                    clinicId);

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("No templates found");
	            response.setStatus(404);

	            return response;
	        }

	        log.info(
	                "Found {} templates for clinicId={}",
	                templates.size(),
	                clinicId);

	        List<TemplateSummaryDTO> result =
	                new ArrayList<>();

	        for (PhysiotherapyRecordTemplate template : templates) {

	            log.debug(
	                    "Processing templateRecordId={}",
	                    template.getTemplateRecordId());

	            TemplateSummaryDTO dto =
	                    new TemplateSummaryDTO();

	            dto.setTemplateRecordId(
	                    template.getTemplateRecordId());

	            if (template.getDiagnosis() != null) {

	                dto.setPhysioDiagnosis(
	                        template.getDiagnosis()
	                                .getPhysioDiagnosis());
	            }

	            result.add(dto);
	        }

	        response.setSuccess(true);
	        response.setData(result);
	        response.setMessage("Templates fetched successfully");
	        response.setStatus(200);

	        log.info(
	                "Templates fetched successfully for clinicId={}",
	                clinicId);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error fetching templates for clinicId={}. Error={}",
	                clinicId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}


	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTemplateByClinicIdAndTemplateIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getTemplateByClinicIdAndTemplateId(
	        String clinicId,
	        String templateRecordId) {

	    log.info(
	            "Fetching template by clinicId={} and templateRecordId={}",
	            clinicId,
	            templateRecordId);

	    Response response = new Response();

	    try {

	        Optional<PhysiotherapyRecordTemplate> template =
	                repository.findByClinicIdAndTemplateRecordId(
	                        clinicId,
	                        templateRecordId);

	        if (template.isEmpty()) {

	            log.warn(
	                    "Template not found. clinicId={}, templateRecordId={}",
	                    clinicId,
	                    templateRecordId);

	            response.setSuccess(false);
	            response.setMessage("Template not found");
	            response.setStatus(404);

	            return response;
	        }

	        log.info(
	                "Template fetched successfully. templateRecordId={}",
	                templateRecordId);

	        response.setSuccess(true);
	        response.setData(template.get());
	        response.setMessage("Template fetched successfully");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error fetching template. clinicId={}, templateRecordId={}, error={}",
	                clinicId,
	                templateRecordId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}


	// ✅ GET BY ID
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getById(String id) {

	    log.info("Fetching template by id={}", id);

	    Response response = new Response();

	    try {

	        if (id == null || id.isEmpty()) {

	            log.warn("Template id is null or empty");

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("ID is required");
	            response.setStatus(400);

	            return response;
	        }

	        Optional<PhysiotherapyRecordTemplate> optional =
	                repository.findById(id);

	        if (optional.isEmpty()) {

	            log.warn(
	                    "Template not found for id={}",
	                    id);

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("Template not found");
	            response.setStatus(404);

	            return response;
	        }

	        log.info(
	                "Template fetched successfully for id={}",
	                id);

	        PhysiotherapyRecordTemplate record =
	                optional.get();

	        response.setSuccess(true);
	        response.setData(record);
	        response.setMessage("Success");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error fetching template by id={}. Error={}",
	                id,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}
	
	// ✅ GET ALL
	@Override
	@RateLimiter(name = "physiotherapyRecordTemplateService", fallbackMethod = "getAllFallback")
	@Secured("ROLE_DOCTOR")
	public Response getAll() {

	    log.info("Fetching all physiotherapy record templates");

	    Response response = new Response();

	    try {

	        List<PhysiotherapyRecordTemplate> list =
	                repository.findAll();

	        log.info(
	                "Total templates found={}",
	                list.size());

	        if (list.isEmpty()) {

	            log.warn("No physiotherapy record templates found");

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

	        log.info(
	                "Successfully fetched {} physiotherapy record templates",
	                list.size());

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching all physiotherapy record templates. Error={}",
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}


	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateFallback")
	@Secured("ROLE_DOCTOR")
	public Response update(
	        String id,
	        PhysiotherapyRecordTemplateDTO dto) {

	    log.info(
	            "Update physiotherapy record template request received. id={}",
	            id);

	    Response response = new Response();

	    try {

	        if (id == null || id.isEmpty()) {

	            log.warn("Template id is null or empty");

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("ID is required");
	            response.setStatus(400);

	            return response;
	        }

	        if (dto == null) {

	            log.warn(
	                    "Request body is null for template id={}",
	                    id);

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("Request body is null");
	            response.setStatus(400);

	            return response;
	        }

	        Optional<PhysiotherapyRecordTemplate> optional =
	                repository.findById(id);

	        if (optional.isEmpty()) {

	            log.warn(
	                    "Template not found for id={}",
	                    id);

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("Template not found");
	            response.setStatus(404);

	            return response;
	        }

	        PhysiotherapyRecordTemplate existing =
	                optional.get();

	        log.debug(
	                "Updating template fields for id={}",
	                id);

	        if (dto.getDiagnosis() != null) {

	            log.debug("Updating diagnosis");

	            existing.setDiagnosis(dto.getDiagnosis());
	        }

	        if (dto.getTreatmentPlan() != null) {

	            log.debug("Updating treatment plan");

	            existing.setTreatmentPlan(
	                    dto.getTreatmentPlan());
	        }

	        if (dto.getTherapySessions() != null) {

	            log.debug(
	                    "Updating therapy sessions. Count={}",
	                    dto.getTherapySessions().size());

	            existing.setTherapySessions(
	                    dto.getTherapySessions());
	        }

	        if (dto.getExercisePlan() != null) {

	            log.debug("Updating exercise plan");

	            existing.setExercisePlan(
	                    dto.getExercisePlan());
	        }

	        if (dto.getFollowUp() != null) {

	            log.debug("Updating follow-up details");

	            existing.setFollowUp(
	                    dto.getFollowUp());
	        }

	        if (dto.getRecoverySupport() != null) {

	            log.debug("Updating recovery support");

	            existing.setRecoverySupport(
	                    dto.getRecoverySupport());
	        }

	        String now =
	                LocalDateTime.now()
	                        .format(
	                                DateTimeFormatter.ofPattern(
	                                        "yyyy-MM-dd"));

	        existing.setUpdatedAt(now);

	        log.debug(
	                "Updated timestamp set to {}",
	                now);

	        PhysiotherapyRecordTemplate updated =
	                repository.save(existing);

	        log.info(
	                "Physiotherapy record template updated successfully. id={}",
	                id);

	        response.setSuccess(true);
	        response.setData(updated);
	        response.setMessage("Updated successfully");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while updating physiotherapy record template. id={}, error={}",
	                id,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}

	// ✅ DELETE
	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteFallback")
	@Secured("ROLE_DOCTOR")
	public Response delete(String id) {

	    log.info("Delete physiotherapy record template request received. id={}", id);

	    Response response = new Response();

	    try {

	        if (id == null || id.isEmpty()) {

	            log.warn("Template id is null or empty");

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("ID is required");
	            response.setStatus(400);

	            return response;
	        }

	        boolean exists = repository.existsById(id);

	        if (!exists) {

	            log.warn(
	                    "Template not found for deletion. id={}",
	                    id);

	            response.setSuccess(false);
	            response.setData(null);
	            response.setMessage("Template not found");
	            response.setStatus(404);

	            return response;
	        }

	        log.debug(
	                "Deleting physiotherapy record template. id={}",
	                id);

	        repository.deleteById(id);

	        log.info(
	                "Physiotherapy record template deleted successfully. id={}",
	                id);

	        response.setSuccess(true);
	        response.setData(null);
	        response.setMessage("Deleted successfully");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while deleting physiotherapy record template. id={}, error={}",
	                id,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setData(null);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
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
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByMultipleFieldsFallback")
	@Secured("ROLE_DOCTOR")
	public Response getByMultipleFields(
	        String clinicId,
	        String branchId,
	        String bookingId,
	        String templateRecordId) {

	    log.info(
	            "Fetching template by multiple fields. clinicId={}, branchId={}, bookingId={}, templateRecordId={}",
	            clinicId,
	            branchId,
	            bookingId,
	            templateRecordId);

	    Response response = new Response();

	    try {

	        if (clinicId == null
	                || branchId == null
	                || bookingId == null
	                || templateRecordId == null) {

	            log.warn(
	                    "Required fields are missing. clinicId={}, branchId={}, bookingId={}, templateRecordId={}",
	                    clinicId,
	                    branchId,
	                    bookingId,
	                    templateRecordId);

	            response.setSuccess(false);
	            response.setMessage("All fields are required");
	            response.setStatus(400);

	            return response;
	        }

	        Optional<PhysiotherapyRecordTemplate> record =
	                repository.findByClinicIdAndBranchIdAndBookingIdAndTemplateRecordId(
	                        clinicId,
	                        branchId,
	                        bookingId,
	                        templateRecordId);

	        if (record.isEmpty()) {

	            log.warn(
	                    "Template not found. clinicId={}, branchId={}, bookingId={}, templateRecordId={}",
	                    clinicId,
	                    branchId,
	                    bookingId,
	                    templateRecordId);

	            response.setSuccess(false);
	            response.setMessage("Template not found");
	            response.setStatus(404);

	            return response;
	        }

	        log.info(
	                "Template fetched successfully. templateRecordId={}",
	                templateRecordId);

	        response.setSuccess(true);
	        response.setData(record.get());
	        response.setMessage("Template fetched successfully");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching template. clinicId={}, branchId={}, bookingId={}, templateRecordId={}, error={}",
	                clinicId,
	                branchId,
	                bookingId,
	                templateRecordId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
	}


	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getByWithoutTherapistRecordIdFallback")
	@Secured("ROLE_DOCTOR")
	public Response getByWithoutTherapistRecordId(
	        String clinicId,
	        String branchId,
	        String bookingId) {

	    log.info(
	            "Fetching templates by clinicId={}, branchId={}, bookingId={}",
	            clinicId,
	            branchId,
	            bookingId);

	    Response response = new Response();

	    try {

	        if (clinicId == null
	                || branchId == null
	                || bookingId == null) {

	            log.warn(
	                    "Required fields are missing. clinicId={}, branchId={}, bookingId={}",
	                    clinicId,
	                    branchId,
	                    bookingId);

	            response.setSuccess(false);
	            response.setMessage("All fields are required");
	            response.setStatus(400);

	            return response;
	        }

	        List<PhysiotherapyRecordTemplate> records =
	                repository.findByClinicIdAndBranchIdAndBookingId(
	                        clinicId,
	                        branchId,
	                        bookingId);

	        log.debug(
	                "Templates found count={}",
	                records != null ? records.size() : 0);

	        if (records == null || records.isEmpty()) {

	            log.warn(
	                    "No template records found. clinicId={}, branchId={}, bookingId={}",
	                    clinicId,
	                    branchId,
	                    bookingId);

	            response.setSuccess(false);
	            response.setMessage("No template record found");
	            response.setStatus(404);

	            return response;
	        }

	        log.info(
	                "Successfully fetched {} template records",
	                records.size());

	        response.setSuccess(true);
	        response.setData(records);
	        response.setMessage("Template record fetched successfully");
	        response.setStatus(200);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching template records. clinicId={}, branchId={}, bookingId={}, error={}",
	                clinicId,
	                branchId,
	                bookingId,
	                e.getMessage(),
	                e);

	        response.setSuccess(false);
	        response.setMessage(e.getMessage());
	        response.setStatus(500);

	        return response;
	    }
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
	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getProgramAndTherapyInfoFallback")
@Secured("ROLE_DOCTOR")
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
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getCalculationsFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> getCalculations(String clinicId, String branchId, String bookingId) {

	        log.info("Fetching calculations for clinicId={}, branchId={}, bookingId={}",
	                clinicId, branchId, bookingId);

	        try {

	            Response fetchedResponse = getByWithoutTherapistRecordId(clinicId, branchId, bookingId);

	            if (fetchedResponse == null || fetchedResponse.getData() == null) {
	                log.warn("No template found for bookingId={}, clinicId={}, branchId={}",
	                        bookingId, clinicId, branchId);

	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                        .body(new Response(false, null, "Template not found", 404));
	            }

	            List<PhysiotherapyRecordTemplate> records = extractRecords(fetchedResponse.getData());

	            if (records == null || records.isEmpty()) {
	                log.warn("No records found for bookingId={}", bookingId);

	                return ResponseEntity.status(HttpStatus.NO_CONTENT)
	                        .body(new Response(false, null, "No records found", 204));
	            }

	            log.info("Found {} physiotherapy records for bookingId={}",
	                    records.size(), bookingId);

	            List<Object> result = new ArrayList<>();

	            for (PhysiotherapyRecordTemplate record : records) {

	                log.debug("Processing templateRecordId={}, bookingId={}",
	                        record.getTemplateRecordId(), record.getBookingId());

	                if (record.getTherapySessions() == null || record.getTherapySessions().isEmpty()) {

	                    log.warn("No therapy sessions found for templateRecordId={}",
	                            record.getTemplateRecordId());

	                    continue;
	                }

	                log.info("Found {} therapy sessions for templateRecordId={}",
	                        record.getTherapySessions().size(),
	                        record.getTemplateRecordId());

	                for (TherapySession session : record.getTherapySessions()) {

	                    String serviceType = session.getServiceType();

	                    if (serviceType == null || serviceType.isBlank()) {

	                        log.warn("Skipping session due to empty serviceType. templateRecordId={}",
	                                record.getTemplateRecordId());

	                        continue;
	                    }

	                    log.info("Processing serviceType={} for templateRecordId={}",
	                            serviceType, record.getTemplateRecordId());

	                    switch (serviceType.toLowerCase()) {

	                    case "package":
	                        result.add(handlePackage(record, session));
	                        log.debug("Package calculation completed for templateRecordId={}",
	                                record.getTemplateRecordId());
	                        break;

	                    case "program":
	                        result.add(handleProgram(record, session));
	                        log.debug("Program calculation completed for templateRecordId={}",
	                                record.getTemplateRecordId());
	                        break;

	                    case "therapy":
	                        result.add(handleTherapy(record, session));
	                        log.debug("Therapy calculation completed for templateRecordId={}",
	                                record.getTemplateRecordId());
	                        break;

	                    case "exercise":
	                        result.add(handleExercise(record, session));
	                        log.debug("Exercise calculation completed for templateRecordId={}",
	                                record.getTemplateRecordId());
	                        break;

	                    default:
	                        log.error("Invalid service type '{}' found for templateRecordId={}",
	                                serviceType, record.getTemplateRecordId());

	                        throw new RuntimeException("Invalid service type: " + serviceType);
	                    }
	                }
	            }

	            if (result.isEmpty()) {

	                log.warn("No calculations generated for bookingId={}", bookingId);

	                return ResponseEntity.status(HttpStatus.NO_CONTENT)
	                        .body(new Response(false, null, "No calculations available", 204));
	            }

	            log.info("Successfully generated {} calculations for bookingId={}",
	                    result.size(), bookingId);

	            return ResponseEntity.ok(
	                    new Response(true, result, "Calculations fetched successfully", 200));

	        } catch (IllegalArgumentException ex) {

	            log.error("Validation error while fetching calculations for bookingId={}. Error={}",
	                    bookingId, ex.getMessage(), ex);

	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                    .body(new Response(false, null, ex.getMessage(), 400));

	        } catch (RuntimeException ex) {

	            log.error("Runtime exception while fetching calculations for bookingId={}. Error={}",
	                    bookingId, ex.getMessage(), ex);

	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	                    .body(new Response(false, null, ex.getMessage(), 400));

	        } catch (Exception ex) {

	            log.error("Unexpected exception while fetching calculations for bookingId={}",
	                    bookingId, ex);

	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(new Response(false, null, "Something went wrong", 500));
	        }
	    }
	

	 private PackageCalculationForTemplate handlePackage(PhysiotherapyRecordTemplate record, TherapySession session) {

		    log.info("Started package calculation. bookingId={}, packageId={}, packageName={}",
		            record.getBookingId(), session.getPackageId(), session.getPackageName());

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

		        log.debug("Processing program. programId={}, programName={}",
		                program.getProgramId(), program.getProgramName());

		        ProgramDataForPackage programDTO = new ProgramDataForPackage();
		        programDTO.setProgramId(program.getProgramId());
		        programDTO.setProgramName(program.getProgramName());

		        double programTotal = 0;
		        List<TherapyinfoForPackage> therapyList = new ArrayList<>();

		        for (TherapyData therapy : program.getTherapyData()) {

		            double therapyTotal = 0;
		            List<Exercise> exercises = mapExercises(therapy.getExercises());

		            for (Exercise ex : exercises) {
		                double total = calculateExerciseCost(ex);
		                ex.setTotalSessionCost(total);
		                therapyTotal += total;
		            }

		            TherapyinfoForPackage therapyDTO = new TherapyinfoForPackage();
		            therapyDTO.setTherapyId(therapy.getTherapyId());
		            therapyDTO.setTherapyName(therapy.getTherapyName());
		            therapyDTO.setExercises(exercises);
		            therapyDTO.setTotalPrice(therapyTotal);

		            programTotal += therapyTotal;
		            therapyList.add(therapyDTO);
		        }

		        programDTO.setTherapyData(therapyList);
		        programDTO.setTotalPrice(programTotal);

		        log.debug("Program total calculated. programId={}, total={}",
		                program.getProgramId(), programTotal);

		        programList.add(programDTO);
		    }

		    dto.setTherapySessions(programList);

		    for (ProgramDataForPackage t : programList) {
		        totalPackageCost += t.getTotalPrice();
		    }

		    dto.setTotal(totalPackageCost);

		    log.info("Package calculation completed. bookingId={}, packageId={}, totalPackageCost={}",
		            record.getBookingId(), session.getPackageId(), totalPackageCost);

		    return dto;
		}
	 
	 
	 

	 private ProgramCalculationsForTemplate handleProgram(PhysiotherapyRecordTemplate record, TherapySession session) {

		    log.info("Started program calculation. bookingId={}, programId={}, programName={}",
		            record.getBookingId(), session.getProgramId(), session.getProgramName());

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

		        log.debug("Processing therapy. therapyId={}, therapyName={}",
		                therapy.getTherapyId(), therapy.getTherapyName());

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

		        log.debug("Therapy total calculated. therapyId={}, total={}",
		                therapy.getTherapyId(), therapyTotal);

		        programTotal += therapyTotal;
		        therapyList.add(therapyDTO);
		    }

		    dto.setTherapyData(therapyList);
		    dto.setTotalPrice((int) programTotal);

		    log.info("Program calculation completed. bookingId={}, programId={}, totalPrice={}",
		            record.getBookingId(), session.getProgramId(), programTotal);

		    return dto;
		}
	 
	 private TherapyCalculationsForTemplate handleTherapy(PhysiotherapyRecordTemplate record, TherapySession session) {

		    log.info("Started therapy calculation. bookingId={}, therapyId={}, therapyName={}",
		            record.getBookingId(), session.getTherapyId(), session.getTherapyName());

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

		        log.debug("Exercise calculated. exerciseId={}, cost={}",
		                ex.getExerciseId(), cost);
		    }

		    dto.setExercises(exercises);
		    dto.setTotalPrice((int) total);

		    log.info("Therapy calculation completed. bookingId={}, therapyId={}, totalPrice={}",
		            record.getBookingId(), session.getTherapyId(), total);

		    return dto;
		}
	 
	 private ExerciseCalculationsForTemplate handleExercise(PhysiotherapyRecordTemplate record, TherapySession session) {

		    log.info("Started exercise calculation. bookingId={}",
		            record.getBookingId());

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

		        log.debug("Exercise calculated. exerciseId={}, cost={}",
		                ex.getExerciseId(), cost);
		    }

		    dto.setExercises(exercises);
		    dto.setTotalPrice((int) total);

		    log.info("Exercise calculation completed. bookingId={}, totalPrice={}, exerciseCount={}",
		            record.getBookingId(), total, exercises.size());

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

	@RateLimiter(
		    name = "physiotherapydoctorService",
		    fallbackMethod = "getByClinicBranchAndBookingFallback"
		)
		@Override
		public Response getByClinicBranchAndBooking(
		        String clinicId,
		        String branchId,
		        String bookingId) {

		    log.info("Fetching templates. clinicId={}, branchId={}, bookingId={}",
		            clinicId, branchId, bookingId);

		    Response response = new Response();

		    if (clinicId == null || clinicId.isEmpty()
		            || branchId == null || branchId.isEmpty()
		            || bookingId == null || bookingId.isEmpty()) {

		        log.warn("Invalid request. clinicId={}, branchId={}, bookingId={}",
		                clinicId, branchId, bookingId);

		        response.setSuccess(false);
		        response.setData(null);
		        response.setMessage("clinicId, branchId and bookingId are required");
		        response.setStatus(400);
		        return response;
		    }

		    List<PhysiotherapyRecordTemplate> record =
		            repository.findByClinicIdAndBranchIdAndBookingId(
		                    clinicId, branchId, bookingId);

		    if (record == null || record.isEmpty()) {

		        log.warn("No template found. clinicId={}, branchId={}, bookingId={}",
		                clinicId, branchId, bookingId);

		        response.setSuccess(false);
		        response.setData(null);
		        response.setMessage("No Template found");
		        response.setStatus(404);
		        return response;
		    }

		    log.info("Successfully fetched {} template records for bookingId={}",
		            record.size(), bookingId);

		    response.setSuccess(true);
		    response.setData(record);
		    response.setMessage("Records fetched successfully");
		    response.setStatus(200);

		    return response;
		}
	
	@RateLimiter(
		    name = "physiotherapydoctorService",
		    fallbackMethod = "getSessionsByBookingIdAndDateFallback"
		)
		@Secured("ROLE_DOCTOR")
		public ResponseEntity<List<Session>> getSessionsByBookingIdAndDate(
		        String bookingId,
		        String date) {

		    log.info("Fetching sessions. bookingId={}, date={}", bookingId, date);

		    try {

		        Optional<PaymentRecord> optional =
		                paymentRepository.findByBookingId(bookingId);

		        if (optional.isEmpty()) {

		            log.warn("No payment record found for bookingId={}", bookingId);

		            return ResponseEntity.ok(null);
		        }

		        PaymentRecord record = optional.get();
		        List<Session> matchedSessions = new ArrayList<>();

		        if (record.getTherapyWithSessions() == null) {

		            log.warn("No therapy sessions found in payment record. bookingId={}",
		                    bookingId);

		            return ResponseEntity.ok(null);
		        }

		        for (TherapyWithSessions therapy : record.getTherapyWithSessions()) {
		            handlePrograms(therapy.getPrograms(), date, matchedSessions);
		        }

		        log.info("Found {} matching sessions for bookingId={} and date={}",
		                matchedSessions.size(), bookingId, date);

		        return matchedSessions.isEmpty()
		                ? ResponseEntity.ok(null)
		                : ResponseEntity.ok(matchedSessions);

		    } catch (Exception e) {

		        log.error("Error while fetching sessions. bookingId={}, date={}, error={}",
		                bookingId, date, e.getMessage(), e);

		        return ResponseEntity.status(500).body(null);
		    }
		}
	
	

	private void handlePrograms(List<Program> programs, String date, List<Session> result) {

	    if (programs == null) {
	        log.debug("Programs list is null for date={}", date);
	        return;
	    }

	    log.debug("Processing {} programs for date={}", programs.size(), date);

	    for (Program program : programs) {
	        handleTherapyData(program.getTherapyData(), date, result);
	    }
	}
	
	private void handleTherapyData(List<TherapyData> therapyDataList,
            String date,
            List<Session> result) {

if (therapyDataList == null) {
log.debug("Therapy data list is null for date={}", date);
return;
}

log.debug("Processing {} therapies for date={}",
therapyDataList.size(), date);

for (TherapyData td : therapyDataList) {
handleExercises(td.getExercises(), date, result);
}
}
	
	private void handleExercises(List<TherapyExercise> exercises,
            String date,
            List<Session> result) {

if (exercises == null) {
log.debug("Exercises list is null for date={}", date);
return;
}

for (TherapyExercise ex : exercises) {

if (ex.getSessions() == null) {
continue;
}

for (Session session : ex.getSessions()) {

if (date.equals(session.getDate())) {

log.debug("Matching session found. sessionId={}, date={}",
       session.getSessionId(), session.getDate());

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

	    double totalCost = sessions * price;

	    log.debug("Exercise cost calculated. exerciseId={}, sessions={}, price={}, totalCost={}",
	            ex.getExerciseId(), sessions, price, totalCost);

	    return totalCost;
	}


    private Response buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Rate limit exceeded. Please try again later.");
        return response;
    }


    public Response createFallback(PhysiotherapyRecordTemplateDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getTemplatesByClinicIdFallback(String clinicId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getTemplateByClinicIdAndTemplateIdFallback(String clinicId, String templateRecordId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getByIdFallback(String id, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getAllFallback(Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response updateFallback(String id, PhysiotherapyRecordTemplateDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response deleteFallback(String id, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getByMultipleFieldsFallback(String clinicId, String branchId, String bookingId, String templateRecordId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getByWithoutTherapistRecordIdFallback(String clinicId, String branchId, String bookingId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public Response getProgramAndTherapyInfoFallback(String clinicId, String branchId, String patientId, String bookingId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<Response> getCalculationsFallback(String clinicId, String branchId, String bookingId, Exception ex) {
        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
    }

public Response getByClinicBranchAndBookingFallback(
        String clinicId,
        String branchId,
        String bookingId,
        Exception ex) {

    return buildRateLimitResponse(ex);
}

public ResponseEntity<List<Session>> getSessionsByBookingIdAndDateFallback(
        String bookingId,
        String date,
        Exception ex) {

    return ResponseEntity.status(429).body(null);
}

}