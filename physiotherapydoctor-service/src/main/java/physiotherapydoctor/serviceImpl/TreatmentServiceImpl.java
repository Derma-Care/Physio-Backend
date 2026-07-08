package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.service.TreatmentService;
import physiotherapydoctor.util.ClinicAdminFeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
public class TreatmentServiceImpl implements TreatmentService {

	@Autowired
	private ClinicAdminFeignImpl clinicAdminServiceClient;

	@Autowired
	private KeyCloakTokenStore keyCloakTokenStore;

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "addTreatmentFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {
		try {
			return clinicAdminServiceClient.addTreatment(dto);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllTreatmentsFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getAllTreatments() {
		try {
			return clinicAdminServiceClient.getAllTreatments();
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTreatmentByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {
		try {
			return clinicAdminServiceClient.getTreatmentById(id, hospitalId);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteTreatmentByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {
		try {
			return clinicAdminServiceClient.deleteTreatmentById(id, hospitalId);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateTreatmentByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {
		try {
			return clinicAdminServiceClient.updateTreatmentById(id, hospitalId, dto);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	private Response buildRateLimitResponse(Exception ex) {
		return new Response(false, null, "Rate limit exceeded. Please try again later.", 429);
	}

	public ResponseEntity<Response> addTreatmentFallback(TreatmentDTO dto, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> getAllTreatmentsFallback(Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> getTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> deleteTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> updateTreatmentByIdFallback(String id, String hospitalId, TreatmentDTO dto,
			Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

}