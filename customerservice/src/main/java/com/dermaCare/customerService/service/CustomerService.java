package com.dermaCare.customerService.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.dto.ConsultationDTO;
import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.dto.CustomerRatingDomain;
import com.dermaCare.customerService.dto.FavouriteDoctorsDTO;
import com.dermaCare.customerService.dto.FirstVisitHistoryRequest;
import com.dermaCare.customerService.dto.LoginDTO;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.dto.PatientFeedbackDTO;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.dto.TherapistRecordRequest;
import com.dermaCare.customerService.dto.VisitHistoryRequest;
import com.dermaCare.customerService.util.ResBody;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.ResponseStructure;
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
