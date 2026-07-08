package com.dermacare.notification_service.util;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
	
	  @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getDeviceIdFallback")
	    @Retry(name = "clinicAdminadminService", fallbackMethod = "getDeviceIdFallback")
	    public String getDeviceId(
	            String clinicId,
	            String branchId) {

	        return clinicFeign.getDeviceId(clinicId, branchId);
	    }

	    public String getDeviceIdFallback(
	            String clinicId,
	            String branchId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "customerDeviceIdFallback")
	    @Retry(name = "clinicAdminadminService", fallbackMethod = "customerDeviceIdFallback")
	    public String customerDeviceId(
	            String customerId) {

	        return clinicFeign.customerDeviceId(customerId);
	    }

	    public String customerDeviceIdFallback(
	            String customerId,
	            Exception ex) {

	        throw getFallbackException(ex);
	    }

	    @CircuitBreaker(name = "clinicAdminadminService", fallbackMethod = "getDoctorDeviceIdFallback")
	    @Retry(name = "clinicAdminadminService", fallbackMethod = "getDoctorDeviceIdFallback")
	    public String getDoctorDeviceId(
	            String doctorId) {

	        return clinicFeign.getDoctorDeviceId(doctorId);
	    }

	    public String getDoctorDeviceIdFallback(
	            String doctorId,
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
