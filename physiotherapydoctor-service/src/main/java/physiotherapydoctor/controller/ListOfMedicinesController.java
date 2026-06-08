package physiotherapydoctor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.ListOfMedicinesDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.ListOfMedicinesService;

@RestController
@RequestMapping("/physiotherapy-doctor")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
@RequiredArgsConstructor
public class ListOfMedicinesController {

	private final ListOfMedicinesService service;

	// ✅ Create medicine list
	@PostMapping("/createListOfMedicines")
	public ResponseEntity<Response> createListOfMedicines(@RequestBody ListOfMedicinesDTO dto) {
		Response response = service.create(dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Update medicine list
	@PutMapping("/updateListOfMedicinesById/{id}")
	public ResponseEntity<Response> updateListOfMedicines(@PathVariable String id,
			@RequestBody ListOfMedicinesDTO dto) {
		Response response = service.update(id, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Delete medicine list by ID
	@DeleteMapping("/deleteListOfMedicinesById/{id}")
	public ResponseEntity<Response> deleteListOfMedicines(@PathVariable String id) {
		Response response = service.delete(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Get medicine list by ID
	@GetMapping("/getListOfMedicinesById/{id}")
	public ResponseEntity<Response> getByListOfMedicinesId(@PathVariable String id) {
		Response response = service.getById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Get all medicine lists
	@GetMapping("/getAllListOfMedicines")
	public ResponseEntity<Response> getAllListOfMedicines() {
		Response response = service.getAll();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Get medicine lists by clinic ID
	@GetMapping("/getListOfMedicinesByClinicId/{clinicId}")
	public ResponseEntity<Response> getListOfMedicinesByClinicId(@PathVariable String clinicId) {
		Response response = service.getByClinicId(clinicId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ Add or search medicine in a clinic list
	@PostMapping("/addOrSearchListOfMedicine")
	public ResponseEntity<Response> addOrSearchMedicine(@RequestBody ListOfMedicinesDTO dto) {
		Response response = service.addOrSearchMedicine(dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
}