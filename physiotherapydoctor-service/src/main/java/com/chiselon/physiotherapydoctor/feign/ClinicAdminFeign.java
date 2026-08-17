package com.chiselon.physiotherapydoctor.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.chiselon.physiotherapydoctor.dto.BookingResponse;
import com.chiselon.physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import com.chiselon.physiotherapydoctor.dto.ClinicInfoDTO;
import com.chiselon.physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.physiotherapydoctor.dto.DoctorsDTO;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.dto.ResponseStructure;
import com.chiselon.physiotherapydoctor.dto.TherapistRecordDTO;
import com.chiselon.physiotherapydoctor.dto.TreatmentDTO;
import com.chiselon.physiotherapydoctor.dto.VitalsDTO;

@FeignClient(name = "clinicadmin")
public interface ClinicAdminFeign {

	@PostMapping("/clinic-admin/doctorLogin")
	public ResponseEntity<Response> doctorLogin(@RequestBody  Map<String,String> dto);
	

	 @PutMapping("/clinic-admin/update-password/{username}")
	    Response changePassword(@RequestHeader("Authorization") String token,@PathVariable("username") String username, @RequestBody ChangeDoctorPasswordDTO updateDTO);
	 
	 @PostMapping("/clinic-admin/doctorId/{doctorId}/availability")
	 Response updateDoctorAvailability(@RequestHeader("Authorization") String token,@PathVariable("doctorId") String doctorId,
	                                   @RequestBody DoctorAvailabilityStatusDTO availabilityDTO);
	 

    // ✅ Get booking by bookingId
    @GetMapping("/clinic-admin/getBookingById/{bookingId}")
    ResponseStructure<BookingResponse> getBookingById(@RequestHeader("Authorization") String token,
            @PathVariable("bookingId") String bookingId);

    // ✅ Update booking status
    @PutMapping("/clinic-admin/updateAppointmentBasedOnBookingId")
    ResponseEntity<?> updateAppointment(@RequestHeader("Authorization") String token,
            @RequestBody BookingResponse bookingResponse);
    
    @GetMapping("/clinic-admin/getByPatientIdAndBookingId/{patientId}/{bookingId}")
    ResponseStructure<List<TherapistRecordDTO>> getByPatientIdAndBookingId(@RequestHeader("Authorization") String token,
            @PathVariable String patientId,
            @PathVariable String bookingId);
    
    @GetMapping("/clinic-admin/getRecordBySession/{clinicId}/{branchId}/{bookingId}/{patientId}/{sessionId}")
    ResponseEntity<ResponseStructure<TherapistRecordDTO>> getRecordBySession(@RequestHeader("Authorization") String token,
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String bookingId,
            @PathVariable String patientId,
            @PathVariable String sessionId);
    
 // ================= GET Threapistdata by clinicId and Branch Id  with required field=================
 	@GetMapping("/clinic-admin/getTherapistWithRequiredFileds/{clinicId}/{branchId}")
 	public ResponseEntity<Response> getTherapistWithRequiredFileds(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
 			@PathVariable String branchId);
 	
 	
 	@GetMapping("/clinic-admin/getCompletedTherapyRecord/{clinicId}/{branchId}/{therapistRecordId}/{sessionId}")
 	ResponseEntity<ResponseStructure<TherapistRecordDTO>> getCompletedTherapyRecord(@RequestHeader("Authorization") String token,
 	        @PathVariable String clinicId,
 	        @PathVariable String branchId,
 	        @PathVariable String therapistRecordId,
 	        @PathVariable String sessionId);
 	
	@PutMapping("/clinic-admin/updateDoctor/{doctorId}")
	public ResponseEntity<Response> updateDoctorById(@RequestHeader("Authorization") String token,@PathVariable String doctorId,
			 @RequestBody DoctorsDTO dto) ;

//	======================From doctor service========================

//	--------------------------------- TreatmentFeignClient from clinic admin  -------------------------------------
	@PostMapping("/clinic-admin/treatment/addTreatment")
	public ResponseEntity<Response> addTreatment(@RequestBody TreatmentDTO dto);

	  @GetMapping("/clinic-admin/doctors")
		 public ResponseEntity<Response> getAllDoctors(@RequestHeader("Authorization") String token);
		 
		 @GetMapping("/clinic-admin/doctor/{id}")
		 public ResponseEntity<Response> getDoctorById(@RequestHeader("Authorization") String token,@PathVariable String id);
		 
