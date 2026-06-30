package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.VitalsDTO;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.DoctorVitalsService;
import physiotherapydoctor.util.KeyCloakTokenStore;


@Service
public class DoctorVitalsServiceImpl implements DoctorVitalsService {

	@Autowired
	private ClinicAdminFeign clinicAdminServiceClient;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	    
	/**
	 * Add Vitals for a booking
	 */
	@Override
    @RateLimiter(name = "doctorVitalsService", fallbackMethod = "addVitalsFallback")
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto) {
		// Directly forward Clinic Admin response
		return clinicAdminServiceClient.addVitals(keyCloakTokenStore.getAccess_token(),bookingId, dto);

	}

	/**
	 * Get Vitals by bookingId and patientId
	 */
	@Override
    @RateLimiter(name = "doctorVitalsService", fallbackMethod = "getVitalsFallback")
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getVitals(String bookingId, String patientId) {
		return clinicAdminServiceClient.getVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId);
	}

	/**
	 * Delete Vitals by bookingId and patientId
	 */
	@Override
    @RateLimiter(name = "doctorVitalsService", fallbackMethod = "deleteVitalsFallback")
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> deleteVitals(String bookingId, String patientId) {
		return clinicAdminServiceClient.delVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId);
	}

	/**
	 * Update Vitals by bookingId and patientId
	 */
	@Override
    @RateLimiter(name = "doctorVitalsService", fallbackMethod = "updateVitalsFallback")
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> updateVitals(String bookingId, String patientId, VitalsDTO dto) {
		return clinicAdminServiceClient.updateVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId, dto);
	}

    
    private ResponseEntity<Response> buildRateLimitResponse(Exception ex) {
        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Rate limit exceeded. Please try again later.");
        return ResponseEntity.status(429).body(response);
    }

    public ResponseEntity<Response> addVitalsFallback(String bookingId, VitalsDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<Response> getVitalsFallback(String bookingId, String patientId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<Response> deleteVitalsFallback(String bookingId, String patientId, Exception ex) {
        return buildRateLimitResponse(ex);
    }

    public ResponseEntity<Response> updateVitalsFallback(String bookingId, String patientId, VitalsDTO dto, Exception ex) {
        return buildRateLimitResponse(ex);
    }

}