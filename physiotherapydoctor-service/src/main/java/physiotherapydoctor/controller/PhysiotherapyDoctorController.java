package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;

@RestController
@RequestMapping("/physiotherapy-doctor")
public class PhysiotherapyDoctorController {

	@Autowired
	PhysiotherapyDoctorDetails doctorService;

	@GetMapping("/getTherapistWithRequiredFileds/{clinicId}/{branchId}")
	public ResponseEntity<Response> getTherapistDataFrom(@PathVariable String clinicId, @PathVariable String branchId) {
		Response response = doctorService
				.getPhysioDoctorDetails(clinicId, branchId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}

	@PutMapping("/update-password/{username}")
	public Response updatePassword(@PathVariable String username,
			@RequestBody ChangeDoctorPasswordDTO updatePasswordDTO) {
		Response response = doctorService.changePassword(username, updatePasswordDTO);
//    	System.out.println(response);
		return response;
	}

	@PostMapping("/login")
	public ResponseEntity<Response> login(@Valid @RequestBody DoctorLoginDTO dto) {
		Response res = doctorService.login(dto);
		if (res != null) {
			return ResponseEntity.status(res.getStatus()).body(res);
		}
		return null;
	}

	@PutMapping("/update-availability/{doctorId}")

	public Response updateDoctorAvailability(@PathVariable String doctorId,
			@RequestBody DoctorAvailabilityStatusDTO availabilityDTO) {

		return doctorService.updateDoctorAvailability(doctorId, availabilityDTO);

	}

	// New Apis

	@GetMapping("/getAllDoctors")
	public ResponseEntity<?> getAllDoctors() {
		return doctorService.getAllDoctors();
	}

	@GetMapping("/getDoctorById/{id}")
	public ResponseEntity<?> getDoctorById(@PathVariable String id) {
		return doctorService.getDoctorById(id);
	}

	@GetMapping("/getDoctorByClinicAndDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> getDoctorByClinicAndDoctorId(@PathVariable String clinicId,
			@PathVariable String doctorId) {
		return doctorService.getDoctorByClinicAndDoctorId(clinicId, doctorId);
	}

	@GetMapping("/getDoctorsByHospitalById/{clinicId}")
	public ResponseEntity<?> getDoctorsByHospitalById(@PathVariable String clinicId) {
		return doctorService.getDoctorsByHospitalById(clinicId);
	}

	@GetMapping("/getDoctorsBySubServiceId/{hsptlId}/{subServiceId}")
	public ResponseEntity<?> getDoctorsBySubServiceId(@PathVariable String hsptlId, @PathVariable String subServiceId) {
		return doctorService.getDoctorsBySubServiceId(hsptlId, subServiceId);
	}

//	@GetMapping("/getAllDoctorsBySubServiceId/{subServiceId}")
//	public ResponseEntity<?> getAllDoctorsBySubServiceId(@PathVariable String subServiceId) {
//		return doctorService.getAllDoctorsBySubServiceId(subServiceId);
//	}

	@GetMapping("/getDoctorFutureAppointments/{doctorId}")
	public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId) {
		return doctorService.getDoctorFutureAppointments(doctorId);
	}

	@GetMapping("/getDiseasesByHospitalId/{hospitalId}")
	public ResponseEntity<Response> getDiseases(@PathVariable String hospitalId) {
		return doctorService.getDiseasesFromClinicAdmin(hospitalId);
	}

	@GetMapping("/getLabTestsByHospitalId/{hospitalId}")
	public ResponseEntity<Response> getLabTests(@PathVariable String hospitalId) {

		return doctorService.getLabTestsFromClinicAdmin(hospitalId);
	}
}
