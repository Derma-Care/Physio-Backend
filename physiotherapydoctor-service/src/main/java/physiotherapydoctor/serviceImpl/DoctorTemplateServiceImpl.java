package physiotherapydoctor.serviceImpl;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.DatesDTO;
import physiotherapydoctor.dto.DoctorTemplateDTO;
import physiotherapydoctor.dto.FollowUpDetailsDTO;
import physiotherapydoctor.dto.MedicinesDTO;
import physiotherapydoctor.dto.PrescriptionDetailsDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TestDetailsDTO;
import physiotherapydoctor.dto.TreatmentDetailsDTO;
import physiotherapydoctor.dto.TreatmentResponseDTO;
import physiotherapydoctor.entity.Dates;
import physiotherapydoctor.entity.DoctorTemplate;
import physiotherapydoctor.entity.FollowUpDetails;
import physiotherapydoctor.entity.Medicines;
import physiotherapydoctor.entity.PrescriptionDetails;
import physiotherapydoctor.entity.TestDetails;
import physiotherapydoctor.entity.TreatmentDetails;
import physiotherapydoctor.entity.TreatmentResponse;
import physiotherapydoctor.repository.DoctorTemplateRepository;
import physiotherapydoctor.service.DoctorTemplateService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorTemplateServiceImpl implements DoctorTemplateService {

    private final DoctorTemplateRepository repository;

    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "createTemplateFallback")
    @Secured("ROLE_DOCTOR")
    public Response createTemplate(DoctorTemplateDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered createTemplate() with title : {}", dto.getTitle());

        try {

            String normalizedTitle = dto.getTitle()
                    .trim()
                    .replaceAll("\\s+", " ")
                    .toLowerCase();

            log.debug("Normalized title : {}", normalizedTitle);

            log.debug("Fetching existing templates from repository");

            Optional<DoctorTemplate> existingTemplateOpt = repository.findAll()
                    .stream()
                    .filter(t -> t.getTitle() != null
                            && t.getTitle()
                            .trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase()
                            .equals(normalizedTitle))
                    .findFirst();

            DoctorTemplate savedTemplate;

            if (existingTemplateOpt.isPresent()) {

                log.info("Existing template found for title : {}", dto.getTitle());

                DoctorTemplate existingTemplate = existingTemplateOpt.get();

                dto.setTitle(existingTemplate.getTitle());

                DoctorTemplate updatedEntity = convertToEntity(dto);
                updatedEntity.setId(existingTemplate.getId());
                updatedEntity.setTitle(existingTemplate.getTitle());

                log.debug("Updating existing template with id : {}",
                        existingTemplate.getId());

                savedTemplate = repository.save(updatedEntity);

                log.info("Template updated successfully with id : {}",
                        savedTemplate.getId());

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("createTemplate() completed successfully in {} ms",
                        executionTime);

                return Response.builder()
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .message("Existing template updated successfully")
                        .data(savedTemplate)
                        .build();
            }

            log.info("No existing template found. Creating new template");

            savedTemplate = repository.save(convertToEntity(dto));

            log.info("Template created successfully with id : {}",
                    savedTemplate.getId());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("createTemplate() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.CREATED.value())
                    .message("Doctor template created successfully")
                    .data(savedTemplate)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while creating/updating template. Title : {} Error : {}",
                    dto.getTitle(),
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to create/update doctor template: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTemplateByIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response getTemplateById(String id) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getTemplateById() with id : {}", id);

        try {

            log.debug("Fetching template from repository with id : {}", id);

            Optional<DoctorTemplate> template = repository.findById(id);

            if (template.isPresent()) {

                log.info("Template found with id : {}", id);

                DoctorTemplateDTO dto = convertToDto(template.get());

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("getTemplateById() completed successfully in {} ms",
                        executionTime);

                return Response.builder()
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .message("Doctor template found")
                        .data(dto)
                        .build();
            }

            log.warn("Doctor template not found with id : {}", id);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Doctor template not found with ID: " + id)
                    .data(null)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while fetching template id : {} Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to fetch doctor template: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllTemplatesFallback")
    @Secured("ROLE_DOCTOR")
    public Response getAllTemplates() {

        long startTime = System.currentTimeMillis();

        log.info("Entered getAllTemplates()");

        try {

            log.debug("Fetching all doctor templates from repository");

            List<DoctorTemplate> templates = repository.findAll();

            log.info("Retrieved {} templates from repository",
                    templates.size());

            List<DoctorTemplateDTO> dtos = templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getAllTemplates() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("All doctor templates fetched successfully")
                    .data(dtos)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while fetching all templates. Error : {}",
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to fetch doctor templates: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteTemplateFallback")
    @Secured("ROLE_DOCTOR")
    public Response deleteTemplate(String id) {

        long startTime = System.currentTimeMillis();

        log.info("Entered deleteTemplate() with id : {}", id);

        try {

            log.debug("Checking template existence with id : {}", id);

            Optional<DoctorTemplate> existing = repository.findById(id);

            if (existing.isPresent()) {

                log.info("Template found. Deleting template with id : {}", id);

                repository.deleteById(id);

                log.info("Template deleted successfully with id : {}", id);

                long executionTime = System.currentTimeMillis() - startTime;

                log.info("deleteTemplate() completed successfully in {} ms",
                        executionTime);

                return Response.builder()
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .message("Doctor template deleted successfully")
                        .data(null)
                        .build();
            }

            log.warn("Doctor template not found with id : {}", id);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Doctor template not found with ID: " + id)
                    .data(null)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while deleting template id : {} Error : {}",
                    id,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to delete doctor template: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    private DoctorTemplate convertToEntity(DoctorTemplateDTO dto) {
        return DoctorTemplate.builder()
                .title(dto.getTitle())
                .createdAt(LocalDateTime.now())
                .clinicId(dto.getClinicId())
                .symptoms(dto.getSymptoms())

                // Mapping tests
                .tests(dto.getTests() != null
                        ? TestDetails.builder()
                            .selectedTests(dto.getTests().getSelectedTests())
                            .testReason(dto.getTests().getTestReason())
                            .build()
                        : null)

                // Mapping treatments
                .treatments(dto.getTreatments() != null
                ? TreatmentResponse.builder()
                    .selectedTestTreatment(dto.getTreatments().getSelectedTestTreatment() != null
                        ? dto.getTreatments().getSelectedTestTreatment()
                        : new ArrayList<>()
                    )
                    .generatedData(dto.getTreatments().getGeneratedData() != null
                        ? dto.getTreatments().getGeneratedData().entrySet().stream()
                            .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> TreatmentDetails.builder()
                                    .reason(entry.getValue().getReason())
                                    .frequency(entry.getValue().getFrequency())
                                    .sittings(entry.getValue().getSittings())
                                    .startDate(entry.getValue().getStartDate())
                                    .dates(entry.getValue().getDates() != null
                                        ? entry.getValue().getDates().stream()
                                            .map(d -> new Dates(d.getDate(), d.getSitting(),d.getStatus()))
                                            .collect(Collectors.toList())
                                        : null
                                    )
                                    .build()
                            ))
                        : new HashMap<>()
                    )
                    .build()
                : null
            )

                // Mapping follow-up
                .followUp(dto.getFollowUp() != null
                        ? FollowUpDetails.builder()
                            .durationValue(dto.getFollowUp().getDurationValue())
                            .durationUnit(dto.getFollowUp().getDurationUnit())
                            .nextFollowUpDate(dto.getFollowUp().getNextFollowUpDate())
                            .followUpNote(dto.getFollowUp().getFollowUpNote())
                            .build()
                        : null)

                // Mapping prescription
                .prescription(dto.getPrescription() != null
                        ? PrescriptionDetails.builder()
                            .medicines(dto.getPrescription().getMedicines() != null
                                    ? dto.getPrescription().getMedicines().stream()
                                        .map(m -> Medicines.builder()
                                                .id(m.getId() != null && !m.getId().isEmpty()
                                                        ? UUID.fromString(m.getId())
                                                        : UUID.randomUUID())
                                                .name(m.getName())
                                                .dose(m.getDose())
                                                .duration(m.getDuration())
                                                .durationUnit(m.getDurationUnit())
                                                .medicineType(m.getMedicineType())
                                                .note(m.getNote())
                                                .food(m.getFood())
                                                .remindWhen(m.getRemindWhen())
                                                .times(m.getTimes())
                                                .others(m.getOthers())
                                                .build())
                                        .collect(Collectors.toList())
                                    : new ArrayList<>())
                            .build()
                        : null)

                .build();
    }




    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateTemplateFallback")
    @Secured("ROLE_DOCTOR")
    public ResponseEntity<Response> updateTemplate(String id, DoctorTemplateDTO dto) {

        long startTime = System.currentTimeMillis();

        log.info("Entered updateTemplate() with templateId : {}, title : {}",
                id,
                dto.getTitle());

        try {

            log.debug("Fetching template from repository with id : {}", id);

            Optional<DoctorTemplate> existingTemplate = repository.findById(id);

            if (existingTemplate.isPresent()) {

                log.info("Template found with id : {}", id);

                String newTitleNormalized =
                        dto.getTitle().trim().replaceAll("\\s+", " ").toLowerCase();

                String currentTitleNormalized =
                        existingTemplate.get()
                                .getTitle()
                                .trim()
                                .replaceAll("\\s+", " ")
                                .toLowerCase();

                log.debug("Current Title : {}, New Title : {}",
                        currentTitleNormalized,
                        newTitleNormalized);

                if (!newTitleNormalized.equals(currentTitleNormalized)) {

                    log.info("Template title modified. Checking duplicate titles");

                    boolean titleExists = repository.findAll()
                            .stream()
                            .anyMatch(t ->
                                    !t.getId().equals(id)
                                            && t.getTitle() != null
                                            && t.getTitle()
                                            .trim()
                                            .replaceAll("\\s+", " ")
                                            .toLowerCase()
                                            .equals(newTitleNormalized));

                    if (titleExists) {

                        log.warn("Duplicate template title found : {}",
                                dto.getTitle());

                        Response conflictResponse = Response.builder()
                                .success(false)
                                .status(HttpStatus.CONFLICT.value())
                                .message("Another template already exists with the new title")
                                .data(null)
                                .build();

                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(conflictResponse);
                    }
                }

                DoctorTemplate updatedEntity = convertToEntity(dto);
                updatedEntity.setId(id);
                updatedEntity.setCreatedAt(
                        existingTemplate.get().getCreatedAt());

                log.debug("Saving updated template with id : {}", id);

                DoctorTemplate saved =
                        repository.save(updatedEntity);

                log.info("Template updated successfully with id : {}",
                        saved.getId());

                long executionTime =
                        System.currentTimeMillis() - startTime;

                log.info("updateTemplate() completed successfully in {} ms",
                        executionTime);

                Response response = Response.builder()
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .message("Doctor template updated successfully")
                        .data(saved)
                        .build();

                return ResponseEntity.ok(response);

            } else {

                log.warn("Template not found with id : {}", id);

                Response response = Response.builder()
                        .success(false)
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("Doctor template not found with ID: " + id)
                        .data(null)
                        .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }

        } catch (Exception e) {

            log.error("Exception occurred while updating template id : {}. Error : {}",
                    id,
                    e.getMessage(),
                    e);

            Response response = Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to update template : " + e.getMessage())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "searchTemplatesByTitleFallback")
    @Secured("ROLE_DOCTOR")
    public Response searchTemplatesByTitle(String keyword) {

        long startTime = System.currentTimeMillis();

        log.info("Entered searchTemplatesByTitle() with keyword : {}", keyword);

        try {

            if (keyword == null || keyword.trim().isEmpty()) {

                log.warn("Search keyword is null or empty");

                return Response.builder()
                        .success(false)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Keyword must not be empty")
                        .data(null)
                        .build();
            }

            String normalizedKeyword =
                    keyword.trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase();

            log.debug("Normalized keyword : {}", normalizedKeyword);

            log.debug("Fetching all templates from repository");

            List<DoctorTemplate> allTemplates =
                    repository.findAll();

            log.info("Retrieved {} templates from repository",
                    allTemplates.size());

            List<DoctorTemplate> filtered = allTemplates.stream()
                    .filter(t -> {

                        String keywordLower = normalizedKeyword;

                        boolean inTitle =
                                t.getTitle() != null &&
                                t.getTitle()
                                        .trim()
                                        .replaceAll("\\s+", " ")
                                        .toLowerCase()
                                        .equals(keywordLower);

                        return inTitle;

                    }).collect(Collectors.toList());

            log.info("Found {} matching templates for keyword : {}",
                    filtered.size(),
                    keyword);

            if (filtered.isEmpty()) {

                log.warn("No templates found with title : {}",
                        keyword);

                return Response.builder()
                        .success(false)
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No templates found with exact title: " + keyword)
                        .data(null)
                        .build();
            }

            long executionTime =
                    System.currentTimeMillis() - startTime;

            log.info("searchTemplatesByTitle() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Matching templates found")
                    .data(filtered)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while searching templates with keyword : {}. Error : {}",
                    keyword,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error during template search: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    private DoctorTemplateDTO convertToDto(DoctorTemplate entity) {
        return DoctorTemplateDTO.builder()
                .title(entity.getTitle())
                .clinicId(entity.getClinicId())
                .createdAt(entity.getCreatedAt())
                .symptoms(entity.getSymptoms())
                     .tests(entity.getTests() != null ?
                    TestDetailsDTO.builder()
                        .selectedTests(entity.getTests().getSelectedTests())
                        .testReason(entity.getTests().getTestReason())
                        .build()
                    : null)

                .treatments(
                        entity.getTreatments() != null
                            ? TreatmentResponseDTO.builder()
                                .selectedTestTreatment(entity.getTreatments().getSelectedTestTreatment())
                                .generatedData(
                                    entity.getTreatments().getGeneratedData() != null
                                        ? entity.getTreatments().getGeneratedData().entrySet().stream()
                                            .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> TreatmentDetailsDTO.builder()
                                                    .dates(
                                                        entry.getValue().getDates() != null
                                                            ? entry.getValue().getDates().stream()
                                                                .map(d -> new DatesDTO(d.getDate(), d.getSitting(),d.getStatus(), null))
                                                                .collect(Collectors.toList())
                                                            : null
                                                    )
                                                    .reason(entry.getValue().getReason())
                                                    .frequency(entry.getValue().getFrequency())
                                                    .sittings(entry.getValue().getSittings())
                                                    .startDate(entry.getValue().getStartDate())
                                                    .build()
                                            ))
                                        : null
                                )
                                .build()
                            : null
                    )




                .followUp(entity.getFollowUp() != null ?
                    FollowUpDetailsDTO.builder()
                        .durationValue(entity.getFollowUp().getDurationValue())
                        .durationUnit(entity.getFollowUp().getDurationUnit())
                        .nextFollowUpDate(entity.getFollowUp().getNextFollowUpDate())
                        .followUpNote(entity.getFollowUp().getFollowUpNote())
                        .build()
                    : null)

                .prescription(entity.getPrescription() != null ?
                	    PrescriptionDetailsDTO.builder()
                	        .medicines(entity.getPrescription().getMedicines().stream()
                	            .map(med -> MedicinesDTO.builder()
                	                    .id(med.getId() != null ? med.getId().toString() : null)
                	                    .name(med.getName())
                	                    .dose(med.getDose())
                	                    .duration(med.getDuration())
                	                    .durationUnit(med.getDurationUnit())
                	                    .medicineType(med.getMedicineType())
                	                    .note(med.getNote())
                	                    .food(med.getFood())
                	                    .remindWhen(med.getRemindWhen())
                	                    .times(med.getTimes())
                	                    .others(med.getOthers())
                	                    .build())
                	            .collect(Collectors.toList()))
                	        .build()
                	    : null)

                .build();
    }
  
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTemplatesByClinicIdFallback")
    @Secured("ROLE_DOCTOR")
    public Response getTemplatesByClinicId(String clinicId) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getTemplatesByClinicId() with clinicId : {}", clinicId);

        try {

            log.debug("Fetching templates from repository for clinicId : {}", clinicId);

            List<DoctorTemplate> templates = repository.findByClinicId(clinicId);

            log.info("Retrieved {} templates for clinicId : {}",
                    templates.size(),
                    clinicId);

            List<DoctorTemplateDTO> dtos = templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getTemplatesByClinicId() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor templates fetched successfully for clinicId: " + clinicId)
                    .data(dtos)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while fetching templates for clinicId : {}. Error : {}",
                    clinicId,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error fetching doctor templates by clinicId: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    
    @Override
    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTemplatesByClinicIdAndTitleFallback")
    @Secured("ROLE_DOCTOR")
    public Response getTemplatesByClinicIdAndTitle(String clinicId, String title) {

        long startTime = System.currentTimeMillis();

        log.info("Entered getTemplatesByClinicIdAndTitle() with clinicId : {}, title : {}",
                clinicId,
                title);

        try {

            if (clinicId == null || clinicId.trim().isEmpty()
                    || title == null || title.trim().isEmpty()) {

                log.warn("ClinicId or Title is empty. clinicId : {}, title : {}",
                        clinicId,
                        title);

                return Response.builder()
                        .success(false)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("ClinicId and Title must not be empty")
                        .data(null)
                        .build();
            }

            String normalizedTitle =
                    title.trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase();

            log.debug("Normalized title : {}", normalizedTitle);

            log.debug("Fetching templates from repository for clinicId : {}", clinicId);

            List<DoctorTemplate> clinicTemplates =
                    repository.findByClinicId(clinicId);

            log.info("Retrieved {} templates for clinicId : {}",
                    clinicTemplates.size(),
                    clinicId);

            List<DoctorTemplate> templates = clinicTemplates.stream()
                    .filter(t -> t.getTitle() != null
                            && t.getTitle()
                            .trim()
                            .replaceAll("\\s+", " ")
                            .toLowerCase()
                            .equals(normalizedTitle))
                    .collect(Collectors.toList());

            log.info("Found {} matching templates for clinicId : {} and title : {}",
                    templates.size(),
                    clinicId,
                    title);

            if (templates.isEmpty()) {

                log.warn("No templates found for clinicId : {} and title : {}",
                        clinicId,
                        title);

                return Response.builder()
                        .success(false)
                        .status(HttpStatus.OK.value())
                        .message("No templates found for clinicId: "
                                + clinicId
                                + " and exact title: "
                                + title)
                        .data(null)
                        .build();
            }

            List<DoctorTemplateDTO> dtos = templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("getTemplatesByClinicIdAndTitle() completed successfully in {} ms",
                    executionTime);

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor templates fetched successfully for clinicId and exact title")
                    .data(dtos)
                    .build();

        } catch (Exception e) {

            log.error("Exception occurred while fetching templates for clinicId : {} and title : {}. Error : {}",
                    clinicId,
                    title,
                    e.getMessage(),
                    e);

            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error fetching doctor templates: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    private Response buildRateLimitResponse(Exception ex) {
        return Response.builder()
                .success(false)
                .status(429)
                .message("Rate limit exceeded. Please try again later.")
                .data(null)
                .build();
    }

    public Response createTemplateFallback(DoctorTemplateDTO dto, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getTemplateByIdFallback(String id, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getAllTemplatesFallback(Exception ex) { return buildRateLimitResponse(ex); }
    public Response deleteTemplateFallback(String id, Exception ex) { return buildRateLimitResponse(ex); }
    public Response searchTemplatesByTitleFallback(String keyword, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getTemplatesByClinicIdFallback(String clinicId, Exception ex) { return buildRateLimitResponse(ex); }
    public Response getTemplatesByClinicIdAndTitleFallback(String clinicId, String title, Exception ex) { return buildRateLimitResponse(ex); }
   // public Response getTemplatesByClinicIdFallback(String clinicId, Exception ex) { return buildRateLimitResponse(ex); }
    
    public ResponseEntity<Response> updateTemplateFallback(String id, DoctorTemplateDTO dto, Exception ex) {
        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
    }

}