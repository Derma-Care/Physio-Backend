package com.dermacare.notification_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import com.dermacare.notification_service.dto.Response;

@FeignClient(value = "doctor-service" )
public interface DoctorFeign {
	
	 @GetMapping("/api/doctors/getAllDoctorSaveDetails")
	    public ResponseEntity<Response> getAllDoctorSaveDetails();
	
	     
}
