package com.clinicadmin.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.clinicadmin.dto.BookingRequset;
import com.clinicadmin.dto.BookingResponse;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface BookingService {
	public Response deleteBookedService(String id);

	Response getAllBookedServicesDetailsByBranchId(String branchId);
	
	public ResponseEntity<ResponseStructure<List<BookingResponse>>> getBookingsByClinicIdWithBranchId(String clinicId, String branchId);
	public ResponseEntity<?> retrieveOneWeekAppointments(String clinicId, String branchId);

	public ResponseEntity<?> retrieveAppointnmentsByServiceDate(String clinicId, String branchId,String date);
	
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse bookingResponse);
 
	public ResponseEntity<?> retrieveAppointnmentsByInput(String input, String clinicId);

	ResponseEntity<?> retrieveAppointnmentsByPatientId(String patientId);

	Response bookService(BookingRequset req) throws JsonProcessingException;

	ResponseEntity<?> getInprogressBookingsByPatientId(String patientId);
	
	ResponseEntity<?> getInprogressBookingsByPatientIdAndClinicId(String patientId, String clinicId);
		
	public ResponseEntity<?> getReprts(String clinicId,
			String branchId,
			Integer number,
		    String startDate,
			String endDate);

}
