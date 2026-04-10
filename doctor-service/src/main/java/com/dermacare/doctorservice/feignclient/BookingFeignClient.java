package com.dermacare.doctorservice.feignclient;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.dermacare.doctorservice.dto.BookingRequset;
import com.dermacare.doctorservice.dto.BookingResponse;
import com.dermacare.doctorservice.dto.ResponseStructure;


@FeignClient(name = "bookingservice")
public interface  BookingFeignClient {
	
	@GetMapping("/api/v1/getBookedServiceById/{id}")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(@PathVariable String id);
	
	
	@PutMapping("/api/v1/updateAppointment")
	public ResponseEntity<?> updateAppointment(@RequestBody BookingResponse bookingResponse );
	
	@GetMapping("/api/v1/getAppointmentByPatientId/{patientId}")
	public ResponseEntity<?> getAppointmentByPatientId(@PathVariable String patientId);
	
	@GetMapping("/api/v1/getAppointsByInput/{input}")
	public ResponseEntity<?> getAppointsByInput(@PathVariable String input);
	
	@GetMapping("/api/v1/getTodayDoctorAppointmentsByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
	@GetMapping("/api/v1/filterDoctorAppointmentsByDoctorId/{clinicId}/{doctorId}/{number}")
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId,@PathVariable String number);
	
	@GetMapping("/api/v1/getCompletedApntsByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getSizeOfConsultationTypesByDoctorId/{clinicId}/{doctorId}")
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(@PathVariable String clinicId,@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getInProgressAppointments/{mobilenumber}")
	public ResponseEntity<?> inProgressAppointments(@PathVariable String mobilenumber);
	
	@GetMapping("/api/v1/getDoctorFutureAppointments/{doctorId}")
	public ResponseEntity<?> getDoctorFutureAppointments(@PathVariable String doctorId);
	
	@GetMapping("/api/v1/getAllBookedServices/{doctorId}")
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingByDoctorId(@PathVariable String doctorId);
	@PostMapping("/api/v1/bookService")
	ResponseEntity<?> bookService(@RequestBody BookingRequset bookingRequest);
	
	
}
