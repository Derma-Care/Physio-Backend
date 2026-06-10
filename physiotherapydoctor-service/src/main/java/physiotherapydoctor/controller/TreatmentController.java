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

import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.service.TreatmentService;

@RestController
@RequestMapping("/physiotherapy-doctor")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class TreatmentController {

	@Autowired
	private TreatmentService treatmentService;

	@PostMapping("/addTreatment")
	public ResponseEntity<Response> addTreatment(@RequestBody TreatmentDTO dto) {
		return treatmentService.addTreatment(dto);
	}

	@GetMapping("/getAllTreatments")
	public ResponseEntity<Response> getAllTreatments() {
		return treatmentService.getAllTreatments();
	}

	@GetMapping("/getTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> getTreatmentById(@PathVariable String id, @PathVariable String hospitalId) {
		return treatmentService.getTreatmentById(id, hospitalId);
	}

	@DeleteMapping("/deleteTreatmentByIdAndHospitalId/{id}/{hospitalId}")
	public ResponseEntity<Response> deleteTreatment(@PathVariable String id, @PathVariable String hospitalId) {
		return treatmentService.deleteTreatmentById(id, hospitalId);
	}

	@PutMapping("/updateTreatmentByIdAndHospital/{id}/{hospitalId}")
	public ResponseEntity<Response> updateTreatment(@PathVariable String id, @PathVariable String hospitalId,
			@RequestBody TreatmentDTO dto) {
		return treatmentService.updateTreatmentById(id, hospitalId, dto);
	}
}
