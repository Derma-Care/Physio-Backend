package com.dermacare.bookingService.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.dermacare.bookingService.dto.BookingInfoByInput;
import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.util.Response;

public interface BookingService_Service {

	public ResponseEntity<?> addService(BookingRequset req);
	public BookingResponse deleteService(String id);
	public BookingResponse getBookedService(String id);
	public List<BookingResponse> getBookedServices(String mobileNumber);
	public List<BookingResponse> getAllBookedServices();
	public List<BookingResponse> bookingByDoctorId(String doctorId);
	public List<BookingResponse> bookingByServiceId(String serviceId);
	public List<BookingResponse> bookingByClinicId(String clinicId);
	public ResponseEntity<?> updateAppointment(BookingResponse bookingResponse);
	public List<BookingResponse> bookingByBranchId(String branchId);
	public ResponseEntity<?> getAppointsByPatientId(String patientId);
	public ResponseEntity<?> getAppointsByInput(String input);
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String hospitalId,String doctorId);
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String hospitalId,String doctorId,String number);
	public ResponseEntity<?> getCompletedApntsByDoctorId(String hospitalId,String doctorId);
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String hospitalId,String doctorId);
	public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber);
	public ResponseEntity<?> getInProgressAppointments(String number);
	public ResponseEntity<?> retrieveOneWeekAppointments(String cinicId,String branchId);								
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId);
	public List<BookingResponse> getBookedServicesByClinicIdWithBranchId(String clinicId, String branchId);
	public ResponseEntity<?> retrieveAppointments(String cinicId,String branchId,String date);
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse dto);
	public ResponseEntity<?> getRelationsByCustomerId(String customerId);
	public List<BookingResponse> bookingByCustomerId(String customerId);
	public List<BookingResponse> bookingByPatientId(String patientId);
	public BookingInfoByInput bookingByInput(String input,String clinicId);
	public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId);
	public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId,String clinicId);
	public BookingResponse checkBookingByDateAndTime(String date,String time,String doctorId);
	public ResponseEntity<Response> getPatientAndPriceInfo(
	        String clinicId,
	        String branchId,
	        Integer number,
	        String startDate,
	        String endDate);
	
}
