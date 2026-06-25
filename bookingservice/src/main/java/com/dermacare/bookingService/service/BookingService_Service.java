package com.dermacare.bookingService.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import com.dermacare.bookingService.dto.BookingInfoByInput;
import com.dermacare.bookingService.dto.BookingRequset;
import com.dermacare.bookingService.dto.BookingResponse;
import com.dermacare.bookingService.dto.ReportsDTO;
import com.dermacare.bookingService.util.Response;
import com.dermacare.bookingService.util.ResponseStructure;

public interface BookingService_Service {

	public ResponseEntity<?> followUpBooking(BookingResponse req);
	public BookingResponse deleteService(String id);
	public BookingResponse getBookedService(String id);

	public Page<BookingResponse> getBookedServices(
	        String mobileNumber,
	        int page,
	        int size);
	public Page<BookingResponse> getAllBookedServices(int page, int size);

	public Page<BookingResponse> bookingByDoctorId(
	        String doctorId,
	        int page,
	        int size);
	///public List<BookingResponse> bookingByServiceId(String serviceId);
	public ResponseEntity<?> bookingByClinicId(
	        String clinicId,
	        int page,
	        int size);


	//public ResponseEntity<?> updateAppointment(BookingResponse bookingResponse);
	public Page<BookingResponse> bookingByBranchId(
	        String branchId,
	        int page,
	        int size);
	public ResponseEntity<?> getAppointsByPatientId(String patientId, int page, int size);
	public ResponseEntity<?> getAppointsByInput(
	        String input,
	        int page,
	        int size);
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(
	        String clinicId,
	        String doctorId,
	        int page,
	        int size);
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String hospitalId,String doctorId,String number);
	public ResponseEntity<?> getCompletedApntsByDoctorId(String hospitalId,String doctorId);
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String hospitalId,String doctorId);
	public Response getPatientDetailsForConsetForm(String bookingId, String patientId, String mobileNumber);

	public ResponseEntity<?> getInProgressAppointments(
	        String number,
	        int page,
	        int size);
	public ResponseEntity<?> retrieveOneWeekAppointments(
	        String clinicId,
	        String branchId,
	        int page,
	        int size);
	public ResponseEntity<?> getDoctorFutureAppointments(
	        String doctorId,
	        int page,
	        int size);
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchId(
	        String clinicId,
	        String branchId,
	        int page,
	        int size);
	public ResponseEntity<?> retrieveAppointments(String cinicId,String branchId,String date);
	public ResponseEntity<ResponseStructure<BookingResponse>> updateAppointmentBasedOnBookingId(BookingResponse dto);
	public ResponseEntity<?> getRelationsByCustomerId(String customerId);

//	public ResponseEntity<?> bookingByCustomerId(
//	        String customerId,
//	        int page,
//	        int size) ;

	public Page<BookingResponse> bookingByPatientId(String clincId,String patientId, int page, int size) ;
		//public BookingInfoByInput bookingByInput(String input,String clinicId);

	public ResponseEntity<?> getInProgressAppointmentsByCustomerId(String customerId);
	public ResponseEntity<?> getInProgressAppointmentsByPatientId(String patientId,String clinicId);
	public BookingResponse checkBookingByDateAndTime(String date,String time,String doctorId);
	public ResponseEntity<Response> getPatientAndPriceInfo(
	        String clinicId,
	        String branchId,
	        Integer number,
	        String startDate,
	        String endDate);

	public ResponseEntity<?> getTodayBookings(
	        String cId,
	        String bId,
	        int page,
	        int size);

public ResponseEntity<?> physioAppointment(BookingRequset request);
public ResponseEntity<Response> getTodayAllBookings(
        String clinicId,
        String branchId,
        int page,
        int size);
public ResponseEntity<Response> getUpcomingBookings(
        String clinicId,
        String branchId,
        int option,
        int page,
        int size);

public ResponseEntity<Response> getBookingByDate(String clinicId, String branchId,String date);
public ResponseEntity<Response> getBookingByCustomRange(
        String clinicId,
        String branchId,
        String start,
        String end,
        int page,
        int size);
public ResponseEntity<Response> getBookingById(String bookingId);

public Page<BookingResponse> bookingByPatientIdAndBookingId(
        String patientId,
        String bookingId,
        int page,
        int size);

public List<ReportsDTO> getReportsByPatientId(String patientId);
public void deleteBookedServiceReports(String bookingId,String index);
public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(
        String clinicId,
        String branchId,
        String doctorId,

        String status,
        int page,
        int size);

public List<Map<String, Object>> CompletedbookingByCustomerId(String customerId);
List<Map<String, Object>> searchBookings(String clinicId, String input);

public List<Map<String, Object>> bookingByCustomerId(String customerId);

public ResponseEntity<Response> getTodayBookings(String clinicId, String branchId) ;


	   	
}
