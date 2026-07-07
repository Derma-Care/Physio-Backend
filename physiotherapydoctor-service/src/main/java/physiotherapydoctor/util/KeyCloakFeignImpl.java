package physiotherapydoctor.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.feign.KeyCloakFeign;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyCloakFeignImpl {

	private final KeyCloakFeign keyCloakFeign;

	private String getFallbackMessage(Throwable ex) {

		if (ex instanceof RequestNotPermitted) {
			return "Too many requests. Please try again later.";
		}

		if (ex instanceof CallNotPermittedException) {
			return "Keycloak Circuit Breaker is OPEN.";
		}

		if (ex instanceof FeignException) {
			return "Keycloak Service is unavailable.";
		}

		if (ex instanceof ConnectException) {
			return "Unable to connect to Keycloak.";
		}

		if (ex instanceof SocketTimeoutException) {
			return "Keycloak request timed out.";
		}

		return "Keycloak Service is temporarily unavailable.";
	}

	@CircuitBreaker(name = "keycloakService", fallbackMethod = "getTokenFallback")
	@Retry(name = "keycloakService", fallbackMethod = "getTokenFallback")
	@RateLimiter(name = "keycloakService", fallbackMethod = "getTokenFallback")
	public Map<String, Object> getToken(MultiValueMap<String, String> form) {

		return keyCloakFeign.getToken(form);
	}

	public Map<String, Object> getTokenFallback(MultiValueMap<String, String> form, Throwable ex) {

		log.error("Keycloak Token Generation Failed", ex);

		Map<String, Object> response = new HashMap<>();
		response.put("success", false);
		response.put("status", 503);
		response.put("message", getFallbackMessage(ex));

		return response;
	}

}