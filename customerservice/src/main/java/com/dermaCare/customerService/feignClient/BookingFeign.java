package com.dermaCare.customerService.feignClient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.util.ExtractFeignMessage;
import com.dermaCare.customerService.util.ResponseStructure;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;



@FeignClient(value = "bookingservice")
//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "bookingServiceFallBack")
public interface BookingFeign {

	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(@PathVariable String id);
	
	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestBody BookingResponse bookingResponse );
	
	@PostMapping("/api/v1/bookService")
	public ResponseEntity<ResponseStructure<BookingResponse>> bookService(@RequestBody BookingRequset req);
	
	@DeleteMapping("/api/v1/deleteService/{id}")
	//@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "deleteBookedServiceFallBack")
	public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(@PathVariable String id);
	
	@GetMapping("/api/v1/getBookedServicesByMobileNumber/{mobileNumber}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getCustomerBookedServices(
			@PathVariable String mobileNumber);
	
	@GetMapping("/api/v1/getAllBookedServices")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedService();
	
	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable String doctorId);

	@GetMapping("/api/v1/getBookedServicesByServiceId/{serviceId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByServiceId(@PathVariable String serviceId);
	
	@GetMapping("/api/v1/getBookedServicesByClinicId/{clinicId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByClinicId(@PathVariable String clinicId);

	
	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@PathVariable String mobilenumber);
	
	
	@GetMapping("/api/v1/getDoctorFutureAppointments/{doctorId}")
	public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getAllBookedServicesByBranchId/{branchId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getAllBookedServicesByBranchId(@PathVariable String branchId);

	@GetMapping("/api/v1/getBookedServicesByClinicIdWithBranchId/{clinicId}/{branchId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookedServicesByClinicIdWithBranchId(
	        @PathVariable String clinicId,
	        @PathVariable String branchId);
	
	@GetMapping("/api/v1/booking/customerId/{customerId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByCustomerId(@PathVariable String customerId);

	@GetMapping("/api/v1/appointments/Inprogress/{customerId}")
	public ResponseEntity<?> getInprogressAppointmentsByCustomerId(@PathVariable String customerId);
	
	@GetMapping("/api/v1/appointments/FilterbyRelation/{customerId}")
	public ResponseEntity<?> retrieveAppointnmentsByRelation(@PathVariable String customerId);
	
	@GetMapping("/api/v1/appointments/patientId/{patientId}")	
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByPatientId(@PathVariable String patientId);
	
	@GetMapping("/api/v1/appointments/Inprogress/patientId/{patientId}/{clinicId}")
	public ResponseEntity<?> getInprogressAppointmentsByPatientId(@PathVariable String patientId,@PathVariable String clinicId );
	
	//FALLBACK METHODS
	
//		default ResponseEntity<?> bookingServiceFallBack(Exception e){		 
//		return ResponseEntity.status(503).body(new ResponseStructure<BookingResponse>(null,"Booking-Service Not Available",HttpStatus.SERVICE_UNAVAILABLE,503));}
		
//		
//		default ResponseEntity<?> deleteBookedServiceFallBack(FeignException e){		 
//			return ResponseEntity.status(e.status()).body(new ResponseStructure<BookingResponse>(null,ExtractFeignMessage.clearMessage(e),null,e.status()));}
//			
	
	
}