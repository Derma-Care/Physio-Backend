package com.dermaCare.customerService.util;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.dermaCare.customerService.dto.BookingRequset;
import com.dermaCare.customerService.dto.BookingResponse;
import com.dermaCare.customerService.dto.FirstVisitHistoryRequest;
import com.dermaCare.customerService.dto.NotificationToCustomer;
import com.dermaCare.customerService.dto.PatientFeedbackDTO;
import com.dermaCare.customerService.dto.TempBlockingSlot;
import com.dermaCare.customerService.dto.TherapistRecordRequest;
import com.dermaCare.customerService.dto.VisitHistoryRequest;
import com.dermaCare.customerService.feignClient.BookingFeign;
import com.dermaCare.customerService.feignClient.ClinicAdminFeign;
import com.dermaCare.customerService.feignClient.NotificationFeign;
import com.dermaCare.customerService.feignClient.PhysioFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignImpl {
	
	private final BookingFeign bookingFeign;
	private final ClinicAdminFeign clinicAdminFeign;
	private final NotificationFeign notificationFeign;
	private final PhysioFeign physiotherapyFeign;
	
	
	  @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(String id) {
	        return bookingFeign.getBookedService(id);
	    }

	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(
	            String id,
	            Exception ex) {

	        log.error("Fallback executed for getBookedService : {}", ex.getMessage());

	        ResponseStructure<BookingResponse> response = new ResponseStructure<>();
	        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }

	    /* ================= BOOK SERVICE ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookServiceFallback")
	    @Retry(name = "bookingService", fallbackMethod = "bookServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> bookService(
	            BookingRequset req) {

	        return bookingFeign.bookService(req);
	    }

	    public ResponseEntity<ResponseStructure<BookingResponse>> bookServiceFallback(
	            BookingRequset req,
	            Exception ex) {

	        log.error("Fallback executed for bookService : {}", ex.getMessage());

	        ResponseStructure<BookingResponse> response = new ResponseStructure<>();
	        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }

	    /* ================= GET BOOKINGS BY CUSTOMER ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookingByCustomerIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getBookingByCustomerIdFallback")
	    public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getBookingByCustomerId(
	            String token,
	            String customerId) {

	        return bookingFeign.getBookingByCustomerId(token, customerId);
	    }

	    public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getBookingByCustomerIdFallback(
	            String token,
	            String customerId,
	            Exception ex) {

	        log.error("Fallback executed for getBookingByCustomerId : {}", ex.getMessage());

	        ResponseStructure<List<Map<String, Object>>> response = new ResponseStructure<>();
	        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }

	    /* ================= BOOK PHYSIO APPOINTMENT ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
	    @Retry(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
	    public ResponseEntity<?> bookPhysioAppointment(
	            BookingRequset req) {

	        return bookingFeign.bookPhysioAppointment(req);
	    }

	    public ResponseEntity<?> bookPhysioAppointmentFallback(
	            BookingRequset req,
	            Exception ex) {

	        log.error("Fallback executed for bookPhysioAppointment : {}", ex.getMessage());

	        com.dermaCare.customerService.util.Response response =
	                new com.dermaCare.customerService.util.Response();

	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }

	    /* ================= COMPLETED BOOKINGS ================= */

	    @CircuitBreaker(name = "bookingService", fallbackMethod = "getCompletedBookingByCustomerIdFallback")
	    @Retry(name = "bookingService", fallbackMethod = "getCompletedBookingByCustomerIdFallback")
	    public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getCompletedBookingByCustomerId(
	            String customerId) {

	        return bookingFeign.getCompletedBookingByCustomerId(customerId);
	    }

	    public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> getCompletedBookingByCustomerIdFallback(
	            String customerId,
	            Exception ex) {

	        log.error("Fallback executed for getCompletedBookingByCustomerId : {}", ex.getMessage());

	        ResponseStructure<List<Map<String, Object>>> response = new ResponseStructure<>();
	        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }
	    
	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorSlotFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getDoctorSlotFallback")
	    public ResponseEntity<Response> getDoctorSlot(
	            String hospitalId,
	            String branchId,
	            String doctorId) {

	        return clinicAdminFeign.getDoctorSlot(hospitalId, branchId, doctorId);
	    }

	    public ResponseEntity<Response> getDoctorSlotFallback(
	            String hospitalId,
	            String branchId,
	            String doctorId,
	            Exception ex) {

	        log.error("Fallback executed for getDoctorSlot : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= UPDATE DOCTOR SLOT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
	    public boolean updateDoctorSlotWhileBooking(
	            String token,
	            String doctorId,
	            String branchId,
	            String date,
	            String time) {

	        return clinicAdminFeign.updateDoctorSlotWhileBooking(
	                token, doctorId, branchId, date, time);
	    }

	    public boolean updateDoctorSlotWhileBookingFallback(
	            String token,
	            String doctorId,
	            String branchId,
	            String date,
	            String time,
	            Exception ex) {

	        log.error("Fallback executed for updateDoctorSlotWhileBooking : {}", ex.getMessage());
	        return false;
	    }

	    /* ================= GET REPORTS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getReportsBycustomerIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getReportsBycustomerIdFallback")
	    public ResponseEntity<Response> getReportsBycustomerId(String customerId) {

	        return clinicAdminFeign.getReportsBycustomerId(customerId);
	    }

	    public ResponseEntity<Response> getReportsBycustomerIdFallback(
	            String customerId,
	            Exception ex) {

	        log.error("Fallback executed for getReportsBycustomerId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= CUSTOMER LOGIN ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "loginFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "loginFallback")
	    public ResponseEntity<Response> login(Map<String, String> dto) {

	        return clinicAdminFeign.login(dto);
	    }

	    public ResponseEntity<Response> loginFallback(
	            Map<String, String> dto,
	            Exception ex) {

	        log.error("Fallback executed for login : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= RECOMMENDED CLINICS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndOnDoctorsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getRecommendedClinicsAndOnDoctorsFallback")
	    public ResponseEntity<Response> getRecommendedClinicsAndOnDoctors(
	            String keyPoints) {

	        return clinicAdminFeign.getRecommendedClinicsAndOnDoctors(keyPoints);
	    }

	    public ResponseEntity<Response> getRecommendedClinicsAndOnDoctorsFallback(
	            String keyPoints,
	            Exception ex) {

	        log.error("Fallback executed for getRecommendedClinicsAndOnDoctors : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= BLOCK SLOT ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "blockSlotFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "blockSlotFallback")
	    public boolean blockSlot(TempBlockingSlot tempBlockingSlot) {

	        return clinicAdminFeign.blockSlot(tempBlockingSlot);
	    }

	    public boolean blockSlotFallback(
	            TempBlockingSlot tempBlockingSlot,
	            Exception ex) {

	        log.error("Fallback executed for blockSlot : {}", ex.getMessage());
	        return false;
	    }

	    /* ================= THERAPIST SESSION DETAILS ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getTherapistSessionDetailsFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getTherapistSessionDetailsFallback")
	    public ResponseEntity<Response> getTherapistSessionDetails(
	            String token,
	            TherapistRecordRequest request) {

	        return clinicAdminFeign.getTherapistSessionDetails(token, request);
	    }

	    public ResponseEntity<Response> getTherapistSessionDetailsFallback(
	            String token,
	            TherapistRecordRequest request,
	            Exception ex) {

	        log.error("Fallback executed for getTherapistSessionDetails : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= STAFF INFO ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getStaffInfoFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getStaffInfoFallback")
	    public ResponseEntity<Response> getStaffInfo(
	            String token,
	            String hospitalId,
	            String branchId) {

	        return clinicAdminFeign.getStaffInfo(token, hospitalId, branchId);
	    }

	    public ResponseEntity<Response> getStaffInfoFallback(
	            String token,
	            String hospitalId,
	            String branchId,
	            Exception ex) {

	        log.error("Fallback executed for getStaffInfo : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }

	    /* ================= CREATE FEEDBACK ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "createFeedbackFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "createFeedbackFallback")
	    public Response createFeedback(
	            String token,
	            PatientFeedbackDTO dto) {

	        return clinicAdminFeign.createFeedback(token, dto);
	    }

	    public Response createFeedbackFallback(
	            String token,
	            PatientFeedbackDTO dto,
	            Exception ex) {

	        log.error("Fallback executed for createFeedback : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return response;
	    }

	    /* ================= GET PATIENT FEEDBACK ================= */

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getByClinicIdAndBranchIdAndPatirntIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getByClinicIdAndBranchIdAndPatirntIdFallback")
	    public ResponseEntity<Response> getByClinicIdAndBranchIdAndPatirntId(
	            String token,
	            String clinicId,
	            String branchId,
	            String patientId) {

	        return clinicAdminFeign.getByClinicIdAndBranchIdAndPatirntId(
	                token, clinicId, branchId, patientId);
	    }

	    public ResponseEntity<Response> getByClinicIdAndBranchIdAndPatirntIdFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String patientId,
	            Exception ex) {

	        log.error("Fallback executed for getByClinicIdAndBranchIdAndPatirntId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(503).body(response);
	    }
	    
	    
	    @CircuitBreaker(name = "notificationService", fallbackMethod = "customerNotificationFallback")
	    @Retry(name = "notificationService", fallbackMethod = "customerNotificationFallback")
	    public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotification(
	            String customerMobileNumber) {

	        return notificationFeign.customerNotification(customerMobileNumber);
	    }

	    public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotificationFallback(
	            String customerMobileNumber,
	            Exception ex) {

	        log.error("Fallback executed for customerNotification : {}", ex.getMessage());

	        ResBody<List<NotificationToCustomer>> response = new ResBody<>();
	        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	                .body(response);
	    }
	    
	    /* ================= VISIT HISTORY ================= */

	    @CircuitBreaker(name = "physioService", fallbackMethod = "getVisitHistoryByDoctorFallback")
	    @Retry(name = "physioService", fallbackMethod = "getVisitHistoryByDoctorFallback")
	    public ResponseEntity<Response> getVisitHistoryByDoctor(
	            String token,
	            VisitHistoryRequest request) {

	        return physiotherapyFeign.getVisitHistoryByDoctor(token, request);
	    }

	    public ResponseEntity<Response> getVisitHistoryByDoctorFallback(
	            String token,
	            VisitHistoryRequest request,
	            Exception ex) {

	        log.error("Fallback executed for getVisitHistoryByDoctor : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= FIRST VISIT HISTORY ================= */

	    @CircuitBreaker(name = "physioService", fallbackMethod = "getFirstVisitHistoryFallback")
	    @Retry(name = "physioService", fallbackMethod = "getFirstVisitHistoryFallback")
	    public ResponseEntity<Response> getFirstVisitHistory(
	            String token,
	            FirstVisitHistoryRequest request) {

	        return physiotherapyFeign.getFirstVisitHistory(token, request);
	    }

	    public ResponseEntity<Response> getFirstVisitHistoryFallback(
	            String token,
	            FirstVisitHistoryRequest request,
	            Exception ex) {

	        log.error("Fallback executed for getFirstVisitHistory : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= EXERCISE SESSIONS WITH RECORDS ================= */

	    @CircuitBreaker(name = "physioService", fallbackMethod = "getExerciseSessionsWithRecordsFallback")
	    @Retry(name = "physioService", fallbackMethod = "getExerciseSessionsWithRecordsFallback")
	    public ResponseEntity<Response> getExerciseSessionsWithRecords(
	            String token,
	            String clinicId,
	            String branchId,
	            String bookingId,
	            String patientId,
	            String therapistId,
	            String therapistRecordId) {

	        return physiotherapyFeign.getExerciseSessionsWithRecords(
	                token,
	                clinicId,
	                branchId,
	                bookingId,
	                patientId,
	                therapistId,
	                therapistRecordId);
	    }

	    public ResponseEntity<Response> getExerciseSessionsWithRecordsFallback(
	            String token,
	            String clinicId,
	            String branchId,
	            String bookingId,
	            String patientId,
	            String therapistId,
	            String therapistRecordId,
	            Exception ex) {

	        log.error("Fallback executed for getExerciseSessionsWithRecords : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }


	    /* ================= DOCTOR SAVE DETAILS ================= */

	    @CircuitBreaker(name = "physioService", fallbackMethod = "getDoctorSaveDetailsByCustomerIdFallback")
	    @Retry(name = "physioService", fallbackMethod = "getDoctorSaveDetailsByCustomerIdFallback")
	    public ResponseEntity<Response> getDoctorSaveDetailsByCustomerId(
	            String customerId) {

	        return physiotherapyFeign.getDoctorSaveDetailsByCustomerId(customerId);
	    }

	    public ResponseEntity<Response> getDoctorSaveDetailsByCustomerIdFallback(
	            String customerId,
	            Exception ex) {

	        log.error("Fallback executed for getDoctorSaveDetailsByCustomerId : {}", ex.getMessage());

	        Response response = new Response();
	        response.setStatus(503);
	        response.setMessage("Service is temporarily unavailable");

	        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	    }
}
