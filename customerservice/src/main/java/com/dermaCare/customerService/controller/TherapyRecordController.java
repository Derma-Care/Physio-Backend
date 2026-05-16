package com.dermaCare.customerService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dermaCare.customerService.dto.TherapyRecordDTO;
import com.dermaCare.customerService.service.TherapyRecordService;


@RestController
@RequestMapping("/customer/therapy-records")
@CrossOrigin(origins = "*")
public class TherapyRecordController {

	 @Autowired
	    private TherapyRecordService service;

	    @PostMapping("/create")
	    public ResponseEntity<?> createTherapyRecord(
	            @RequestBody TherapyRecordDTO dto) {

	        return service.createTherapyRecord(dto);
	    }

	    @GetMapping("/getAll")
	    public ResponseEntity<?> getAllTherapyRecords() {

	        return service.getAllTherapyRecords();
	    }

	    @GetMapping("/getById/{id}")
	    public ResponseEntity<?> getTherapyRecordById(
	            @PathVariable String id) {

	        return service.getTherapyRecordById(id);
	    }

	    @PutMapping("/update/{id}")
	    public ResponseEntity<?> updateTherapyRecord(
	            @PathVariable String id,
	            @RequestBody TherapyRecordDTO dto) {

	        return service.updateTherapyRecord(id, dto);
	    }

	    @DeleteMapping("/delete/{id}")
	    public ResponseEntity<?> deleteTherapyRecord(
	            @PathVariable String id) {

	        return service.deleteTherapyRecord(id);
	    }
	    
	 // GET BY CLINIC + BRANCH + PATIENT

	    @GetMapping("/getByClinicBranchPatient/{clinicId}/{branchId}/{patientId}")
	    public ResponseEntity<?>
	    getByClinicBranchAndPatient(
	    		   @PathVariable String clinicId,
	    		   @PathVariable String branchId,
	    		   @PathVariable String patientId) {

	        return service.getByClinicBranchAndPatient(
	                        clinicId,
	                        branchId,
	                        patientId);
	    }
	    
	 // GET BY CLINIC + BRANCH + PATIENT + THERAPY RECORD ID

	    @GetMapping("/getByClinicBranchPatientTherapyRecord/{clinicId}/{branchId}/{patientId}/{therapyRecordId}")
	    public ResponseEntity<?>
	    getByClinicBranchPatientAndTherapyRecordId(
	    		 @PathVariable String clinicId,
	    		 @PathVariable String branchId,
	    		 @PathVariable String patientId,
	    		 @PathVariable String therapyRecordId) {

	        return service.getByClinicBranchPatientAndTherapyRecordId(
	                        clinicId,
	                        branchId,
	                        patientId,
	                        therapyRecordId);
	    }
	    
	    
}

