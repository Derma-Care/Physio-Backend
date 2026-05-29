package com.dermacare.bookingService.feign;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.dermacare.bookingService.dto.CustomerOnbordingDTO;
import com.dermacare.bookingService.util.Response;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {
	
	 @GetMapping("/clinic-admin/customer/patientId/{patientId}/{clinicId}")
	    public ResponseEntity<Response> getCustomerByPatientId(@PathVariable String patientId,@PathVariable String clinicId);
	                                                                                        
	  @GetMapping("/clinic-admin/expenses/today/{clinicId}/{branchId}")
	    public Double getTodayExpenses(
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	  
	  @GetMapping("/clinic-admin/expenses/weekly/{clinicId}/{branchId}")
	    public Double getWeeklyExpenses(
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	  @GetMapping("/clinic-admin/expenses/monthly/{clinicId}/{branchId}")
	    public Double getMonthlyExpenses(
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId); 
	  
	  @GetMapping("/clinic-admin/expenses/custom/{startDate}/{endDate}")
	    public Double customFilter(
	    		@PathVariable String startDate,
	    		@PathVariable String endDate);
	  
	  @GetMapping("/clinic-admin/customers/mobilenumber/{mobilenumber}/name/{name}")
	    public Map<String,String> getCustomerByMobilenumberAndName(@PathVariable String mobilenumber,@PathVariable String name);
	     
	  @GetMapping("/clinic-admin/customer/mobilenumber/{mobilenumber}/{clinicId}")
	    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(@PathVariable String mobilenumber,@PathVariable String clinicId);
	    
	    
	    @GetMapping("/clinic-admin/customer/name/{name}/{clinicId}")
	    public  List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(@PathVariable String name,@PathVariable String clinicId);
	 // ─────────────────────────────────────────────────────────────────
	    // S3 — Get signed URL for a raw S3 key
	    // Used to convert report file keys → accessible signed URLs
	    // before returning BookingResponse to frontend
	    // ─────────────────────────────────────────────────────────────────
	    @GetMapping("/clinic-admin/api/s3/signed-url")
	    String getSignedUrl(@RequestParam("fileKey") String fileKey);
	    

}
