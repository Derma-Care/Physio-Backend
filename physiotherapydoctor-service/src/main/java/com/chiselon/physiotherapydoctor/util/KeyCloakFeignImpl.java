package com.chiselon.physiotherapydoctor.util;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.chiselon.physiotherapydoctor.feign.KeyCloakFeign;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyCloakFeignImpl {

	private final KeyCloakFeign keyCloakFeign;

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

	@CircuitBreaker(name = "keycloak", fallbackMethod = "getTokenFallback")
	@Retry(name = "keycloak", fallbackMethod = "getTokenFallback")
	@RateLimiter(name = "keycloak", fallbackMethod = "getTokenFallback")
	public Map<String, Object> getToken(MultiValueMap<String, String> form) {

		return keyCloakFeign.getToken(form);
	}

	public Map<String, Object> getTokenFallback(MultiValueMap<String, String> form, Throwable ex) {

		throw getFallbackException(ex);
	}

}