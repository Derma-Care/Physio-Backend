package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.dto.PhysiotherapyRecordTemplateDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.PhysiotherapyRecordTemplateService;

@RestController
@RequestMapping("/physiotherapy-doctor")
public class PhysiotherapyRecordTemplateController {

	@Autowired
	private PhysiotherapyRecordTemplateService service;

	// ✅ CREATE
	@PostMapping("/physiotherapy-record-template/create")
	public ResponseEntity<Response> create(@RequestBody PhysiotherapyRecordTemplateDTO dto) {

		Response response = service.create(dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/physiotherapy-record-template/template/{clinicId}/{templateRecordId}")
	public ResponseEntity<Response> getTemplate(@PathVariable String clinicId, @PathVariable String templateRecordId) {

		Response response = service.getTemplateByClinicIdAndTemplateId(clinicId, templateRecordId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@GetMapping("/physiotherapy-record-template/template/list/{clinicId}")
	public ResponseEntity<Response> getTemplates(@PathVariable String clinicId) {

		Response response = service.getTemplatesByClinicId(clinicId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET BY ID
	@GetMapping("/physiotherapy-record-template/getById/{id}")
	public ResponseEntity<Response> getById(@PathVariable String id) {

		Response response = service.getById(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET ALL
	@GetMapping("/physiotherapy-record-template/getAll")
	public ResponseEntity<Response> getAll() {

		Response response = service.getAll();
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ UPDATE
	@PutMapping("/physiotherapy-record-template/update/{id}")
	public ResponseEntity<Response> update(@PathVariable String id, @RequestBody PhysiotherapyRecordTemplateDTO dto) {

		Response response = service.update(id, dto);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ DELETE
	@DeleteMapping("/physiotherapy-record-template/delete/{id}")
	public ResponseEntity<Response> delete(@PathVariable String id) {

		Response response = service.delete(id);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET BY MULTIPLE FIELDS
	@GetMapping("/physiotherapy-record-template/getByMultipleFields/{clinicId}/{branchId}/{bookingId}/{templateRecordId}")
	public ResponseEntity<Response> getByMultipleFields(@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String bookingId, @PathVariable String templateRecordId) {

		Response response = service.getByMultipleFields(clinicId, branchId, bookingId, templateRecordId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET WITHOUT TEMPLATE RECORD ID
	@GetMapping("/physiotherapy-record-template/getByWithoutTemplateRecordId/{clinicId}/{branchId}/{bookingId}")
	public ResponseEntity<Response> getByWithoutTemplateRecordId(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String bookingId) {

		Response response = service.getByWithoutTherapistRecordId(clinicId, branchId, bookingId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}

	// ✅ GET CALCULATIONS
	@GetMapping("/physiotherapy-record-template/getCalculations/{clinicId}/{branchId}/{bookingId}")
	public ResponseEntity<Response> getCalculations(@PathVariable String clinicId, @PathVariable String branchId,
			@PathVariable String bookingId) {

		return service.getCalculations(clinicId, branchId, bookingId);
	}

	// ✅ GET BY CLINIC + BRANCH + BOOKING
	@GetMapping("/physiotherapy-record-template/getByClinicBranchAndBooking/{clinicId}/{branchId}/{bookingId}")
	public ResponseEntity<Response> getByClinicBranchAndBooking(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String bookingId) {

		Response response = service.getByClinicBranchAndBooking(clinicId, branchId, bookingId);

		return ResponseEntity.status(response.getStatus()).body(response);
	}
}