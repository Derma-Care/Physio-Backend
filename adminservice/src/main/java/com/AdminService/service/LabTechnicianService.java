package com.AdminService.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.AdminService.dto.LabTechnicianRequestDTO;
import com.AdminService.util.ResponseStructure;

public interface LabTechnicianService {

    ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> addLabTechnician(LabTechnicianRequestDTO dto);

    ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians();

    ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnicianById(String id, LabTechnicianRequestDTO dto);

    ResponseEntity<ResponseStructure<String>> deleteLabTechnicianById(String id);

    ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(String clinicId);

    ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(String clinicId, String technicianId);

    ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(String clinicId, String branchId);
}
