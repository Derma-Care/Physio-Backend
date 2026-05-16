package com.dermaCare.customerService.service;

import org.springframework.http.ResponseEntity;

import com.dermaCare.customerService.dto.TherapyRecordDTO;

public interface TherapyRecordService {
	 ResponseEntity<?> createTherapyRecord(TherapyRecordDTO dto);

	    ResponseEntity<?> getAllTherapyRecords();

	    ResponseEntity<?> getTherapyRecordById(String id);

	    ResponseEntity<?> updateTherapyRecord(String id, TherapyRecordDTO dto);

	    ResponseEntity<?> deleteTherapyRecord(String id);

	    ResponseEntity<?> getByClinicBranchAndPatient(
	            String clinicId,
	            String branchId,
	            String patientId);

	    ResponseEntity<?> getByClinicBranchPatientAndTherapyRecordId(
	            String clinicId,
	            String branchId,
	            String patientId,
	            String therapyRecordId);
}