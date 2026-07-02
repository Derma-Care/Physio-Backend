package com.dermaCare.customerService.feignClient;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.util.ResponseStructure;

@FeignClient(value = "bookingservice")
public interface BookingFeign {

	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(@PathVariable String id);
	
	@PostMapping("/api/v1/bookService")
	public ResponseEntity<ResponseStructure<BookingResponse>> bookService(@RequestBody BookingRequset req);
	
	@GetMapping("/api/v1/booking/customerId/{customerId}")
	public ResponseEntity<ResponseStructure<List<Map<String,Object>>>> getBookingByCustomerId(@RequestHeader("Authorization") String token,@PathVariable String customerId);
	
	@PostMapping("/api/v1/bookPhysioAppointment")
	public  ResponseEntity<?> bookPhysioAppointment(@RequestBody BookingRequset req);
	
	@GetMapping("/api/v1/booking/completed/customerId/{customerId}")
	public ResponseEntity<ResponseStructure<List<Map<String,Object>>>> getCompletedBookingByCustomerId(@PathVariable String customerId);		
	
	
}