package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;

@RestController
@RequestMapping("/physiotherapy-doctor")
public class PhysiotherapyDoctorController {

	@Autowired
	PhysiotherapyDoctorDetails physiotherapyDoctorDetails;

	@GetMapping("/getTherapistWithRequiredFileds/{clinicId}/{branchId}")
	public ResponseEntity<Response> getTherapistDataFrom(@PathVariable String clinicId, @PathVariable String branchId) {
		Response response = physiotherapyDoctorDetails.getPhysioDoctorDetails(clinicId, branchId);
		return ResponseEntity.status(response.getStatus()).body(response);

	}
}
