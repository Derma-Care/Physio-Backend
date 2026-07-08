
package com.AdminService.util;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.AdminService.dto.BookingRequset;
import com.AdminService.dto.BookingResponse;
import com.AdminService.dto.BookingResponseDTO;
import com.AdminService.feign.BookingFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingFeignImpl {

    private final BookingFeign bookingFeign;
    
   
    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookServiceFallback")
    @Retry(name = "bookingService", fallbackMethod = "bookServiceFallback")
    public ResponseEntity<?> bookService(String token, BookingRequset req) {
        return bookingFeign.bookService(token, req);
    }
    public ResponseEntity<?> bookServiceFallback(String token, BookingRequset req, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getAllBookingsFallback")
    @Retry(name = "bookingService", fallbackMethod = "getAllBookingsFallback")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(String token, int page, int size) {
        return bookingFeign.getAllBookings(token, page, size);
    }
    public ResponseEntity<Page<BookingResponse>> getAllBookingsFallback(String token, int page, int size, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "deleteBookedServiceFallback")
    @Retry(name = "bookingService", fallbackMethod = "deleteBookedServiceFallback")
    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(String token, String id) {
        return bookingFeign.deleteBookedService(token, id);
    }
    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedServiceFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByDoctorIdFallback")
    @Retry(name = "bookingService", fallbackMethod = "bookingByDoctorIdFallback")
    public ResponseEntity<?> bookingByDoctorId(String token, String doctorId, int page, int size) {
        return bookingFeign.bookingByDoctorId(token, doctorId, page, size);
    }
    public ResponseEntity<?> bookingByDoctorIdFallback(String token, String doctorId, int page, int size, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
    @Retry(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
    public ResponseEntity<ResponseStructure<BookingResponseDTO>> getBookedService(String token, String id) {
        return bookingFeign.getBookedService(token, id);
    }
    public ResponseEntity<ResponseStructure<BookingResponseDTO>> getBookedServiceFallback(String token, String id, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByPatientIdFallback")
    @Retry(name = "bookingService", fallbackMethod = "bookingByPatientIdFallback")
    public ResponseEntity<?> bookingByPatientId(String token, String clinicId, String patientId, int page, int size) {
        return bookingFeign.bookingByPatientId(token, clinicId, patientId, page, size);
    }
    public ResponseEntity<?> bookingByPatientIdFallback(String token, String clinicId, String patientId, int page, int size, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
    @Retry(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
    public ResponseEntity<?> updateAppointmentBasedOnBookingId(String token, BookingResponseDTO dto) {
        return bookingFeign.updateAppointmentBasedOnBookingId(token, dto);
    }
    public ResponseEntity<?> updateAppointmentBasedOnBookingIdFallback(String token, BookingResponseDTO dto, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
    @Retry(name = "bookingService", fallbackMethod = "getPatientDetailsForConsentFormFallback")
    public ResponseEntity<Response> getPatientDetailsForConsentForm(
            String token, String bookingId, String patientId, String mobileNumber) {
        return bookingFeign.getPatientDetailsForConsentForm(token, bookingId, patientId, mobileNumber);
    }
    public ResponseEntity<Response> getPatientDetailsForConsentFormFallback(
            String token, String bookingId, String patientId, String mobileNumber, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "updateAppointmentFallback")
    @Retry(name = "bookingService", fallbackMethod = "updateAppointmentFallback")
    public ResponseEntity<?> updateAppointment(String token, BookingResponse bookingResponse) {
        return bookingFeign.updateAppointment(token, bookingResponse);
    }
    public ResponseEntity<?> updateAppointmentFallback(String token, BookingResponse bookingResponse, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServicesFallback")
    @Retry(name = "bookingService", fallbackMethod = "getBookedServicesFallback")
    public ResponseEntity<?> getBookedServices(String token, String mobileNumber, int page, int size) {
        return bookingFeign.getBookedServices(token, mobileNumber, page, size);
    }
    public ResponseEntity<?> getBookedServicesFallback(String token, String mobileNumber, int page, int size, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "getBookingByServiceIdFallback")
    @Retry(name = "bookingService", fallbackMethod = "getBookingByServiceIdFallback")
    public ResponseEntity<?> getBookingByServiceId(String token, String serviceId) {
        return bookingFeign.getBookingByServiceId(token, serviceId);
    }
    public ResponseEntity<?> getBookingByServiceIdFallback(String token, String serviceId, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByClinicIdFallback")
    @Retry(name = "bookingService", fallbackMethod = "bookingByClinicIdFallback")
    public ResponseEntity<?> bookingByClinicId(String token, String clinicId, int page, int size) {
        return bookingFeign.bookingByClinicId(token, clinicId, page, size);
    }
    public ResponseEntity<?> bookingByClinicIdFallback(String token, String clinicId, int page, int size, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "inProgressAppointmentsFallback")
    @Retry(name = "bookingService", fallbackMethod = "inProgressAppointmentsFallback")
    public ResponseEntity<?> inProgressAppointments(String token, String mobileNumber) {
        return bookingFeign.inProgressAppointments(token, mobileNumber);
    }
    public ResponseEntity<?> inProgressAppointmentsFallback(String token, String mobileNumber, Exception ex) {
        throw getFallbackException(ex);
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
    @Retry(name = "bookingService", fallbackMethod = "bookPhysioAppointmentFallback")
    public ResponseEntity<Response> bookPhysioAppointment(String token, BookingRequset req) {
        return bookingFeign.bookPhysioAppointment(token, req);
    }
    public ResponseEntity<Response> bookPhysioAppointmentFallback(String token, BookingRequset req, Exception ex) {
        throw getFallbackException(ex);
    }

    private RuntimeException getFallbackException(Exception ex) {

        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            return new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again after some time."
                    );      
        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service is temporarily unavailable"); 
    }else{ return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service is temporarily unavailable");}
    }

   }
