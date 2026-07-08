package com.dermacare.notification_service.util;

import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;

@Component
public class JwtUtil {
	
	
	@RateLimiter(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
	@Retry(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
	@CircuitBreaker(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
	public Map<Object,Object> tokenIntrospection(String token) {
		  Map<Object,Object> map = null;
		 if(token != null) {
			  RestTemplate restTemplate = new RestTemplate();

			   				 String introspectionUrl =
			                "http://localhost:9094/realms/physiocare/protocol/openid-connect/token/introspect";

			        HttpHeaders headers = new HttpHeaders();
			        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			        form.add("token", token);
			        form.add("client_id", "admin-service");
			        form.add("client_secret", "QR5OBooIYbefMFXbf5TMR2EJ2SS9xMRI");

			        HttpEntity<MultiValueMap<String, String>> request =
			                new HttpEntity<>(form, headers);

			        ResponseEntity<Object> response = restTemplate.exchange(
			                introspectionUrl,
			                HttpMethod.POST,
			                request,
			                Object.class
			        );

			        map = new ObjectMapper().convertValue(response.getBody(),new TypeReference<Map<Object,Object>>() {
					});
			    }
		 return map;
}
	
	
	
	public Map<Object,Object> tokenIntrospectionFallback(String token){
				
		return null;
	}
	
		}
	