		 @GetMapping("/clinic-admin/clinic/{clinicId}/doctor/{doctorId}")
			public ResponseEntity<Response> getDoctorByClinicAndDoctorId(@RequestHeader("Authorization") String token,@PathVariable String clinicId,
					@PathVariable String doctorId);
		 
		 @GetMapping("/clinic-admin/doctors/hospitalById/{hospitalId}")
			public ResponseEntity<Response> getDoctorsByHospitalById(@RequestHeader("Authorization") String token,@PathVariable String hospitalId);
		 	 
		 @GetMapping("/clinic-admin/clinic/{clinicId}")
		 ResponseEntity<Response> getClinicById(@RequestHeader("Authorization") String token,@PathVariable String clinicId);

		 @GetMapping("/clinics/doctor/{doctorId}")
		    ClinicInfoDTO getClinicInfoByDoctorId(@RequestHeader("Authorization") String token,@PathVariable String doctorId);
		
		// ------------------------------ Vitals ------------------------------
		 @PostMapping("/clinic-admin/addingVitals/{bookingId}")
		    ResponseEntity<Response> addVitals(@RequestHeader("Authorization") String token,@PathVariable("bookingId") String bookingId,
		                                       @RequestBody VitalsDTO dto);

		    @GetMapping("/clinic-admin/getVitals/{bookingId}/{patientId}")
		    ResponseEntity<Response> getVitals(@RequestHeader("Authorization") String token,@PathVariable("bookingId") String bookingId,
		                                       @PathVariable("patientId") String patientId);

		    @DeleteMapping("/clinic-admin/deleteVitals/{bookingId}/{patientId}")
		    ResponseEntity<Response> delVitals(@RequestHeader("Authorization") String token,@PathVariable("bookingId") String bookingId,
		                                       @PathVariable("patientId") String patientId);

		    @PutMapping("/clinic-admin/updateVitals/{bookingId}/{patientId}")
		    ResponseEntity<Response> updateVitals(@RequestHeader("Authorization") String token,@PathVariable("bookingId") String bookingId,
		                                          @PathVariable("patientId") String patientId,
		                                          @RequestBody VitalsDTO dto);
		    @GetMapping("/clinic-admin/diseases/{hospitalId}")
		    public ResponseEntity<Response> getDiseasesByHospitalId(@RequestHeader("Authorization") String token,@PathVariable String hospitalId);
		 
		    @GetMapping("/clinic-admin/api/s3/signed-url")
		    ResponseEntity<String> getSignedUrl(@RequestHeader("Authorization") String token,@RequestParam("fileKey") String fileKey);

	@GetMapping("/clinic-admin/treatment/getAllTreatments")
	public ResponseEntity<Response> getAllTreatments(@RequestHeader("Authorization") String token);

	@GetMapping("/clinic-admin/treatment/getTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> getTreatmentById(@RequestHeader("Authorization") String token,@PathVariable String id, @PathVariable String hospitalId);

	@DeleteMapping("/clinic-admin/treatment/deleteTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> deleteTreatmentById(@RequestHeader("Authorization") String token,@PathVariable String id, @PathVariable String hospitalId);

	@PutMapping("/clinic-admin/treatment/updateTreatmentById/{id}/{hospitalId}")
	public ResponseEntity<Response> updateTreatmentById(@RequestHeader("Authorization") String token,@PathVariable String id, @PathVariable String hospitalId,
			@RequestBody TreatmentDTO dto);


	@GetMapping("/clinic-admin/getAllRecoverySupportsByClinicId/{clinicId}")

	public Response getAllRecoverySupportsByClinicId(@RequestHeader("Authorization") String token,@PathVariable String clinicId);
	
	   @GetMapping("/clinic-admin/customername/{id}")
	    public String getCustomername(@RequestHeader("Authorization") String token,
	 			 @PathVariable String id ); 

	 @GetMapping("/clinic-admin/getAssignedTherapistDetails/{therapistRecordId}")
	    public ResponseEntity<Response> getAssignedTherapistDetails(@RequestHeader("Authorization") String token,
	            @PathVariable String therapistRecordId);



	@GetMapping("/clinic-admin/patinetname/{id}")
	public String getPatientname(@RequestHeader("Authorization") String token,
			@PathVariable String id );

	}

