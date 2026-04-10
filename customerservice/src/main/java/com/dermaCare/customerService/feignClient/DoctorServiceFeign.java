package com.dermaCare.customerService.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.dermaCare.customerService.util.Response;

@FeignClient(name = "doctor-service")
public interface DoctorServiceFeign {
	
	 @GetMapping("/api/doctors/getDoctorSaveDetailsByCustomerId/{customerId}")
	 public ResponseEntity<Response> getDoctorSaveDetailsByCustomerId(@PathVariable String customerId);
	

}
