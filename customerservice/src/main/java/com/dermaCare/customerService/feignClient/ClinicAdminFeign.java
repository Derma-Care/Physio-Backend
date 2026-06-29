package com.dermaCare.customerService.feignClient;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import com.dermaCare.customerService.dto.PatientFeedbackDTO;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.dto.TherapistRecordRequest;
import com.dermaCare.customerService.util.Response;


@FeignClient(value = "clinicadmin")
//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "clinicAdminServiceFallBack")
public interface ClinicAdminFeign {
	
	@GetMapping("/clinic-admin/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
	public ResponseEntity<Response> getDoctorSlot(
	        @PathVariable String hospitalId, 
	        @PathVariable String branchId,
	        @PathVariable String doctorId);
	
	@PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
	public boolean updateDoctorSlotWhileBooking(@RequestHeader("Authorization") String token,@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);

	@GetMapping("/clinic-admin/getReportsBycustomerId/{customerId}")
    public ResponseEntity<Response> getReportsBycustomerId(@PathVariable String customerId);
   	
	
	 @PostMapping("/clinic-admin/customers/login")
	  public ResponseEntity<Response> login(@RequestBody  Map<String,String>  dto);
	 
	 @GetMapping("/clinic-admin/getBestDoctorByKeyWords/{keyPoints}")
	    public ResponseEntity<Response> getRecommendedClinicsAndOnDoctors(@PathVariable String keyPoints);
	 
	 @PostMapping("/clinic-admin/block/slot")
	  public boolean blockSlot(@RequestBody TempBlockingSlot tempBlockingSlot);
	 
	 @PostMapping("/clinic-admin/therapist-session-details")
	    public ResponseEntity<Response> getTherapistSessionDetails(
	    		@RequestHeader("Authorization") String token,  @RequestBody TherapistRecordRequest request);
	 
	 
	 @GetMapping("/clinic-admin/staff-info/{hospitalId}/{branchId}")
		public ResponseEntity<Response> getStaffInfo(@RequestHeader("Authorization") String token,
		        @PathVariable String hospitalId,
		        @PathVariable String branchId);
	 

	 @PostMapping("/clinic-admin/createPatientFeedback")
	    public Response createFeedback(@RequestHeader("Authorization") String token,
	            @RequestBody PatientFeedbackDTO dto);
	 
	   @GetMapping("/clinic-admin/getByPatientFeedbackClinicIdAndBranchId/{clinicId}/{branchId}/{patientId}")
	    public ResponseEntity<Response> getByClinicIdAndBranchIdAndPatirntId(@RequestHeader("Authorization") String token,
	            @PathVariable String clinicId,
	            @PathVariable String branchId,
	            @PathVariable String patientId );


//	 @PostMapping("/clinic-admin/customers/login")
//	    public ResponseEntity<Response> login(@RequestBody CustomerLoginDTO dto);
//	    

	 
//	//FALLBACK METHODS
//	
//		default ResponseEntity<?> clinicAdminServiceFallBack(Exception e){		 
//		return ResponseEntity.status(503).body(new Response("CLINIC ADMIN SERVICE NOT AVAILABLE",503,false,null));}
}
