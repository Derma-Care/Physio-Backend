package com.dermacare.bookingService.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.dermacare.bookingService.dto.CustomerOnbordingDTO;
import com.dermacare.bookingService.util.Response;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {
	
	 @GetMapping("/clinic-admin/customer/patientId/{patientId}/{clinicId}")
	    public ResponseEntity<Response> getCustomerByPatientId(@RequestHeader("Authorization") String token,@PathVariable String patientId,@PathVariable String clinicId);
	                                                                                        
	  @GetMapping("/clinic-admin/expenses/today/{clinicId}/{branchId}")
	    public Double getTodayExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	  
	  @GetMapping("/clinic-admin/expenses/weekly/{clinicId}/{branchId}")
	    public Double getWeeklyExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	  @GetMapping("/clinic-admin/expenses/monthly/{clinicId}/{branchId}")
	    public Double getMonthlyExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId); 
	  
	  @GetMapping("/clinic-admin/expenses/custom/{startDate}/{endDate}")
	    public Double customFilter(@RequestHeader("Authorization") String token,
	    		@PathVariable String startDate,
	    		@PathVariable String endDate);
	  
	  @GetMapping("/clinic-admin/customers/mobilenumber/{mobilenumber}/name/{name}")
	    public Map<String,String> getCustomerByMobilenumberAndName(@RequestHeader("Authorization") String token,@PathVariable String mobilenumber,@PathVariable String name);
	     
	  @GetMapping("/clinic-admin/customer/mobilenumber/{mobilenumber}/{clinicId}")
	    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(@RequestHeader("Authorization") String token,@PathVariable String mobilenumber,@PathVariable String clinicId);
	    
	    
	    @GetMapping("/clinic-admin/customer/name/{name}/{clinicId}")
	    public  List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(@RequestHeader("Authorization") String token,@PathVariable String name,@PathVariable String clinicId);
	 // ─────────────────────────────────────────────────────────────────
	    // S3 — Get signed URL for a raw S3 key
	    // Used to convert report file keys → accessible signed URLs
	    // before returning BookingResponse to frontend
	    // ─────────────────────────────────────────────────────────────────
	    @GetMapping("/clinic-admin/api/s3/signed-url")
	    String getSignedUrl(@RequestHeader("Authorization") String token,@RequestParam("fileKey") String fileKey);
	    

}
