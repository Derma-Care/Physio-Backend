package physiotherapydoctor.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.ClinicInfoDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.DoctorsDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.dto.TherapistRecordDTO;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.dto.VitalsDTO;

@FeignClient(name = "clinicadmin")
public interface ClinicAdminFeign {

	@PostMapping("/clinic-admin/doctorLogin")
	public Response login(DoctorLoginDTO loginDTO);

	@PutMapping("/clinic-admin/update-password/{username}")
	Response changePassword(@PathVariable("username") String username, @RequestBody ChangeDoctorPasswordDTO updateDTO);

	@PostMapping("/clinic-admin/doctorId/{doctorId}/availability")
	Response updateDoctorAvailability(@PathVariable("doctorId") String doctorId,
			@RequestBody DoctorAvailabilityStatusDTO availabilityDTO);

	// ✅ Get booking by bookingId
	@GetMapping("/clinic-admin/getBookingById/{bookingId}")
	ResponseStructure<BookingResponse> getBookingById(@PathVariable("bookingId") String bookingId);

	// ✅ Update booking status
	@PutMapping("/clinic-admin/updateAppointmentBasedOnBookingId")
	ResponseEntity<?> updateAppointment(@RequestBody BookingResponse bookingResponse);

	@GetMapping("/clinic-admin/getByPatientIdAndBookingId/{patientId}/{bookingId}")
	ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(@PathVariable String patientId,
			@PathVariable String bookingId);

	@GetMapping("/clinic-admin/getRecordBySession/{clinicId}/{branchId}/{bookingId}/{patientId}/{sessionId}")
	ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySession(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String bookingId, @PathVariable String patientId,
			@PathVariable String sessionId);

	// ================= GET Threapistdata by clinicId and Branch Id with required
	// field=================
	@GetMapping("/clinic-admin/getTherapistWithRequiredFileds/{clinicId}/{branchId}")
	public ResponseEntity<Response> getTherapistWithRequiredFileds(@PathVariable String clinicId,
			@PathVariable String branchId);

	@GetMapping("/clinic-admin/getCompletedTherapyRecord/{clinicId}/{branchId}/{therapistRecordId}/{sessionId}")
	ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecord(@PathVariable String clinicId,
			@PathVariable String branchId, @PathVariable String therapistRecordId, @PathVariable String sessionId);

	@PutMapping("/clinic-admin/updateDoctor/{doctorId}")
	public ResponseEntity<Response> updateDoctorById(@PathVariable String doctorId, @RequestBody DoctorsDTO dto);

//	======================From doctor service========================

//	--------------------------------- TreatmentFeignClient from clinic admin  -------------------------------------
	@PostMapping("/clinic-admin/treatment/addTreatment")
	public ResponseEntity<Response> addTreatment(@RequestBody TreatmentDTO dto);

	@GetMapping("/clinic-admin/treatment/getAllTreatments")
	public ResponseEntity<Response> getAllTreatments();

	@GetMapping("/clinic-admin/treatment/getTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> getTreatmentById(@PathVariable String id, @PathVariable String hospitalId);

	@DeleteMapping("/clinic-admin/treatment/deleteTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> deleteTreatmentById(@PathVariable String id, @PathVariable String hospitalId);

	@PutMapping("/clinic-admin/treatment/updateTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> updateTreatmentById(@PathVariable String id, @PathVariable String hospitalId,
			@RequestBody TreatmentDTO dto);

	@GetMapping("/clinic-admin/doctors")
	public ResponseEntity<Response> getAllDoctors();

	@GetMapping("/clinic-admin/doctor/{id}")
	public ResponseEntity<Response> getDoctorById(@PathVariable String id);

	@GetMapping("/clinic-admin/clinic/{clinicId}/doctor/{doctorId}")
	public ResponseEntity<Response> getDoctorByClinicAndDoctorId(@PathVariable String clinicId,
			@PathVariable String doctorId);

	@GetMapping("/clinic-admin/doctors/hospitalById/{hospitalId}")
	public ResponseEntity<Response> getDoctorsByHospitalById(@PathVariable String hospitalId);

	@GetMapping("/clinic-admin/doctors/hospital/{hospitalId}/subServiceId/{subServiceId}")
	public ResponseEntity<Response> getDoctorsBySubServiceId(@PathVariable String hospitalId,
			@PathVariable String subServiceId);

//	@GetMapping("/clinic-admin/getAllDoctorsBySubServiceId/{subServiceId}")
//	public ResponseEntity<Response> getAllDoctorsBySubServiceId(@PathVariable String subServiceId);

	@GetMapping("/clinic-admin/clinic/{clinicId}")
	ResponseEntity<Response> getClinicById(@PathVariable String clinicId);

	@GetMapping("/clinics/doctor/{doctorId}")
	ClinicInfoDTO getClinicInfoByDoctorId(@PathVariable String doctorId);

	// ------------------------------ Vitals ------------------------------
	@PostMapping("/clinic-admin/addingVitals/{bookingId}")
	ResponseEntity<Response> addVitals(@PathVariable("bookingId") String bookingId, @RequestBody VitalsDTO dto);

	@GetMapping("/clinic-admin/getVitals/{bookingId}/{patientId}")
	ResponseEntity<Response> getVitals(@PathVariable("bookingId") String bookingId,
			@PathVariable("patientId") String patientId);

	@DeleteMapping("/clinic-admin/deleteVitals/{bookingId}/{patientId}")
	ResponseEntity<Response> delVitals(@PathVariable("bookingId") String bookingId,
			@PathVariable("patientId") String patientId);

	@PutMapping("/clinic-admin/updateVitals/{bookingId}/{patientId}")
	ResponseEntity<Response> updateVitals(@PathVariable("bookingId") String bookingId,
			@PathVariable("patientId") String patientId, @RequestBody VitalsDTO dto);

	@GetMapping("/clinic-admin/diseases/{hospitalId}")
	public ResponseEntity<Response> getDiseasesByHospitalId(@PathVariable String hospitalId);

	@GetMapping("/clinic-admin/labtests/{hospitalId}")
	public ResponseEntity<Response> getLabTestsByHospitalId(@PathVariable String hospitalId);

	@GetMapping("/clinic-admin/api/s3/signed-url")
	ResponseEntity<String> getSignedUrl(@RequestParam("fileKey") String fileKey);

	@GetMapping("/clinic-admin/getAllRecoverySupportsByClinicId/{clinicId}")
	public Response getAllRecoverySupportsByClinicId(@PathVariable String clinicId);
	
	   @GetMapping("/clinic-admin/customername/{id}")
	    public String getCustomername(
	 			 @PathVariable String id ); 

	 @GetMapping("/clinic-admin/getAssignedTherapistDetails/{therapistRecordId}")
	    public ResponseEntity<Response> getAssignedTherapistDetails(
	            @PathVariable String therapistRecordId);

	@GetMapping("/clinic-admin/patinetname/{id}")
	public String getPatientname(
			@PathVariable String id );

	}
