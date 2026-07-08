package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.VitalsDTO;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.DoctorVitalsService;
import physiotherapydoctor.util.FeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;


@Service
@Slf4j
public class DoctorVitalsServiceImpl implements DoctorVitalsService {

	@Autowired
	private FeignImpl clinicAdminServiceClient;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	    
	/**
	 * Add Vitals for a booking
	 */
	 @Override
	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "addVitalsFallback")
	 @Secured("ROLE_DOCTOR")
	 public ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto) {

	     long startTime = System.currentTimeMillis();

	     log.info("Entered addVitals() with bookingId : {}", bookingId);

	     try {

	         log.info("Calling Clinic Admin Service to add vitals for bookingId : {}",
	                 bookingId);

	         ResponseEntity<Response> response =
	                 clinicAdminServiceClient.addVitals(bookingId, dto);

	         long executionTime = System.currentTimeMillis() - startTime;

	         log.info("Vitals added successfully for bookingId : {} in {} ms",
	                 bookingId,
	                 executionTime);

	         return response;

	     } catch (Exception e) {

	         log.error("Exception occurred while adding vitals for bookingId : {}. Error : {}",
	                 bookingId,
	                 e.getMessage(),
	                 e);

	         throw e;
	     }
	 }
	 
	 @Override
	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getVitalsFallback")
	 @Secured("ROLE_DOCTOR")
	 public ResponseEntity<Response> getVitals(String bookingId, String patientId) {

	     long startTime = System.currentTimeMillis();

	     log.info("Entered getVitals() with bookingId : {}, patientId : {}",
	             bookingId,
	             patientId);

	     try {

	         log.info("Calling Clinic Admin Service to fetch vitals. bookingId : {}, patientId : {}",
	                 bookingId,
	                 patientId);

	         ResponseEntity<Response> response =
	                 clinicAdminServiceClient.getVitals(bookingId, patientId);

	         long executionTime = System.currentTimeMillis() - startTime;

	         log.info("Vitals fetched successfully for bookingId : {}, patientId : {} in {} ms",
	                 bookingId,
	                 patientId,
	                 executionTime);

	         return response;

	     } catch (Exception e) {

	         log.error("Exception occurred while fetching vitals. bookingId : {}, patientId : {}. Error : {}",
	                 bookingId,
	                 patientId,
	                 e.getMessage(),
	                 e);

	         throw e;
	     }
	 }
	 
	 @Override
	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "deleteVitalsFallback")
	 @Secured("ROLE_DOCTOR")
	 public ResponseEntity<Response> deleteVitals(String bookingId, String patientId) {

	     long startTime = System.currentTimeMillis();

	     log.info("Entered deleteVitals() with bookingId : {}, patientId : {}",
	             bookingId,
	             patientId);

	     try {

	         log.info("Calling Clinic Admin Service to delete vitals. bookingId : {}, patientId : {}",
	                 bookingId,
	                 patientId);

	         ResponseEntity<Response> response =
	                 clinicAdminServiceClient.delVitals(bookingId, patientId);

	         long executionTime = System.currentTimeMillis() - startTime;

	         log.info("Vitals deleted successfully for bookingId : {}, patientId : {} in {} ms",
	                 bookingId,
	                 patientId,
	                 executionTime);

	         return response;

	     } catch (Exception e) {

	         log.error("Exception occurred while deleting vitals. bookingId : {}, patientId : {}. Error : {}",
	                 bookingId,
	                 patientId,
	                 e.getMessage(),
	                 e);

	         throw e;
	     }
	 }
	 
	 @Override
	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateVitalsFallback")
	 @Secured("ROLE_DOCTOR")
	 public ResponseEntity<Response> updateVitals(String bookingId,
	                                              String patientId,
	                                              VitalsDTO dto) {

	     long startTime = System.currentTimeMillis();

	     log.info("Entered updateVitals() with bookingId : {}, patientId : {}",
	             bookingId,
	             patientId);

	     try {

	         log.info("Calling Clinic Admin Service to update vitals. bookingId : {}, patientId : {}",
	                 bookingId,
	                 patientId);

	         ResponseEntity<Response> response =
	                 clinicAdminServiceClient.updateVitals(
	                         bookingId,
	                         patientId,
	                         dto);

	         long executionTime = System.currentTimeMillis() - startTime;

	         log.info("Vitals updated successfully for bookingId : {}, patientId : {} in {} ms",
	                 bookingId,
	                 patientId,
	                 executionTime);

	         return response;

	     } catch (Exception e) {

	         log.error("Exception occurred while updating vitals. bookingId : {}, patientId : {}. Error : {}",
	                 bookingId,
	                 patientId,
	                 e.getMessage(),
	                 e);

	         throw e;
	     }
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