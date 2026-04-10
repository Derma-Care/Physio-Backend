package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.BookingRequset;
import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.BookingService;
import com.fasterxml.jackson.core.JsonProcessingException;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class BookingServiceController {

	@Autowired
	BookingService bookingService; 
	
	@GetMapping("/getAllbookingsDetailsByBranchId/{branchId}")
	public ResponseEntity<Response> getAllbookingsDetailsByBranchId(@PathVariable String branchId) {
		Response response = bookingService.getAllBookedServicesDetailsByBranchId(branchId);
		return ResponseEntity.status(response.getStatus()).body(response);
	}
	
	@GetMapping("/getAllbookingsDetailsByClinicAndBranchId/{clinicId}/{branchId}")
	public ResponseEntity<?> getAllbookingsDetailsByClinicAndBranchId(@PathVariable String clinicId,@PathVariable String branchId) {
		return bookingService.getBookingsByClinicIdWithBranchId(clinicId,branchId);
		
	}
	
	
	@GetMapping("/appointments/byIds/{clinicId}/{branchId}")
	public ResponseEntity<?> retrieveOneWeekAppointments(@PathVariable String clinicId,@PathVariable String branchId) {
		return bookingService.retrieveOneWeekAppointments(clinicId, branchId);
	
	}
	
	
	@GetMapping("/appointments/byIdsAndDate/{clinicId}/{branchId}/{date}")
	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(@PathVariable String clinicId,@PathVariable String branchId,@PathVariable String date) {
	return bookingService.retrieveAppointnmentsByServiceDate(clinicId, branchId, date);		
	}
	
	
	@PutMapping("/updateAppointmentBasedOnBookingId")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(@RequestBody BookingResponse bookingResponse) {
		return bookingService.updateAppointmentBasedOnBookingId(bookingResponse);
		
	}
	
	@GetMapping("/bookings/byInput/{input}/{clinicId}")
	   public ResponseEntity<?> getInprogressBookingsByInput(
				 @PathVariable String input, @PathVariable String clinicId){
		   return bookingService.retrieveAppointnmentsByInput(input,clinicId);
	 }
	
	   @GetMapping("/bookings/byPatientId/{patientId}")
	   public ResponseEntity<?> getInprogressBookingsByPatientId(
				 @PathVariable String patientId){
		   return bookingService.retrieveAppointnmentsByPatientId(patientId);
		   
	 }
	   @PostMapping("/bookService")
	   public ResponseEntity<Object> bookService(@RequestBody BookingRequset req)throws JsonProcessingException  {
	   	Response response = bookingService.bookService(req);
	   	if(response != null && response.getData() == null) {
	   		 return ResponseEntity.status(response.getStatus()).body(response);
	   	 }else if(response != null && response.getData() != null) {
	   		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
	   		 else {
	   			 return null;
	   		 }
	   	}
	   @GetMapping("/bookings/Inprogress/patientId/{patientId}")
	   public ResponseEntity<?> getInprogressAppointmentsByPatientId(
				 @PathVariable String patientId){
		   return bookingService.getInprogressBookingsByPatientId(patientId);
	 }
	   @GetMapping("/bookings/Inprogress/patientId/{patientId}/{clinicId}")
	   public ResponseEntity<?> getInprogressAppointmentsByPatientIdAndClinicId(
	           @PathVariable String patientId,
	           @PathVariable String clinicId) {

	       return bookingService.getInprogressBookingsByPatientIdAndClinicId(patientId, clinicId);
	   }
	   
	   
	   @GetMapping("/reprts/{clinicId}/{branchId}/{number}/{startDate}/{endDate}")
	   public ResponseEntity<?> getReprts(@PathVariable String clinicId,
			   @PathVariable String branchId,
			   @PathVariable Integer number,
			   @PathVariable  String startDate,
			   @PathVariable String endDate) {

	       return bookingService.getReprts(clinicId, branchId, number, startDate, endDate);
	   }
	   
}
