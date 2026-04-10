package com.clinicadmin.controller;

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

import com.clinicadmin.dto.PreProcedureFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.PreProcedureFormService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PreProcedureFormController {
	@Autowired
	PreProcedureFormService preProcedureFormService;

	@PostMapping("/addPreProcedureForm/{hospitalId}/{subServiceId}")
	public ResponseEntity<Response> addPreProcedureForm(@PathVariable String hospitalId,
			@PathVariable String subServiceId, @RequestBody PreProcedureFormDTO dto) {
		Response response = preProcedureFormService.addPreProcedureForm(hospitalId, subServiceId, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("getPreProcedureFormByHospitalIdAndPreProcedureId/{hospitalId}/{preProcedureId}")
	public ResponseEntity<Response> addPreProcedureFormByHospitalIdPreProcedureId(@PathVariable String hospitalId,
			@PathVariable	String preProcedureId) {
		Response response = preProcedureFormService.getPreProcedureFormBypreProcedureFormId(hospitalId, preProcedureId);
		return ResponseEntity.status(response.getStatus()).body(response);

	}

	@GetMapping("/getAllPreProcedureForms")
	public ResponseEntity<Response> getAllPreProcedureForms() {
		Response response = preProcedureFormService.getAllPreProcedureForm();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/update-preprocedure-forms/{hospitalId}/{preProcedureFormId}")
	public ResponseEntity<Response> updatePreProcedureForm(@PathVariable String hospitalId,
			@PathVariable String preProcedureFormId, @RequestBody PreProcedureFormDTO dto) {

		Response response = preProcedureFormService.updatePreProcedureForm(hospitalId, preProcedureFormId, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// Delete
	@DeleteMapping("/delete-preprocedure-form/{hospitalId}/{preProcedureFormId}")
	public ResponseEntity<Response> deletePreProcedureForm(@PathVariable String hospitalId,
			@PathVariable String preProcedureFormId) {
		Response response = preProcedureFormService.deletePreProcedureForm(hospitalId, preProcedureFormId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

}
