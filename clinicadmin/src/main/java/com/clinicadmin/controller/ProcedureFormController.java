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

import com.clinicadmin.dto.ProcedureFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.ProcedureFormService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ProcedureFormController {

	@Autowired
	ProcedureFormService procedureFormService;

	@PostMapping("/addProcedureForm/{hospitalId}/{subServiceId}")
	public ResponseEntity<Response> addProcedure(@PathVariable String hospitalId, @PathVariable String subServiceId,
			@RequestBody ProcedureFormDTO dto) {
		Response response = procedureFormService.addProcedureForm(hospitalId, subServiceId, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/getProcedureFormByHospitalIdAndProcedureId/{hospitalId}/{procedureId}")
	public ResponseEntity<Response> getProcedureByHospitalIdAndProcedureId(@PathVariable String hospitalId,
			@PathVariable String procedureId) {
		Response response = procedureFormService.getProcedureById(hospitalId, procedureId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/getAllProcedureForms")
	public ResponseEntity<Response> getAllProcedures() {
		Response response = procedureFormService.getAllProcedureForms();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/update-procedureForm/{hospitalId}/{procedureId}")
	public ResponseEntity<Response> updateProcedure(@PathVariable String hospitalId, @PathVariable String procedureId,
			@RequestBody ProcedureFormDTO dto) {
		Response response = procedureFormService.updateProcedureForm(hospitalId, procedureId, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@DeleteMapping("/delete-procedureForm/{hospitalId}/{procedureId}")
	public ResponseEntity<Response> deleteProcedure(@PathVariable String hospitalId, @PathVariable String procedureId) {
		Response response = procedureFormService.deleteProcedureForm(hospitalId, procedureId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}
