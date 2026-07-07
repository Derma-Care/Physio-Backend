package physiotherapydoctor.util;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
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
import physiotherapydoctor.dto.BookingRequset;
import physiotherapydoctor.dto.BookingResponse;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.ResponseStructure;
import physiotherapydoctor.feign.BookingFeignClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingFeignImpl {

	private final BookingFeignClient bookingFeignClient;
	private final KeyCloakTokenStore keyCloakTokenStore;

	private String token() {
		return keyCloakTokenStore.getAccess_token();
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	@Retry(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getBookedServiceFallback")
	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedService(String id) {

		return bookingFeignClient.getBookedService(token(), id);
	}

	public ResponseEntity<ResponseStructure<BookingResponse>> getBookedServiceFallback(String id, Throwable ex) {

		log.error("Booking Service failed while fetching bookingId={}", id, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(buildResponseStructure(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByPatientIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "bookingByPatientIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "bookingByPatientIdFallback")
	public ResponseEntity<Page<BookingResponse>> bookingByPatientId(String clinicId, String patientId, int page,
			int size) {

		return bookingFeignClient.bookingByPatientId(token(), clinicId, patientId, page, size);
	}

	public ResponseEntity<Page<BookingResponse>> bookingByPatientIdFallback(String clinicId, String patientId, int page,
			int size, Throwable ex) {

		log.error("BookingByPatientId failed clinicId={}, patientId={}", clinicId, patientId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getAppointsByInputFallback")
	@Retry(name = "bookingService", fallbackMethod = "getAppointsByInputFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getAppointsByInputFallback")
	public ResponseEntity<?> getAppointsByInput(String input) {

		return bookingFeignClient.getAppointsByInput(token(), input);
	}

	public ResponseEntity<?> getAppointsByInputFallback(String input, Throwable ex) {

		log.error("Search Appointment failed input={}", input, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getTodayDoctorAppointmentsByDoctorIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "getTodayDoctorAppointmentsByDoctorIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getTodayDoctorAppointmentsByDoctorIdFallback")
	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorId(String clinicId, String doctorId, int page,
			int size) {

		return bookingFeignClient.getTodayDoctorAppointmentsByDoctorId(token(), clinicId, doctorId, page, size);
	}

	public ResponseEntity<?> getTodayDoctorAppointmentsByDoctorIdFallback(String clinicId, String doctorId, int page,
			int size, Throwable ex) {

		log.error("Today's appointments failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "filterDoctorAppointmentsByDoctorIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "filterDoctorAppointmentsByDoctorIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "filterDoctorAppointmentsByDoctorIdFallback")
	public ResponseEntity<?> filterDoctorAppointmentsByDoctorId(String clinicId, String doctorId, String number) {

		return bookingFeignClient.filterDoctorAppointmentsByDoctorId(token(), clinicId, doctorId, number);
	}

	public ResponseEntity<?> filterDoctorAppointmentsByDoctorIdFallback(String clinicId, String doctorId, String number,
			Throwable ex) {

		log.error("Filter appointments failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "completedAppointmentsFallback")
	@Retry(name = "bookingService", fallbackMethod = "completedAppointmentsFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "completedAppointmentsFallback")
	public ResponseEntity<?> completedAppointments(String clinicId, String doctorId) {

		return bookingFeignClient.filterDoctorAppointmentsByDoctorId(token(), clinicId, doctorId);
	}

	public ResponseEntity<?> completedAppointmentsFallback(String clinicId, String doctorId, Throwable ex) {

		log.error("Completed appointments failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getSizeOfConsultationTypesByDoctorIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "getSizeOfConsultationTypesByDoctorIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getSizeOfConsultationTypesByDoctorIdFallback")
	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorId(String clinicId, String doctorId) {

		return bookingFeignClient.getSizeOfConsultationTypesByDoctorId(token(), clinicId, doctorId);
	}

	public ResponseEntity<?> getSizeOfConsultationTypesByDoctorIdFallback(String clinicId, String doctorId,
			Throwable ex) {

		log.error("Consultation count failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "inProgressAppointmentsFallback")
	@Retry(name = "bookingService", fallbackMethod = "inProgressAppointmentsFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "inProgressAppointmentsFallback")
	public ResponseEntity<?> inProgressAppointments(String mobile) {

		return bookingFeignClient.inProgressAppointments(token(), mobile);
	}

	public ResponseEntity<?> inProgressAppointmentsFallback(String mobile, Throwable ex) {

		log.error("In Progress Appointments failed mobile={}", mobile, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
	@Retry(name = "bookingService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page, int size) {

		return bookingFeignClient.getDoctorFutureAppointments(token(), doctorId, page, size);
	}

	public ResponseEntity<?> getDoctorFutureAppointmentsFallback(String doctorId, int page, int size, Throwable ex) {

		log.error("Future appointments failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "bookingByDoctorIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "bookingByDoctorIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "bookingByDoctorIdFallback")
	public ResponseEntity<?> bookingByDoctorId(String doctorId, int page, int size) {

		return bookingFeignClient.bookingByDoctorId(token(), doctorId, page, size);
	}

	public ResponseEntity<?> bookingByDoctorIdFallback(String doctorId, int page, int size, Throwable ex) {

		log.error("BookingByDoctorId failed doctorId={}", doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "bookServiceFallback")
	@Retry(name = "bookingService", fallbackMethod = "bookServiceFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "bookServiceFallback")
	public ResponseEntity<?> bookService(BookingRequset request) {

		return bookingFeignClient.bookService(request);
	}

	public ResponseEntity<?> bookServiceFallback(BookingRequset request, Throwable ex) {

		log.error("Book Service failed", ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getInProgressAppointmentByPatientIdAndBookingIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "getInProgressAppointmentByPatientIdAndBookingIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getInProgressAppointmentByPatientIdAndBookingIdFallback")
	public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingId(String patientId, String bookingId) {

		return bookingFeignClient.getInProgressAppointmentByPatientIdAndBookingId(token(), patientId, bookingId);
	}

	public ResponseEntity<?> getInProgressAppointmentByPatientIdAndBookingIdFallback(String patientId, String bookingId,
			Throwable ex) {

		log.error("InProgressAppointment failed patientId={}, bookingId={}", patientId, bookingId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	@Retry(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "updateAppointmentBasedOnBookingIdFallback")
	public ResponseEntity<?> updateAppointmentBasedOnBookingId(BookingResponse bookingResponse) {

		return bookingFeignClient.updateAppointmentBasedOnBookingId(token(), bookingResponse);
	}

	public ResponseEntity<?> updateAppointmentBasedOnBookingIdFallback(BookingResponse bookingResponse, Throwable ex) {

		log.error("Update Appointment failed", ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback")
	@Retry(name = "bookingService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback")
	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(String clinicId,
			String branchId, String doctorId, String status, int page, int size) {

		return bookingFeignClient.getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatus(token(), clinicId,
				branchId, doctorId, status, page, size);
	}

	public ResponseEntity<?> getBookedServicesByClinicIdWithBranchIdAnddoctorIdAndStatusFallback(String clinicId,
			String branchId, String doctorId, String status, int page, int size, Throwable ex) {

		log.error("BookedServices failed clinicId={}, doctorId={}", clinicId, doctorId, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(buildResponse(getFallbackMessage(ex)));
	}

	@CircuitBreaker(name = "bookingService", fallbackMethod = "searchBookingsFallback")
	@Retry(name = "bookingService", fallbackMethod = "searchBookingsFallback")
	@RateLimiter(name = "bookingService", fallbackMethod = "searchBookingsFallback")
	public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> searchBookings(String clinicId, String input) {

		return bookingFeignClient.searchBookings(clinicId, input);
	}

	public ResponseEntity<ResponseStructure<List<Map<String, Object>>>> searchBookingsFallback(String clinicId,
			String input, Throwable ex) {

		log.error("SearchBookings failed clinicId={}, input={}", clinicId, input, ex);

		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(buildResponseStructure(getFallbackMessage(ex)));
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

	private String getFallbackMessage(Throwable ex) {

		if (ex instanceof RequestNotPermitted) {
			return "Too many requests. Please try again later.";
		}

		if (ex instanceof CallNotPermittedException) {
			return "Booking Service is temporarily unavailable because Circuit Breaker is OPEN.";
		}

		if (ex instanceof FeignException) {
			return "Booking Service is currently unavailable.";
		}

		if (ex instanceof java.net.ConnectException) {
			return "Unable to connect to Booking Service.";
		}

		if (ex instanceof java.net.SocketTimeoutException) {
			return "Booking Service response timed out.";
		}

		return "Booking Service is temporarily unavailable. Please try again later.";
	}
}