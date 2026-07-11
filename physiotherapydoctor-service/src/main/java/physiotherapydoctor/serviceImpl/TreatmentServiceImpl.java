package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import feign.FeignException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.service.TreatmentService;
import physiotherapydoctor.util.ClinicAdminFeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
@Slf4j
public class TreatmentServiceImpl implements TreatmentService {

	@Autowired
	private ClinicAdminFeignImpl clinicAdminServiceClient;

	
	   @Override
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "addTreatmentFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {

	        log.info("Adding treatment. dto={}", dto);

	        try {
	            return clinicAdminServiceClient.addTreatment(dto);
	        } catch (FeignException ex) {
	            log.error("Error while adding treatment", ex);
	            return ResponseEntity.status(ex.status())
	                    .body(new Response(false, null, ex.getMessage(), ex.status()));
	        }
	    }

	    @Override
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllTreatmentsFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> getAllTreatments() {

	        log.info("Fetching all treatments");

	        try {
	            return clinicAdminServiceClient.getAllTreatments();
	        } catch (FeignException ex) {
	            log.error("Error while fetching all treatments", ex);
	            return ResponseEntity.status(ex.status())
	                    .body(new Response(false, null, ex.getMessage(), ex.status()));
	        }
	    }

	    @Override
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getTreatmentByIdFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {

	        log.info("Fetching treatment by id={}, hospitalId={}", id, hospitalId);

	        try {
	            return clinicAdminServiceClient.getTreatmentById(id, hospitalId);
	        } catch (FeignException ex) {
	            log.error("Error while fetching treatment. id={}, hospitalId={}", id, hospitalId, ex);
	            return ResponseEntity.status(ex.status())
	                    .body(new Response(false, null, ex.getMessage(), ex.status()));
	        }
	    }

	    @Override
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteTreatmentByIdFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {

	        log.info("Deleting treatment. id={}, hospitalId={}", id, hospitalId);

	        try {
	            return clinicAdminServiceClient.deleteTreatmentById(id, hospitalId);
	        } catch (FeignException ex) {
	            log.error("Error while deleting treatment. id={}, hospitalId={}", id, hospitalId, ex);
	            return ResponseEntity.status(ex.status())
	                    .body(new Response(false, null, ex.getMessage(), ex.status()));
	        }
	    }

	    @Override
	    @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateTreatmentByIdFallback")
	    @Secured("ROLE_DOCTOR")
	    public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {

	        log.info("Updating treatment. id={}, hospitalId={}, dto={}", id, hospitalId, dto);

	        try {
	            return clinicAdminServiceClient.updateTreatmentById(id, hospitalId, dto);
	        } catch (FeignException ex) {
	            log.error("Error while updating treatment. id={}, hospitalId={}", id, hospitalId, ex);
	            return ResponseEntity.status(ex.status())
	                    .body(new Response(false, null, ex.getMessage(), ex.status()));
	        }
	    }

	    private Response buildRateLimitResponse(Exception ex) {

	        log.warn("Rate limiter triggered. reason={}",
	                ex != null ? ex.getMessage() : "unknown");

	        return new Response(false, null,
	                "Rate limit exceeded. Please try again later.",
	                429);
	    }

	    public ResponseEntity<Response> addTreatmentFallback(TreatmentDTO dto, Exception ex) {
	        log.error("addTreatmentFallback triggered. dto={}", dto, ex);
	        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	    }

	    public ResponseEntity<Response> getAllTreatmentsFallback(Exception ex) {
	        log.error("getAllTreatmentsFallback triggered", ex);
	        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	    }

	    public ResponseEntity<Response> getTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
	        log.error("getTreatmentByIdFallback triggered. id={}, hospitalId={}", id, hospitalId, ex);
	        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	    }

	    public ResponseEntity<Response> deleteTreatmentByIdFallback(String id, String hospitalId, Exception ex) {
	        log.error("deleteTreatmentByIdFallback triggered. id={}, hospitalId={}", id, hospitalId, ex);
	        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	    }

	    public ResponseEntity<Response> updateTreatmentByIdFallback(String id, String hospitalId,
	            TreatmentDTO dto, Exception ex) {
	        log.error("updateTreatmentByIdFallback triggered. id={}, hospitalId={}, dto={}",
	                id, hospitalId, dto, ex);
	        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	    }
}