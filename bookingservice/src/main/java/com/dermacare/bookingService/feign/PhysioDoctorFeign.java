package com.dermacare.bookingService.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.dermacare.bookingService.dto.Session;


@FeignClient(value = "PHYSIOTHERAPYDOCTOR-SERVICE")
public interface PhysioDoctorFeign {
	
	@GetMapping("/api/physiotherapy-doctor/getPhysioByBookingId/{bookingId}/{date}")
	public ResponseEntity<List<Session>> getPhysioByBookingId(@PathVariable String bookingId,@PathVariable String date);		
	

}
