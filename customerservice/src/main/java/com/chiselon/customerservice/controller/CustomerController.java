package com.chiselon.customerservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chiselon.customerservice.dto.BookingRequset;
import com.chiselon.customerservice.dto.ExerciseSessionsWithRecords;
import com.chiselon.customerservice.dto.FirstVisitHistoryRequest;
import com.chiselon.customerservice.dto.NotificationToCustomer;
import com.chiselon.customerservice.dto.PatientFeedbackDTO;
import com.chiselon.customerservice.dto.TherapistRecordRequest;
import com.chiselon.customerservice.dto.VisitHistoryRequest;
import com.chiselon.customerservice.entity.QuestionsByPartEntity;
import com.chiselon.customerservice.service.CustomerService;
import com.chiselon.customerservice.service.PhysiotherapyService;
import com.chiselon.customerservice.util.GetByKey;
import com.chiselon.customerservice.util.ResBody;
import com.chiselon.customerservice.util.Response;
import com.fasterxml.jackson.core.JsonProcessingException;


@RestController
@RequestMapping("/customer")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CustomerController {

	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private GetByKey getByKey;
	
	@Autowired
	private PhysiotherapyService service;

   
   @GetMapping("/getDoctorSlots/{hospitalId}/{branchId}/{doctorId}")
   public ResponseEntity<Response> getDoctorSlots(@PathVariable String hospitalId,@PathVariable String branchId,@PathVariable String doctorId){
   	Response response = customerService.getDoctorsSlots(hospitalId,branchId,doctorId);
   	if(response != null && response.getStatus() != 0) {
  		 return ResponseEntity.status(response.getStatus()).body(response);
  	 }else {
  			return null;
	}}

   	   
// BOOKING APIS

@PostMapping("/bookService")
public ResponseEntity<Object> bookService(@RequestBody BookingRequset req)throws JsonProcessingException  {
	Response response = customerService.bookService(req);
	if(response != null && response.getData() == null) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else if(response != null && response.getData() != null) {
		 return ResponseEntity.status(response.getStatus()).body(response.getData());}
		 else {
			 return null;
		 }
	}


   //NOTIFICATION
   
   @GetMapping("/customerNotification/{customerMobileNumber}")
   public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 @PathVariable String customerMobileNumber){
	   return customerService.notificationToCustomer(customerMobileNumber);
   }
   
   //booking api
   
   @GetMapping("/bookings/customerId/{customerId}")
   public ResponseEntity<?> getBookingsByCustomerId(
			 @PathVariable String customerId){
	   return customerService.getBookingsByCustomerId(customerId);
 }
   
   @GetMapping("/booking/completed/customerId/{customerId}")
   public ResponseEntity<?> getCompletedBookingsByCustomerId(
			 @PathVariable String customerId){
	   return customerService.getCompletedBookingsByCustomerId(customerId);
 }

    
   @GetMapping("getByKey/{key}")
   public ResponseEntity<QuestionsByPartEntity> getByKey(@PathVariable String key) {
       QuestionsByPartEntity response = getByKey.getByKey(key);

       if (response != null) {
           return ResponseEntity.ok(response);
       } else {
           return ResponseEntity.notFound().build();
       }
   }
   
   @PostMapping("/getTherapistSessionDetails")
   public ResponseEntity<?> getTherapistSessionDetails(@RequestBody TherapistRecordRequest dto){
	   return customerService.getTherapistSessionDetails(dto);
   }
   
   @PostMapping("/visit-history")
   public ResponseEntity<Response> getVisitHistoryByDoctor(
           @RequestBody VisitHistoryRequest request) {

       return customerService.getVisitHistoryByDoctor(request);
   }


   @PostMapping("/first-visit-history")
   public ResponseEntity<Response> getFirstVisitHistory(
           @RequestBody FirstVisitHistoryRequest request) {

       return customerService.getFirstVisitHistory(request);
   }
   
   @PostMapping("/bookPhysioAppointment")
   public ResponseEntity<?> bookPhysioAppointment(
		   @RequestBody BookingRequset req) {

       return customerService.bookPhysioAppointment(req);
   }
   
   @PostMapping("/getExerciseSessionsWithRecords")
   public ResponseEntity<Response> getExerciseSessionsWithRecords(@RequestBody ExerciseSessionsWithRecords  dto) {
       return service.getExerciseSessionsWithRecords(dto.getClinicId(), dto.getBranchId(), dto.getBookingId(), dto.getPatientId(), dto.getTherapistId(), dto.getTherapistRecordId());
   }
   
   @GetMapping("/staff-info/{hospitalId}/{branchId}")
   public ResponseEntity<Response> getStaffInfo(
	        @PathVariable String hospitalId,
	        @PathVariable String branchId){
   return customerService.getStaffInfo(hospitalId, branchId);
   }

   @PostMapping("/createPatientFeedback")
   public ResponseEntity<Response> createFeedback(
           @RequestBody PatientFeedbackDTO dto) {
	   Response res = customerService.createFeedback(dto);
	   return ResponseEntity.status(res.getStatus()).body(res);
   }
   
   @GetMapping("/getByPatientFeedbackClinicIdAndBranchId/{clinicId}/{branchId}/{patientId}")
   public ResponseEntity<Response> getByClinicIdAndBranchId(
           @PathVariable String clinicId,
           @PathVariable String branchId,
           @PathVariable String patientId){
	   return customerService.getByClinicIdAndBranchId(clinicId, branchId,patientId);
   }
   
 @GetMapping("/getReports/{customerId}")
 public ResponseEntity<Response> getReports(@PathVariable String customerId){
 	Response response = customerService.getReportsAndDoctorSaveDetails(customerId);
 	if(response != null && response.getStatus() != 0) {
		 return ResponseEntity.status(response.getStatus()).body(response);
	 }else {
			return null;
	}}


}
