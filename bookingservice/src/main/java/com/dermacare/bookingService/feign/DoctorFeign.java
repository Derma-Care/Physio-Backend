package com.dermacare.bookingService.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dermacare.bookingService.dto.DoctorSaveDetailsDTO;
import com.dermacare.bookingService.util.Response;


@FeignClient(name = "doctor-service")
public interface DoctorFeign {
	
	 @GetMapping("/api/doctors/getDoctorSaveDetailsByBookingId/{bookingId}")
	    public ResponseEntity<Response> getDoctorSaveDetailsByBookingId(@PathVariable String bookingId);
	    	      

}
