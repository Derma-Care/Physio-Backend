package com.dermacare.doctorservice.service;

import org.springframework.http.ResponseEntity;
import com.dermacare.doctorservice.dto.DoctorTemplateDTO;
import com.dermacare.doctorservice.dto.Response;

public interface DoctorTemplateService {

    /**
     * Creates a new DoctorTemplate from the given DTO.
     * @param dto the doctor template data
     * @return ResponseEntity with status and created data
     */
    Response  createTemplate(DoctorTemplateDTO dto);

    /**
     * Retrieves a DoctorTemplate by its ID.
     * @param id the template ID
     * @return ResponseEntity with the found template or error message
     */
    Response  getTemplateById(String id);

    /**
     * Fetches all available DoctorTemplates.
     * @return ResponseEntity with list of templates
     */
   Response  getAllTemplates();

    /**
     * Deletes a DoctorTemplate by its ID.
     * @param id the template ID
     * @return ResponseEntity with deletion status
     */
   Response deleteTemplate(String id);

    /**
     * Updates a DoctorTemplate by ID with provided DTO data.
     * @param id the template ID
     * @param dto the updated template data
     * @return ResponseEntity with update status and updated data
     */
    ResponseEntity<Response> updateTemplate(String id, DoctorTemplateDTO dto);

	Response searchTemplatesByTitle(String keyword);

	Response getTemplatesByClinicId(String clinicId);

	Response getTemplatesByClinicIdAndTitle(String clinicId, String title);
}
