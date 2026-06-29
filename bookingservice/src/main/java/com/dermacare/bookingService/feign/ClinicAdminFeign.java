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

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


@FeignClient(value = "clinicadmin")
public interface ClinicAdminFeign {
	
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getCustomerByPatientId" )
	@Retry(name = "bookingService", fallbackMethod = "getCustomerByPatientId")
	 @GetMapping("/clinic-admin/customer/patientId/{patientId}/{clinicId}")
	    public ResponseEntity<Response> getCustomerByPatientId(@RequestHeader("Authorization") String token,@PathVariable String patientId,@PathVariable String clinicId);
	  
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getTodayExpenses" )
	@Retry(name = "bookingService", fallbackMethod = "getTodayExpenses")
	  @GetMapping("/clinic-admin/expenses/today/{clinicId}/{branchId}")
	    public Double getTodayExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	  
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getWeeklyExpenses" )
	@Retry(name = "bookingService", fallbackMethod = "getWeeklyExpenses")
	  @GetMapping("/clinic-admin/expenses/weekly/{clinicId}/{branchId}")
	    public Double getWeeklyExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId);
	  
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getMonthlyExpenses" )
	@Retry(name = "bookingService", fallbackMethod = "getMonthlyExpenses")
	  @GetMapping("/clinic-admin/expenses/monthly/{clinicId}/{branchId}")
	    public Double getMonthlyExpenses(@RequestHeader("Authorization") String token,
	    		@PathVariable String clinicId,
	    		@PathVariable String branchId); 
	  
	@CircuitBreaker(name = "bookingService", fallbackMethod = "customFilter" )
	@Retry(name = "bookingService", fallbackMethod = "customFilter")
	  @GetMapping("/clinic-admin/expenses/custom/{startDate}/{endDate}")
	    public Double customFilter(@RequestHeader("Authorization") String token,
	    		@PathVariable String startDate,
	    		@PathVariable String endDate);
	  
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getCustomerByMobilenumberAndName" )
	@Retry(name = "bookingService", fallbackMethod = "getCustomerByMobilenumberAndName")
	  @GetMapping("/clinic-admin/customers/mobilenumber/{mobilenumber}/name/{name}")
	    public Map<String,String> getCustomerByMobilenumberAndName(@RequestHeader("Authorization") String token,@PathVariable String mobilenumber,@PathVariable String name);
	    
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getCustomerByMobileNumberAndClinicId" )
	@Retry(name = "bookingService", fallbackMethod = "getCustomerByMobileNumberAndClinicId")
	  @GetMapping("/clinic-admin/customer/mobilenumber/{mobilenumber}/{clinicId}")
	    public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(@RequestHeader("Authorization") String token,@PathVariable String mobilenumber,@PathVariable String clinicId);
	    
	    
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getCustomerByNameAndClinicId" )
	@Retry(name = "bookingService", fallbackMethod = "getCustomerByNameAndClinicId")
	    @GetMapping("/clinic-admin/customer/name/{name}/{clinicId}")
	    public  List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(@RequestHeader("Authorization") String token,@PathVariable String name,@PathVariable String clinicId);
	 // ─────────────────────────────────────────────────────────────────
	    // S3 — Get signed URL for a raw S3 key
	    // Used to convert report file keys → accessible signed URLs
	    // before returning BookingResponse to frontend
	    // ─────────────────────────────────────────────────────────────────
	@CircuitBreaker(name = "bookingService", fallbackMethod = "getSignedUrl" )
	@Retry(name = "bookingService", fallbackMethod = "getSignedUrl")
	  @GetMapping("/clinic-admin/api/s3/signed-url")
	   String getSignedUrl(@RequestHeader("Authorization") String token,@RequestParam("fileKey") String fileKey);
	    
//// FALLBACK METHODS //////
	default ResponseEntity<Response> getCustomerByPatientId (String token, String patientId,String clinicId,Exception e){
	   Response res = new Response();
	   res.setMessage(e.getMessage());
	   res.setStatus(503);
	   res.setSuccess(false);
		return ResponseEntity.status(503).body(res);}
	
	default Double getTodayExpenses (String token,
    		 String clinicId,
    		 String branchId,Exception e){		
		  throw new RuntimeException(e.getMessage());
	}
	
	default Double getWeeklyExpenses( String token,
    		String clinicId,
    	 String branchId,Exception e){		
		  throw new RuntimeException(e.getMessage());
	}
	
	default Double getMonthlyExpenses(String token,
    		String clinicId,
    		 String branchId,Exception e){		
		  throw new RuntimeException(e.getMessage());
	}
	
	default Double customFilter(String token,
    	 String startDate,
    		 String endDate,Exception e){		
		  throw new RuntimeException(e.getMessage());
	}
	
	default Map<String,String> getCustomerByMobilenumberAndName( String token, String mobilenumber, String name
	  ,Exception e){		
		  throw new RuntimeException(e.getMessage());
		}
	
	
	default CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId( String token, String mobilenumber,@PathVariable String clinicId,Exception e){		
		  throw new RuntimeException(e.getMessage());}
	
	
//	  default List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(String token, String name,String clinicId){
//		  throw new RuntimeException(e.getMessage());
//	  }

}
