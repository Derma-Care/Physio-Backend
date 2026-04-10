package com.AdminService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.CustomerDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "customerservice")
@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "customerServiceFallBack")
public interface CustomerFeign {
	
	@PostMapping("/api/customer/saveBasicDetails")
	public ResponseEntity<Response> saveCustomerBasicDetails(@RequestBody CustomerDTO customerDTO );
	
	@GetMapping("/api/customer/getCustomerByInput/{input}")
  	public ResponseEntity<?> getCustomerByUsernameMobileEmail(@PathVariable("input") String input);
	
	@GetMapping("/api/customer/getBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> getCustomerBasicDetails(@PathVariable("mobileNumber") String mobileNumber );
	
	@GetMapping("/api/customer/getAllCustomers")
	public ResponseEntity<Response> getAllCustomers();
	
	@PutMapping("/api/customer/updateCustomerBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> updateCustomerBasicDetails(@RequestBody CustomerDTO customerDTO,
			@PathVariable("mobileNumber") String mobileNumber );
	
	@DeleteMapping("/api/customer/deleteCustomerBasicDetails/{mobileNumber}")
	public ResponseEntity<Response> deleteCustomerBasicDetails(@PathVariable("mobileNumber") String mobileNumber );
	
	
	//FALLBACK METHOD
	
	default ResponseEntity<?> customerServiceFallBack(Throwable e){		 
		return ResponseEntity.status(503).body(new Response(false,null,"CUSTOMER SERVICE NOT AVAILABLE",503,null,null, null, null, null, null));}


}
