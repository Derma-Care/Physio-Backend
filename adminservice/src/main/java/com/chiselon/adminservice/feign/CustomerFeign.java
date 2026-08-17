package com.chiselon.adminservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.chiselon.adminservice.entity.QuestionsByPartEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@FeignClient(name = "customerservice")
@CircuitBreaker(name = "circuitBreaker", fallbackMethod = "customerServiceFallBack")
public interface CustomerFeign {
	
	@GetMapping("/api/customer/getByKey/{key}")
	public ResponseEntity<QuestionsByPartEntity> getByKey(@PathVariable String key);

}
