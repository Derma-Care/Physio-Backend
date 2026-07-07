package physiotherapydoctor.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.NotificationDTO;
import physiotherapydoctor.dto.ResBody;
import physiotherapydoctor.feign.NotificationFeign;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationFeignImpl {

	private final NotificationFeign notificationFeign;

	private String getFallbackMessage(Throwable ex) {

		if (ex instanceof RequestNotPermitted) {
			return "Too many requests. Please try again later.";
		}

		if (ex instanceof CallNotPermittedException) {
			return "Notification Service Circuit Breaker is OPEN.";
		}

		if (ex instanceof FeignException) {
			return "Notification Service is unavailable.";
		}

		if (ex instanceof ConnectException) {
			return "Unable to connect to Notification Service.";
		}

		if (ex instanceof SocketTimeoutException) {
			return "Notification Service timed out.";
		}

		return "Notification Service is temporarily unavailable.";
	}

	@CircuitBreaker(name = "notificationService", fallbackMethod = "notificationtodoctorFallback")
	@Retry(name = "notificationService", fallbackMethod = "notificationtodoctorFallback")
	@RateLimiter(name = "notificationService", fallbackMethod = "notificationtodoctorFallback")
	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctor(String hospitalId, String doctorId) {

		return notificationFeign.notificationtodoctor(hospitalId, doctorId);
	}

	public ResponseEntity<ResBody<List<NotificationDTO>>> notificationtodoctorFallback(String hospitalId,
			String doctorId, Throwable ex) {

		log.error("Notification failed hospitalId={}, doctorId={}", hospitalId, doctorId, ex);

		ResBody<List<NotificationDTO>> response = new ResBody<>();
		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		response.setMessage(getFallbackMessage(ex));
		response.setData(null);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}

}