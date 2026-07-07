package physiotherapydoctor.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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

	private Response buildResponse(String message) {
		Response response = new Response();
		response.setSuccess(false);
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		response.setMessage(message);
		response.setData(null);
		return response;
	}

	private <T> ResponseStructure<T> buildResponseStructure(String message) {
		return ResponseStructure.buildResponse(null, message, HttpStatus.SERVICE_UNAVAILABLE,
				HttpStatus.SERVICE_UNAVAILABLE.value());
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

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	public ResponseEntity<ResponseStructure<BranchDTO>> getBranchByIdFallback(String branchId, Exception ex) {

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(buildResponseStructure(getFallbackMessage(ex)));
	}

	private String getFallbackMessage(Exception ex) {

		if (ex instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
			return "Too many requests. Please try again after some time.";
		}

		if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
			return "Admin Service is temporarily unavailable because the circuit breaker is OPEN.";
		}

		return "Admin Service is temporarily unavailable. Please try again later.";
	}
}