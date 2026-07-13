package com.dermacare.notification_service.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dermacare.notification_service.dto.BookingResponse;
import com.dermacare.notification_service.dto.CustomerOnbordingDTO;
import com.dermacare.notification_service.dto.ImageForNotificationDto;
import com.dermacare.notification_service.dto.Response;
import com.dermacare.notification_service.dto.ResponseStructure;
import com.dermacare.notification_service.feign.BookServiceFeign;
import com.dermacare.notification_service.feign.CllinicFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeignImpl {
	
    private final CllinicFeign clinicFeign;    
    private final KeyCloakTokenStore KeyCloakTokenStore;
    private final BookServiceFeign bookingFeign; 
    
	
	  @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDeviceIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getDeviceIdFallback")
	    public String getDeviceId(
	            String clinicId,
	            String branchId) {

	        return clinicFeign.getDeviceId(KeyCloakTokenStore.getAccess_token(),clinicId, branchId);
	    }

	    public String getDeviceIdFallback(
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "customerDeviceIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "customerDeviceIdFallback")
	    public String customerDeviceId(
	            String customerId) {

	        return clinicFeign.customerDeviceId(KeyCloakTokenStore.getAccess_token(),customerId);
	    }

	    public String customerDeviceIdFallback(
	            String customerId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorDeviceIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getDoctorDeviceIdFallback")
	    public String getDoctorDeviceId(
	            String doctorId) {

	        return clinicFeign.getDoctorDeviceId(KeyCloakTokenStore.getAccess_token(),doctorId);
	    }

	    public String getDoctorDeviceIdFallback(
	            String doctorId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }
	    
	 
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getDoctorByIdFallback")
	    public ResponseEntity<Response> getDoctorById(String id) {
	        return clinicFeign.getDoctorById(KeyCloakTokenStore.getAccess_token(),id);
	    }

	    public ResponseEntity<Response> getDoctorByIdFallback(String id, Exception ex) {
	        throw getFallbackException(ex);
	    }

	  
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "updateDoctorSlotWhileBookingFallback")
	    public Boolean updateDoctorSlotWhileBooking(
	            String token,
	            String doctorId,
	            String date,
	            String time) {

	        return clinicFeign.updateDoctorSlotWhileBooking(
	                token, doctorId, date, time);
	    }

	    public Boolean updateDoctorSlotWhileBookingFallback(
	            String token,
	            String doctorId,
	            String date,
	            String time,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	  
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "makingFalseDoctorSlotFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "makingFalseDoctorSlotFallback")
	    public boolean makingFalseDoctorSlot(
	            String token,
	            String doctorId,
	            String branchId,
	            String date,
	            String time) {

	        return clinicFeign.makingFalseDoctorSlot(
	                token, doctorId, branchId, date, time);
	    }

	    public boolean makingFalseDoctorSlotFallback(
	            String token,
	            String doctorId,
	            String branchId,
	            String date,
	            String time,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "uploadImageForNotificationFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "uploadImageForNotificationFallback")
	    public ResponseEntity<?> uploadImageForNotification(
	            String token,
	            ImageForNotificationDto imageForNotificationDto) {

	        return clinicFeign.uploadImageForNotification(
	                token, imageForNotificationDto);
	    }

	    public ResponseEntity<?> uploadImageForNotificationFallback(
	            String token,
	            ImageForNotificationDto imageForNotificationDto,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	   
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getAllCustomersFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getAllCustomersFallback")
	    public ResponseEntity<Response> getAllCustomers(String token) {
	        return clinicFeign.getAllCustomers(token);
	    }

	    public ResponseEntity<Response> getAllCustomersFallback(
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	  
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "getCustomerByTokenFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "getCustomerByTokenFallback")
	    public CustomerOnbordingDTO getCustomerByToken(
	            String auth,
	            String token) {

	        return clinicFeign.getCustomerByToken(auth, token);
	    }

	    public CustomerOnbordingDTO getCustomerByTokenFallback(
	            String auth,
	            String token,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	   
	    @CircuitBreaker(name = "clinicAdminService", fallbackMethod = "retrivetherapistDeviceIdFallback")
	    @Retry(name = "clinicAdminService", fallbackMethod = "retrivetherapistDeviceIdFallback")
	    public String retrivetherapistDeviceId(
	            String token,
	            String deviceId) {

	        return clinicFeign.retrivetherapistDeviceId(
	                token, deviceId);
	    }

	    public String retrivetherapistDeviceIdFallback(
	            String token,
	            String deviceId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }
	    
	    

	    @CircuitBreaker(
	            name = "bookingService",
	            fallbackMethod = "getBookedServiceFallback")
	    @Retry(
	            name = "bookingService",
	            fallbackMethod = "getBookedServiceFallback")
	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(
	            String token,
	            String id) {

	        return bookingFeign.getBookedService(token, id);
	    }

	    public ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(
	            String token,
	            String id,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	  
	    @CircuitBreaker(
	            name = "bookingService",
	            fallbackMethod = "updateAppointmentFallback")
	    @Retry(
	            name = "bookingService",
	            fallbackMethod = "updateAppointmentFallback")
	    public ResponseEntity<?> updateAppointment(
	            String token,
	            BookingResponse bookingResponse) {

	        return bookingFeign.updateAppointment(
	                token,
	                bookingResponse);
	    }

	    public ResponseEntity<?> updateAppointmentFallback(
	            String token,
	            BookingResponse bookingResponse,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

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

}
