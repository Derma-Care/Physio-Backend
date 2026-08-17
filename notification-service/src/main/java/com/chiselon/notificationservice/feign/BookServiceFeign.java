package com.chiselon.notificationservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.chiselon.notificationservice.dto.BookingResponse;
import com.chiselon.notificationservice.dto.ResponseStructure;


@FeignClient(value = "bookingservice")
public interface BookServiceFeign {
	
	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(@RequestHeader("Authorization") String token,@PathVariable String id);
	
	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestHeader("Authorization") String token,@RequestBody BookingResponse bookingResponse );

}
