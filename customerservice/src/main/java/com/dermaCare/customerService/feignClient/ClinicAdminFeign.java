package com.dermaCare.customerService.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.dto.DoctorsDTO;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.util.Response;


@FeignClient(value = "clinicadmin")
//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "clinicAdminServiceFallBack")
public interface ClinicAdminFeign {

	@GetMapping("/clinic-admin/getDoctorByServiceId/{hospitalId}/{service}")
	public ResponseEntity<Response> getDoctorByService(@PathVariable String hospitalId, @PathVariable String service);
	
	@GetMapping("/clinic-admin/doctors/hospital/{hospitalId}/subServiceId/{subServiceId}")
	public ResponseEntity<Response> getDoctorsBySubServiceId(@PathVariable String hospitalId, @PathVariable String subServiceId);
	
	@GetMapping("/clinic-admin/doctors/{hospitalId}/{branchId}/{subServiceId}")
	public ResponseEntity<Response> getDoctorsByHospitalBranchAndSubService(@PathVariable String hospitalId,
			@PathVariable String branchId, @PathVariable String subServiceId);

	@GetMapping("/clinic-admin/doctor/{id}")
	public ResponseEntity<Response> getDoctorById(@PathVariable String id);
	
	@GetMapping("/clinic-admin/getHospitalAndDoctorUsingSubServiceId/{subServiceId}")
	public ResponseEntity<Response> getHospitalAndDoctorUsingSubServiceId(@PathVariable String subServiceId);
	
	@GetMapping("/clinic-admin/getAllDoctorsBySubServiceId/{subServiceId}")
	public ResponseEntity<Response> getAllDoctorsBySubServiceId(@PathVariable String subServiceId);
	
	@GetMapping("/clinic-admin/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
	public ResponseEntity<Response> getDoctorSlot(
	        @PathVariable String hospitalId, 
	        @PathVariable String branchId,
	        @PathVariable String doctorId);
	
	@GetMapping("/clinic-admin/averageRatings/{branchId}/{doctorId}")
	public ResponseEntity<Response> getAverageRatings(@PathVariable String branchId, @PathVariable String doctorId);
	
	@PutMapping("/clinic-admin/updateDoctorSlotWhileBooking/{doctorId}/{branchId}/{date}/{time}")
	public boolean updateDoctorSlotWhileBooking(@PathVariable String doctorId,@PathVariable String branchId, @PathVariable String date,
			@PathVariable String time);
	
	@PutMapping("/clinic-admin/updateDoctor/{doctorId}")
	public ResponseEntity<Response> updateDoctorById(@PathVariable String doctorId,
			@RequestBody DoctorsDTO dto);
	
	@GetMapping("/clinic-admin/getReportsBycustomerId/{customerId}")
    public ResponseEntity<Response> getReportsBycustomerId(@PathVariable String customerId);
   	
	
	 @PostMapping("/clinic-admin/customers/login")
	  public ResponseEntity<Response> login(@RequestBody CustomerLoginDTO dto);
	 
	 @GetMapping("/clinic-admin/getBestDoctorByKeyWords/{keyPoints}")
	    public ResponseEntity<Response> getRecommendedClinicsAndOnDoctors(@PathVariable String keyPoints);
	 
	 @GetMapping("/clinic-admin/getAverageRatingsByDoctorId/{doctorId}")
		public ResponseEntity<Response> getAverageRatingsByDoctorId( @PathVariable String doctorId) ;

	 @PostMapping("/clinic-admin/block/slot")
	  public boolean blockSlot(@RequestBody TempBlockingSlot tempBlockingSlot);
	 
//	//FALLBACK METHODS
//	
//		default ResponseEntity<?> clinicAdminServiceFallBack(Exception e){		 
//		return ResponseEntity.status(503).body(new Response("CLINIC ADMIN SERVICE NOT AVAILABLE",503,false,null));}
}
