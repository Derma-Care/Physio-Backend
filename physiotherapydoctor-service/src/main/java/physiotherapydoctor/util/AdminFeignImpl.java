package physiotherapydoctor.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import physiotherapydoctor.dto.BranchDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.feign.AdminFeignClient;

@Component
@RequiredArgsConstructor
public class AdminFeignImpl {

	private final AdminFeignClient adminFeignClient;
	private final KeyCloakTokenStore keyCloakTokenStore;

	private String token() {
		return keyCloakTokenStore.getAccess_token();
	}

	@CircuitBreaker(name = "adminService", fallbackMethod = "getClinicByIdFallback")
	@Retry(name = "adminService", fallbackMethod = "getClinicByIdFallback")
	@RateLimiter(name = "adminService", fallbackMethod = "getClinicByIdFallback")
	public ResponseEntity<Response> getClinicById(String clinicId) {
		return adminFeignClient.getClinicById(token(), clinicId);
	}

	@CircuitBreaker(name = "adminService", fallbackMethod = "getBranchByIdFallback")
	@Retry(name = "adminService", fallbackMethod = "getBranchByIdFallback")
	@RateLimiter(name = "adminService", fallbackMethod = "getBranchByIdFallback")
	public ResponseEntity<ResponseStructure<BranchDTO>> getBranchById(String branchId) {
		return adminFeignClient.getBranchById(token(), branchId);
	}

	// Common fallback
	public ResponseEntity<Response> getClinicByIdFallback(String clinicId, Exception ex) {

		throw getFallbackException(ex);	}

	public ResponseEntity<ResponseStructure<BranchDTO>> getBranchByIdFallback(String branchId, Exception ex) {

		throw getFallbackException(ex);}

	 private RuntimeException getFallbackException(Throwable ex) {

	        if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
	            return new ResponseStatusException(
	                    HttpStatus.TOO_MANY_REQUESTS,
	                    "Too many requests. Please try again after some time."
	                    );      
	        }else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
	            return new ResponseStatusException(
	                    HttpStatus.SERVICE_UNAVAILABLE,
	                    "Booking Service is temporarily unavailable"); 
	    }else{ return new ResponseStatusException(
	                HttpStatus.SERVICE_UNAVAILABLE,
	                "Booking Service is temporarily unavailable");}
	    }
}