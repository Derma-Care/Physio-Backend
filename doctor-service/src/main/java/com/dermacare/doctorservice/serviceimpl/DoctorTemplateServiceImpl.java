package com.dermacare.doctorservice.serviceimpl;

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
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.DatesDTO;
import com.dermacare.doctorservice.dto.DoctorTemplateDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.dto.TreatmentDetailsDTO;
import com.dermacare.doctorservice.dto.TreatmentResponseDTO;
import com.dermacare.doctorservice.model.Dates;
import com.dermacare.doctorservice.model.DoctorTemplate;
import com.dermacare.doctorservice.model.FollowUpDetails;
import com.dermacare.doctorservice.model.Medicines;
import com.dermacare.doctorservice.model.PrescriptionDetails;
import com.dermacare.doctorservice.model.SymptomDetails;
import com.dermacare.doctorservice.model.TestDetails;
import com.dermacare.doctorservice.model.TreatmentDetails;
import com.dermacare.doctorservice.model.TreatmentResponse;
import com.dermacare.doctorservice.repository.DoctorTemplateRepository;
import com.dermacare.doctorservice.service.DoctorTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorTemplateServiceImpl implements DoctorTemplateService {

    private final DoctorTemplateRepository repository;

    @Override
    public Response createTemplate(DoctorTemplateDTO dto) {
        try {
            String normalizedTitle = dto.getTitle().trim().replaceAll("\\s+", " ").toLowerCase();

            Optional<DoctorTemplate> existingTemplateOpt = repository.findAll().stream()
                    .filter(t -> t.getTitle() != null &&
                            t.getTitle().trim().replaceAll("\\s+", " ").toLowerCase().equals(normalizedTitle))
                    .findFirst();

            DoctorTemplate savedTemplate;

            if (existingTemplateOpt.isPresent()) {
               
                DoctorTemplate existingTemplate = existingTemplateOpt.get();

                // Keep the title unchanged
                dto.setTitle(existingTemplate.getTitle());

                // Map updated details from DTO to entity
                DoctorTemplate updatedEntity = convertToEntity(dto);
                updatedEntity.setId(existingTemplate.getId()); // keep same ID
                updatedEntity.setTitle(existingTemplate.getTitle()); // keep original title

                savedTemplate = repository.save(updatedEntity);

                return Response.builder()
                        .success(true)
                        .status(HttpStatus.OK.value())
                        .message("Existing template updated successfully")
                        .data(savedTemplate)
                        .build();
            } else {
                // ✅ Create new template
                savedTemplate = repository.save(convertToEntity(dto));

                return Response.builder()
                        .success(true)
                        .status(HttpStatus.CREATED.value())
                        .message("Doctor template created successfully")
                        .data(savedTemplate)
                        .build();
            }

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Failed to create/update doctor template: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Override
    public Response getTemplateById(String id) {
        Optional<DoctorTemplate> template = repository.findById(id);
        if (template.isPresent()) {
            DoctorTemplateDTO dto = convertToDto(template.get());
            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor template found")
                    .data(dto)
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Doctor template not found with ID: " + id)
                    .data(null)
                    .build();
        }
    }


    @Override
    public Response getAllTemplates() {
        List<DoctorTemplate> templates = repository.findAll();
        List<DoctorTemplateDTO> dtos = templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return Response.builder()
                .success(true)
                .status(HttpStatus.OK.value())
                .message("All doctor templates fetched successfully")
                .data(dtos)
                .build();
    }


    @Override
    public Response deleteTemplate(String id) {
        Optional<DoctorTemplate> existing = repository.findById(id);
        if (existing.isPresent()) {
            repository.deleteById(id);
            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor template deleted successfully")
                    .data(null)
                    .build();
        } else {
            return Response.builder()
                    .success(false)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Doctor template not found with ID: " + id)
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


                // Mapping symptoms
//                .symptoms(dto.getSymptoms() != null
//                        ? SymptomDetails.builder()
//                            .symptomDetails(dto.getSymptoms().getSymptomDetails())
//                            .doctorObs(dto.getSymptoms().getDoctorObs())
//                            .diagnosis(dto.getSymptoms().getDiagnosis())
//                            .duration(dto.getSymptoms().getDuration())
//                          .reports(encodeFileToBase64(dto.getSymptoms().getReports())) // <-- Base64 here
//                            .build()
//                        : null)

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
    public ResponseEntity<Response> updateTemplate(String id, DoctorTemplateDTO dto) {
        Optional<DoctorTemplate> existingTemplate = repository.findById(id);

        if (existingTemplate.isPresent()) {
            String newTitleNormalized = dto.getTitle().trim().replaceAll("\\s+", " ").toLowerCase();

            // Check if title is changed
            String currentTitleNormalized = existingTemplate.get().getTitle().trim().replaceAll("\\s+", " ").toLowerCase();

            if (!newTitleNormalized.equals(currentTitleNormalized)) {
                boolean titleExists = repository.findAll().stream()
                    .anyMatch(t -> !t.getId().equals(id) && t.getTitle() != null &&
                            t.getTitle().trim().replaceAll("\\s+", " ").toLowerCase().equals(newTitleNormalized));

                if(titleExists) {
                    Response conflictResponse = Response.builder()
                            .success(false)
                            .status(HttpStatus.CONFLICT.value())
                            .message("Another template already exists with the new title")
                            .data(null)
                            .build();
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
                }
            }

            // Update entity
            DoctorTemplate updatedEntity = convertToEntity(dto);
            updatedEntity.setId(id);  
            updatedEntity.setCreatedAt(existingTemplate.get().getCreatedAt()); // preserve original creation time

            DoctorTemplate saved = repository.save(updatedEntity);

            Response response = Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor template updated successfully")
                    .data(saved)
                    .build();
            return ResponseEntity.ok(response);
        } else {
            Response response = Response.builder()
                    .success(false)
                    .status(HttpStatus.NOT_FOUND.value())
                    .message("Doctor template not found with ID: " + id)
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Override
    public Response searchTemplatesByTitle(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Response.builder()
                        .success(false)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Keyword must not be empty")
                        .data(null)
                        .build();
            }

            String normalizedKeyword = keyword.trim().replaceAll("\\s+", " ").toLowerCase();

            List<DoctorTemplate> allTemplates = repository.findAll();
            List<DoctorTemplate> filtered = allTemplates.stream()
                .filter(t -> {
                    String keywordLower = normalizedKeyword;

                    // ✅ Title: exact match only
                    boolean inTitle = t.getTitle() != null &&
                            t.getTitle().trim().replaceAll("\\s+", " ").toLowerCase().equals(keywordLower);

                    // 🔁 Other fields (optional: you can also change these to exact match if needed)
//                    boolean inSymptoms = t.getSymptoms() != null &&
//                            (
////                                    (t.getSymptoms().getSymptomDetails() != null &&
////                                            t.getSymptoms().getSymptomDetails().toLowerCase().contains(keywordLower)) ||
//                                    (t.getSymptoms().getDiagnosis() != null &&
//                                            t.getSymptoms().getDiagnosis().toLowerCase().contains(keywordLower)) ||\
////                                    (t.getSymptoms().getDoctorObs() != null &&
////                                            t.getSymptoms().getDoctorObs().toLowerCase().contains(keywordLower))
//                            );

                    boolean inTests = t.getTests() != null &&
                            t.getTests().getSelectedTests() != null &&
                            t.getTests().getSelectedTests().stream()
                                    .anyMatch(test -> test != null && test.toLowerCase().contains(keywordLower));

                    boolean inTreatments = t.getTreatments() != null && (
                    	    // Check in selectedTreatment list
                    	    (t.getTreatments().getSelectedTestTreatment() != null &&
                    	        t.getTreatments().getSelectedTestTreatment().stream()
                    	            .anyMatch(sel -> sel != null && sel.toLowerCase().contains(keywordLower))
                    	    )
                    	    ||
                    	    // Check in generatedData map
                    	    (t.getTreatments().getGeneratedData() != null &&
                    	        t.getTreatments().getGeneratedData().entrySet().stream()
                    	            .anyMatch(entry -> {
                    	                TreatmentDetails td = entry.getValue();
                    	                return
                    	                    (entry.getKey() != null && entry.getKey().toLowerCase().contains(keywordLower)) ||
                    	                    (td.getReason() != null && td.getReason().toLowerCase().contains(keywordLower)) ||
                    	                    (td.getFrequency() != null && td.getFrequency().toLowerCase().contains(keywordLower)) ||
                    	                    (String.valueOf(td.getSittings()).toLowerCase().contains(keywordLower)) ||
                    	                    (td.getStartDate() != null && td.getStartDate().toLowerCase().contains(keywordLower)) ||
                    	                    (td.getDates() != null && td.getDates().stream()
                    	                        .anyMatch(d ->
                    	                            (d.getDate() != null && d.getDate().toLowerCase().contains(keywordLower)) ||
                    	                            String.valueOf(d.getSitting()).toLowerCase().contains(keywordLower)
                    	                        )
                    	                    );
                    	            })
                    	    )
                    	);

                    boolean inMedicines = t.getPrescription() != null &&
                            t.getPrescription().getMedicines() != null &&
                            t.getPrescription().getMedicines().stream()
                                    .anyMatch(med -> med.getName() != null &&
                                            med.getName().toLowerCase().contains(keywordLower));

                    // ✅ Match only on exact title
                    return inTitle;
                })
                .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("No templates found with exact title: " + keyword)
                        .data(null)
                        .build();
            }

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Matching templates found")
                    .data(filtered)
                    .build();

        } catch (Exception e) {
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
                
//                .symptoms(entity.getSymptoms() != null ? 
//                    com.dermacare.doctorservice.dto.SymptomDetailsDTO.builder()
//                        .symptomDetails(entity.getSymptoms().getSymptomDetails())
//                        .doctorObs(entity.getSymptoms().getDoctorObs())
//                        .diagnosis(entity.getSymptoms().getDiagnosis())
//                        .duration(entity.getSymptoms().getDuration())
//                       .reports(encodeFileToBase64(entity.getSymptoms().getReports())) // <-- Base64 here
//                        .build()
//                    : null)

                .tests(entity.getTests() != null ?
                    com.dermacare.doctorservice.dto.TestDetailsDTO.builder()
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
                                                                .map(d -> new DatesDTO(d.getDate(), d.getSitting(),d.getStatus()))
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
                    com.dermacare.doctorservice.dto.FollowUpDetailsDTO.builder()
                        .durationValue(entity.getFollowUp().getDurationValue())
                        .durationUnit(entity.getFollowUp().getDurationUnit())
                        .nextFollowUpDate(entity.getFollowUp().getNextFollowUpDate())
                        .followUpNote(entity.getFollowUp().getFollowUpNote())
                        .build()
                    : null)

                .prescription(entity.getPrescription() != null ?
                    com.dermacare.doctorservice.dto.PrescriptionDetailsDTO.builder()
                        .medicines(entity.getPrescription().getMedicines().stream()
                            .map(med -> com.dermacare.doctorservice.dto.MedicinesDTO.builder()
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
    public Response getTemplatesByClinicId(String clinicId) {
        try {
            List<DoctorTemplate> templates = repository.findByClinicId(clinicId);

            List<DoctorTemplateDTO> dtos = templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor templates fetched successfully for clinicId: " + clinicId)
                    .data(dtos)
                    .build();

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error fetching doctor templates by clinicId: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
    
    public String encodeFileToBase64(String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Error encoding file to Base64", e);
        }
    }
    
    @Override
    public Response getTemplatesByClinicIdAndTitle(String clinicId, String title) {
        try {
            if (clinicId == null || clinicId.trim().isEmpty() ||
                title == null || title.trim().isEmpty()) {
                return Response.builder()
                        .success(false)
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("ClinicId and Title must not be empty")
                        .data(null)
                        .build();
            }

            String normalizedTitle = title.trim().replaceAll("\\s+", " ").toLowerCase();

            List<DoctorTemplate> templates = repository.findByClinicId(clinicId)
                    .stream()
                    .filter(t -> t.getTitle() != null &&
                            t.getTitle().trim().replaceAll("\\s+", " ").toLowerCase()
                                    .equals(normalizedTitle)) 
                    .collect(Collectors.toList());
            if (templates.isEmpty()) {
                return Response.builder()
                        .success(false)
                        .status(HttpStatus.OK.value())
                        .message("No templates found for clinicId: " + clinicId + " and exact title: " + title)
                        .data(null)
                        .build();
            }

            List<DoctorTemplateDTO> dtos = templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return Response.builder()
                    .success(true)
                    .status(HttpStatus.OK.value())
                    .message("Doctor templates fetched successfully for clinicId and exact title")
                    .data(dtos)
                    .build();

        } catch (Exception e) {
            return Response.builder()
                    .success(false)
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error fetching doctor templates: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }


}