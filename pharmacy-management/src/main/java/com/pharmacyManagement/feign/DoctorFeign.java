package com.pharmacyManagement.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.pharmacyManagement.dto.DoctorSaveDetailsDTO;


@FeignClient(value = "doctor-service")
public interface DoctorFeign {
	
	 @GetMapping("/api/doctors/getDoctorSaveDetails/{customerId}")
	 public ResponseEntity<DoctorSaveDetailsDTO> getDoctorSaveDetails(@PathVariable String customerId);
	    	

}
