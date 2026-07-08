
package com.AdminService.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.AdminService.entity.QuestionsByPartEntity;
import com.AdminService.feign.CustomerFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerFeignImpl {

    private final CustomerFeign customerFeign;

    @CircuitBreaker(name = "customerService", fallbackMethod = "getByKeyFallback")
    @Retry(name = "customerService", fallbackMethod = "getByKeyFallback")
    public ResponseEntity<QuestionsByPartEntity> getByKey(String key) {
        return customerFeign.getByKey(key);
    }

    public ResponseEntity<QuestionsByPartEntity> getByKeyFallback(
            String key,
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
