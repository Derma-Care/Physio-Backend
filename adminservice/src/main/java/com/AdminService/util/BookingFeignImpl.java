
package com.AdminService.util;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

    private ResponseEntity<?> buildFallbackResponse(Exception ex) {
        return ResponseEntity.status(503)
                .body("Booking Service is temporarily unavailable. Please try again later.");
    }

    @CircuitBreaker(name="adminService", fallbackMethod="bookServiceFallback")
    @Retry(name="adminService", fallbackMethod="bookServiceFallback")
    public ResponseEntity<?> bookService(String token, BookingRequset req){ return bookingFeign.bookService(token, req); }
    public ResponseEntity<?> bookServiceFallback(String token, BookingRequset req, Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="getAllBookingsFallback")
    @Retry(name="adminService", fallbackMethod="getAllBookingsFallback")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(String token,int page,int size){ return bookingFeign.getAllBookings(token,page,size); }
    public ResponseEntity<Page<BookingResponse>> getAllBookingsFallback(String token,int page,int size,Exception ex){ throw new RuntimeException(ex.getMessage());}

    @CircuitBreaker(name="adminService", fallbackMethod="deleteBookedServiceFallback")
    @Retry(name="adminService", fallbackMethod="deleteBookedServiceFallback")
    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedService(String token,String id){ return bookingFeign.deleteBookedService(token,id); }
    public ResponseEntity<ResponseStructure<BookingResponse>> deleteBookedServiceFallback(String token,String id,Exception ex){ throw new RuntimeException(ex.getMessage()); }

    @CircuitBreaker(name="adminService", fallbackMethod="bookingByDoctorIdFallback")
    @Retry(name="adminService", fallbackMethod="bookingByDoctorIdFallback")
    public ResponseEntity<?> bookingByDoctorId(String token,String doctorId,int page,int size){ return bookingFeign.bookingByDoctorId(token,doctorId,page,size); }
    public ResponseEntity<?> bookingByDoctorIdFallback(String token,String doctorId,int page,int size,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="getBookedServiceFallback")
    @Retry(name="adminService", fallbackMethod="getBookedServiceFallback")
    public  ResponseEntity<ResponseStructure<BookingResponseDTO>> getBookedService(String token,String id){ return bookingFeign.getBookedService(token,id); }
    public ResponseEntity<?> getBookedServiceFallback(String token,String id,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="bookingByPatientIdFallback")
    @Retry(name="adminService", fallbackMethod="bookingByPatientIdFallback")
    public ResponseEntity<?> bookingByPatientId(String token,String clinicId,String patientId,int page,int size){ return bookingFeign.bookingByPatientId(token,clinicId,patientId,page,size); }
    public ResponseEntity<?> bookingByPatientIdFallback(String token,String clinicId,String patientId,int page,int size,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="updateAppointmentBasedOnBookingIdFallback")
    @Retry(name="adminService", fallbackMethod="updateAppointmentBasedOnBookingIdFallback")
    public ResponseEntity<?> updateAppointmentBasedOnBookingId(String token,BookingResponseDTO dto){ return bookingFeign.updateAppointmentBasedOnBookingId(token,dto); }
    public ResponseEntity<?> updateAppointmentBasedOnBookingIdFallback(String token,BookingResponseDTO dto,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="getPatientDetailsForConsentFormFallback")
    @Retry(name="adminService", fallbackMethod="getPatientDetailsForConsentFormFallback")
    public  ResponseEntity<Response> getPatientDetailsForConsentForm(String token,String bookingId,String patientId,String mobileNumber){ return bookingFeign.getPatientDetailsForConsentForm(token,bookingId,patientId,mobileNumber); }
    public ResponseEntity<?> getPatientDetailsForConsentFormFallback(String token,String bookingId,String patientId,String mobileNumber,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="updateAppointmentFallback")
    @Retry(name="adminService", fallbackMethod="updateAppointmentFallback")
    public ResponseEntity<?> updateAppointment(String token,BookingResponse bookingResponse){ return bookingFeign.updateAppointment(token,bookingResponse); }
    public ResponseEntity<?> updateAppointmentFallback(String token,BookingResponse bookingResponse,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="getBookedServicesFallback")
    @Retry(name="adminService", fallbackMethod="getBookedServicesFallback")
    public ResponseEntity<?> getBookedServices(String token,String mobileNumber,int page,int size){ return bookingFeign.getBookedServices(token,mobileNumber,page,size); }
    public ResponseEntity<?> getBookedServicesFallback(String token,String mobileNumber,int page,int size,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="getBookingByServiceIdFallback")
    @Retry(name="adminService", fallbackMethod="getBookingByServiceIdFallback")
    public ResponseEntity<?> getBookingByServiceId(String token,String serviceId){ return bookingFeign.getBookingByServiceId(token,serviceId); }
    public ResponseEntity<?> getBookingByServiceIdFallback(String token,String serviceId,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="bookingByClinicIdFallback")
    @Retry(name="adminService", fallbackMethod="bookingByClinicIdFallback")
    public ResponseEntity<?> bookingByClinicId(String token,String clinicId,int page,int size){ return bookingFeign.bookingByClinicId(token,clinicId,page,size); }
    public ResponseEntity<?> bookingByClinicIdFallback(String token,String clinicId,int page,int size,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="inProgressAppointmentsFallback")
    @Retry(name="adminService", fallbackMethod="inProgressAppointmentsFallback")
    public ResponseEntity<?> inProgressAppointments(String token,String mobileNumber){ return bookingFeign.inProgressAppointments(token,mobileNumber); }
    public ResponseEntity<?> inProgressAppointmentsFallback(String token,String mobileNumber,Exception ex){ return buildFallbackResponse(ex); }

    @CircuitBreaker(name="adminService", fallbackMethod="bookPhysioAppointmentFallback")
    @Retry(name="adminService", fallbackMethod="bookPhysioAppointmentFallback")
    public ResponseEntity<Response>  bookPhysioAppointment(String token,BookingRequset req){ return bookingFeign.bookPhysioAppointment(token,req); }
    public ResponseEntity<?> bookPhysioAppointmentFallback(String token,BookingRequset req,Exception ex){ return buildFallbackResponse(ex); }
}
