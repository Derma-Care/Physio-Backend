package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import physiotherapydoctor.dto.RecoverySupportDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.RecoverySupportService;
import physiotherapydoctor.util.ClinicAdminFeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;


@Service
public class RecoverySupportServiceImpl implements RecoverySupportService {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClinicAdminFeignImpl clinicAdminFeign;
	
	
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;

	 @Override
	 @RateLimiter(
	     name = "physiotherapydoctorService",
	     fallbackMethod = "getRecoverySupportsFallback"
	 )
	 @Secured("ROLE_DOCTOR")
	 public Response getRecoverySupports(String clinicId) {
	     try {
	         Response response =
	                 clinicAdminFeign.getAllRecoverySupportsByClinicId(	                     
	                         clinicId);

	         if (response != null && response.getData() != null) {
	             List<RecoverySupportDTO> recoverySupports =
	                     objectMapper.convertValue(
	                             response.getData(),
	                             new TypeReference<List<RecoverySupportDTO>>() {});
	             response.setData(recoverySupports);
	         }

	         return response;

	     } catch (Exception e) {
	         throw new RuntimeException("Unable to fetch recovery supports", e);
	     }
	 }
	 
	 public Response getRecoverySupportsFallback(
		        String clinicId,
		        Exception ex) {

		    return buildRateLimitResponse(ex);
		}
	 
	 private Response buildRateLimitResponse(Exception ex) {
		    Response response = new Response();
		    response.setSuccess(false);
		    response.setStatus(429);
		    response.setMessage("Rate limit exceeded. Please try again later.");
		    response.setData(null);
		    return response;
		}
}