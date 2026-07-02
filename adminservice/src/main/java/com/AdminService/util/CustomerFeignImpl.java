
package com.AdminService.util;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.entity.QuestionsByPartEntity;
import com.AdminService.feign.CustomerFeign;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerFeignImpl {

    private final CustomerFeign customerFeign;

    @CircuitBreaker(name = "adminService", fallbackMethod = "getByKeyFallback")
    @Retry(name = "adminService", fallbackMethod = "getByKeyFallback")
    public ResponseEntity<QuestionsByPartEntity> getByKey(String key) {
        return customerFeign.getByKey(key);
    }

    public ResponseEntity<QuestionsByPartEntity> getByKeyFallback(
            String key,
            Exception ex) {

    	 throw new RuntimeException(ex.getMessage());
    }
}
