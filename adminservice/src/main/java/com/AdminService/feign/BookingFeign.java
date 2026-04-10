package com.AdminService.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
	
	@PostMapping("/api/v1//bookService")
	public  ResponseEntity<?> bookService(@RequestBody BookingRequset req);
	
	@GetMapping("/api/v1/getAllBookedServices")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedService();
	
	@DeleteMapping("/api/v1/deleteService/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(@PathVariable("id") String id);
	
	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable("doctorId") String doctorId);

	
	///FALLBACK METHOD
	
	default ResponseEntity<?> bookServiceFallBack(Exception e){		 
		return ResponseEntity.status(503).body( new ResponseStructure<BookingResponse>(null,"Booking Service Not Available",HttpStatus.SERVICE_UNAVAILABLE,503));
		}
	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponseDTO>> getBookedService(@PathVariable String id);
	
	
	@GetMapping("/api/v1/getAppointmentByPatientId/{patientId}")
	public ResponseEntity<?> getAppointmentByPatientId(@PathVariable String patientId);
	
	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestBody BookingResponseDTO res );
	
	//---------------------------to get patientdetails by bookingId,pateintId,mobileNumber---------------------------
	@GetMapping("/api/v1/getPatientDetailsForConsetForm/{bookingId}/{patientId}/{mobileNumber}")
	public ResponseEntity<Response> getPatientDetailsForConsentForm(@PathVariable String bookingId,@PathVariable String patientId,@PathVariable String mobileNumber);

	
	
	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestBody BookingResponse bookingResponse );
	
//	@PostMapping("/api/v1/bookService")
//	public ResponseEntity<ResponseStructure<BookingResponse>> bookService(@RequestBody BookingRequset req);
	
//	@DeleteMapping("/api/v1/deleteService/{id}")
//	//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "deleteBookedServiceFallBack")
//	public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(@PathVariable String id);
	
	@GetMapping("/api/v1/getBookedServicesByMobileNumber/{mobileNumber}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getCustomerBookedServices(
			@PathVariable String mobileNumber);
	
//	@GetMapping("/api/v1/getAllBookedServices")
//	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedService();
	
//	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
//	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable String doctorId);

	@GetMapping("/api/v1/getBookedServicesByServiceId/{serviceId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByServiceId(@PathVariable String serviceId);
	
	@GetMapping("/api/v1/getBookedServicesByClinicId/{clinicId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByClinicId(@PathVariable String clinicId);

	
	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@PathVariable String mobilenumber);


}