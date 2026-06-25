package com.AdminService.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(value = "bookingservice")
@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "bookServiceFallBack")
@CrossOrigin
public interface BookingFeign {
	
	@PostMapping("/api/v1/bookService")
	public  ResponseEntity<?> bookService(@RequestHeader("Authorization") String token,@RequestBody BookingRequset req);

	@GetMapping("/api/v1/getAllBookedServices/{page}/{size}")
	public ResponseEntity<Page<BookingResponse>> getAllBookings(@RequestHeader("Authorization") String token,
			@PathVariable int page,
			@PathVariable int size);

	@DeleteMapping("/api/v1/deleteService/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(@RequestHeader("Authorization") String token,@PathVariable("id") String id);

	@GetMapping("/api/v1/doctor/{doctorId}/{page}/{size}")
	public ResponseEntity<?> bookingByDoctorId(@RequestHeader("Authorization") String token,
			@PathVariable String doctorId,
			@PathVariable int page,
			@PathVariable int size);
	
	
	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponseDTO>> getBookedService(@RequestHeader String token,@PathVariable String id);


	@GetMapping("/api/v1/patient/{clinicId}/{patientId}/{page}/{size}")
	public ResponseEntity<Page<BookingResponse>> bookingByPatientId(@RequestHeader String token,
			@PathVariable String clinicId,
			@PathVariable String patientId,
			@PathVariable int page,
			@PathVariable int size);

	@PutMapping("/update/bookingId")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(@RequestHeader String token,@RequestBody BookingResponseDTO bookingResponse );

		//---------------------------to get patientdetails by bookingId,pateintId,mobileNumber---------------------------
	@GetMapping("/api/v1/getPatientDetailsForConsetForm/{bookingId}/{patientId}/{mobileNumber}")
	public ResponseEntity<Response> getPatientDetailsForConsentForm(@RequestHeader String token,@PathVariable String bookingId,@PathVariable String patientId,@PathVariable String mobileNumber);

	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestHeader String token,@RequestBody BookingResponse bookingResponse );
	
//	@PostMapping("/api/v1/bookService")
//	public ResponseEntity<ResponseStructure<BookingResponse>> bookService(@RequestBody BookingRequset req);
	
//	@DeleteMapping("/api/v1/deleteService/{id}")
//	//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "deleteBookedServiceFallBack")
//	public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(@PathVariable String id);

	@GetMapping("/api/v1/bookings/{mobileNumber}/{page}/{size}")
	public ResponseEntity<?> getBookedServices(@RequestHeader String token,
			@PathVariable String mobileNumber,
			@PathVariable int page,
			@PathVariable int size);
	
//	@GetMapping("/api/v1/getAllBookedServices")
//	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedService();
	
//	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
//	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable String doctorId);

	@GetMapping("/api/v1/getBookedServicesByServiceId/{serviceId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByServiceId(@RequestHeader String token,@PathVariable String serviceId);

	@GetMapping("/api/v1/clinic/{clinicId}/{page}/{size}")
	public ResponseEntity<?> bookingByClinicId(@RequestHeader String token,
			@PathVariable String clinicId,
			@PathVariable int page,
			@PathVariable int size);

	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@RequestHeader String token,@PathVariable String mobilenumber);

	@PostMapping("/api/v1/bookPhysioAppointment")
	public  ResponseEntity<Response> bookPhysioAppointment(@RequestHeader String token,@RequestBody BookingRequset req);
	
	
	///FALLBACK METHOD
	
		default ResponseEntity<?> bookServiceFallBack(Exception e){		 
			return ResponseEntity.status(503).body( new ResponseStructure<BookingResponse>(null,"Booking Service Not Available",HttpStatus.SERVICE_UNAVAILABLE,503));
			}


}