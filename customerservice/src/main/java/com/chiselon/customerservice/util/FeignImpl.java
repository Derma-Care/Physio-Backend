package com.chiselon.customerservice.util;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.chiselon.customerservice.dto.BookingRequset;
import com.chiselon.customerservice.dto.BookingResponse;
import com.chiselon.customerservice.dto.FirstVisitHistoryRequest;
import com.chiselon.customerservice.dto.NotificationToCustomer;
import com.chiselon.customerservice.dto.PatientFeedbackDTO;
import com.chiselon.customerservice.dto.TempBlockingSlot;
import com.chiselon.customerservice.dto.TherapistRecordRequest;
import com.chiselon.customerservice.dto.VisitHistoryRequest;
import com.chiselon.customerservice.feignClient.BookingFeign;
import com.chiselon.customerservice.feignClient.ClinicAdminFeign;
import com.chiselon.customerservice.feignClient.NotificationFeign;
import com.chiselon.customerservice.feignClient.PhysioFeign;

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
	
	
	 private RuntimeException getFallbackException(Throwable ex) {

	        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking service is currently unavailable after multiple retry attempts."
	                    );      
	        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking Service is temporarily unavailable"); 
	    }else{ return new ResponseStatusException(
	                HttpStatus.TOO_MANY_REQUESTS,
	                "Too many requests. Please try again after some time.");}
	    }
	
	
	  /* ================= GET BOOKED SERVICE ================= */

	  @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	  @Retry(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	  public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(String id) {
	      return bookingFeign.getBookedService(id);
	  }

	  public ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(
	          String id,
	          Exception ex) {

	      throw getFallbackException(ex);
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

	      throw getFallbackException(ex);
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

	      throw getFallbackException(ex);
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

	      throw getFallbackException(ex);
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

	      throw getFallbackException(ex);
	  }

	  /* ================= DOCTOR SLOT ================= */

	  @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorSlotFallback")
	  @Retry(name = "clinicAdminService", fallbackMethod = "getDoctorSlotFallback")
	  public ResponseEntity<Response> getDoctorSlot(
	          String hospitalId,
	          String branchId,
	          String doctorId) {

	      return clinicAdminFeign.getDoctorSlot(
	              hospitalId,
	              branchId,
	              doctorId);
	  }

	  public ResponseEntity<Response> getDoctorSlotFallback(
	          String hospitalId,
	          String branchId,
	          String doctorId,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }
	  
	  
	    /* ================= UPDATE DOCTOR SLOT ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
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

	      throw getFallbackException(ex);
	  }

	  /* ================= GET REPORTS ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getReportsBycustomerIdFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "getReportsBycustomerIdFallback")
	  public ResponseEntity<Response> getReportsBycustomerId(
	          String customerId) {

	      return clinicAdminFeign.getReportsBycustomerId(customerId);
	  }

	  public ResponseEntity<Response> getReportsBycustomerIdFallback(
	          String customerId,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= CUSTOMER LOGIN ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "loginFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "loginFallback")
	  public ResponseEntity<Response> login(
	          Map<String, String> dto) {

	      return clinicAdminFeign.login(dto);
	  }

	  public ResponseEntity<Response> loginFallback(
	          Map<String, String> dto,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= RECOMMENDED CLINICS ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getRecommendedClinicsAndOnDoctorsFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "getRecommendedClinicsAndOnDoctorsFallback")
	  public ResponseEntity<Response> getRecommendedClinicsAndOnDoctors(
	          String keyPoints) {

	      return clinicAdminFeign.getRecommendedClinicsAndOnDoctors(keyPoints);
	  }

	  public ResponseEntity<Response> getRecommendedClinicsAndOnDoctorsFallback(
	          String keyPoints,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= BLOCK SLOT ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "blockSlotFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "blockSlotFallback")
	  public boolean blockSlot(
	          TempBlockingSlot tempBlockingSlot) {

	      return clinicAdminFeign.blockSlot(tempBlockingSlot);
	  }

	  public boolean blockSlotFallback(
	          TempBlockingSlot tempBlockingSlot,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= THERAPIST SESSION DETAILS ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getTherapistSessionDetailsFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "getTherapistSessionDetailsFallback")
	  public ResponseEntity<Response> getTherapistSessionDetails(
	          String token,
	          TherapistRecordRequest request) {

	      return clinicAdminFeign.getTherapistSessionDetails(token, request);
	  }

	  public ResponseEntity<Response> getTherapistSessionDetailsFallback(
	          String token,
	          TherapistRecordRequest request,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }
	    /* ================= STAFF INFO ================= */

	  /* ================= GET STAFF INFO ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getStaffInfoFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "getStaffInfoFallback")
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

	      throw getFallbackException(ex);
	  }

	  /* ================= CREATE FEEDBACK ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "createFeedbackFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "createFeedbackFallback")
	  public Response createFeedback(
	          String token,
	          PatientFeedbackDTO dto) {

	      return clinicAdminFeign.createFeedback(token, dto);
	  }

	  public Response createFeedbackFallback(
	          String token,
	          PatientFeedbackDTO dto,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= GET PATIENT FEEDBACK ================= */

	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getByClinicIdAndBranchIdAndPatirntIdFallback")
	  @Retry(name = "clinicAdminadminService", fallbackMethod = "getByClinicIdAndBranchIdAndPatirntIdFallback")
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

	      throw getFallbackException(ex);
	  }

	  /* ================= CUSTOMER NOTIFICATION ================= */

	  @CircuitBreaker(name = "notificationService", fallbackMethod = "customerNotificationFallback")
	  @Retry(name = "notificationService", fallbackMethod = "customerNotificationFallback")
	  public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotification(
	          String customerMobileNumber) {

	      return notificationFeign.customerNotification(customerMobileNumber);
	  }

	  public ResponseEntity<ResBody<List<NotificationToCustomer>>> customerNotificationFallback(
	          String customerMobileNumber,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= VISIT HISTORY ================= */

	  @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getVisitHistoryByDoctorFallback")
	  @Retry(name = "physioDoctorService", fallbackMethod = "getVisitHistoryByDoctorFallback")
	  public ResponseEntity<Response> getVisitHistoryByDoctor(
	          String token,
	          VisitHistoryRequest request) {

	      return physiotherapyFeign.getVisitHistoryByDoctor(token, request);
	  }

	  public ResponseEntity<Response> getVisitHistoryByDoctorFallback(
	          String token,
	          VisitHistoryRequest request,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= FIRST VISIT HISTORY ================= */

	  @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getFirstVisitHistoryFallback")
	  @Retry(name = "physioDoctorService", fallbackMethod = "getFirstVisitHistoryFallback")
	  public ResponseEntity<Response> getFirstVisitHistory(
	          String token,
	          FirstVisitHistoryRequest request) {

	      return physiotherapyFeign.getFirstVisitHistory(token, request);
	  }

	  public ResponseEntity<Response> getFirstVisitHistoryFallback(
	          String token,
	          FirstVisitHistoryRequest request,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }

	  /* ================= EXERCISE SESSIONS WITH RECORDS ================= */

	  @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getExerciseSessionsWithRecordsFallback")
	  @Retry(name = "physioDoctorService", fallbackMethod = "getExerciseSessionsWithRecordsFallback")
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

	      throw getFallbackException(ex);
	  }

	  /* ================= DOCTOR SAVE DETAILS ================= */

	  @CircuitBreaker(name = "physioDoctorService", fallbackMethod = "getDoctorSaveDetailsByCustomerIdFallback")
	  @Retry(name = "physioDoctorService", fallbackMethod = "getDoctorSaveDetailsByCustomerIdFallback")
	  public ResponseEntity<Response> getDoctorSaveDetailsByCustomerId(
	          String customerId) {

	      return physiotherapyFeign.getDoctorSaveDetailsByCustomerId(customerId);
	  }

	  public ResponseEntity<Response> getDoctorSaveDetailsByCustomerIdFallback(
	          String customerId,
	          Exception ex) {

	      throw getFallbackException(ex);
	  }
}
