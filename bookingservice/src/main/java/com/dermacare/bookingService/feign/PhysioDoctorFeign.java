package com.dermacare.bookingService.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.dermacare.bookingService.dto.Session;


@FeignClient(value = "physiotherapydoctor-service")
public interface PhysioDoctorFeign {
	
	@GetMapping("/api/physiotherapy-doctor/getPhysioByBookingId/{bookingId}/{date}")
	public ResponseEntity<List<Session>> getPhysioByBookingId(@PathVariable String bookingId,@PathVariable String date);		
	
	 @GetMapping("/api/physiotherapy-doctor/followups/today/booking-ids")
	 public List<String> getTodayFollowUpBookingIds();

	 @GetMapping("/api/physiotherapy-recordgetById/{BookingId}")
		public String getByBookingId(@PathVariable String BookingId);


}
