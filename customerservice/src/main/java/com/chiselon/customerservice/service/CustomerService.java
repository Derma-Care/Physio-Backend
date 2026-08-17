package com.chiselon.customerservice.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.chiselon.customerservice.dto.BookingRequset;
import com.chiselon.customerservice.dto.FirstVisitHistoryRequest;
import com.chiselon.customerservice.dto.NotificationToCustomer;
import com.chiselon.customerservice.dto.PatientFeedbackDTO;
import com.chiselon.customerservice.dto.TherapistRecordRequest;
import com.chiselon.customerservice.dto.VisitHistoryRequest;
import com.chiselon.customerservice.util.ResBody;
import com.chiselon.customerservice.util.Response;
import com.fasterxml.jackson.core.JsonProcessingException;


public interface CustomerService {
    //BOOKING MANAGENET
    
   public Response bookService(BookingRequset req) throws JsonProcessingException ;
    
	//NOTIFICATION
	public ResponseEntity<ResBody<List<NotificationToCustomer>>> notificationToCustomer(
			 String customerMobileNumber);

	public ResponseEntity<?> getBookingsByCustomerId(String customerId);

	public ResponseEntity<Response> getTherapistSessionDetails(TherapistRecordRequest request);

	public ResponseEntity<Response> getVisitHistoryByDoctor(VisitHistoryRequest request);
	
	public ResponseEntity<Response> getFirstVisitHistory(FirstVisitHistoryRequest request);
	public ResponseEntity<?> bookPhysioAppointment(BookingRequset req);
	public ResponseEntity<?> getCompletedBookingsByCustomerId(String customerId);

	public ResponseEntity<Response> getStaffInfo(
		       String hospitalId,
		        String branchId);

	public Response createFeedback(
	        PatientFeedbackDTO dto);

	
	public ResponseEntity<Response> getByClinicIdAndBranchId(
		      String clinicId,
		      String branchId,
		      String patientId);

	 public Response getDoctorsSlots(String hid,String hospitalId,String doctorId);
	 public Response getReportsAndDoctorSaveDetails(String customerId);


}
