package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import physiotherapydoctor.dto.RecoverySupportDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.RecoverySupportService;
import physiotherapydoctor.util.FeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
@Slf4j
public class RecoverySupportServiceImpl implements RecoverySupportService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeignImpl clinicAdminFeign;

    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;

    @Override
    @RateLimiter(
        name = "physiotherapydoctorService",
        fallbackMethod = "getRecoverySupportsFallback"
    )
    @Secured("ROLE_DOCTOR")
    public Response getRecoverySupports(String clinicId) {

        log.info("Fetching recovery supports for clinicId={}", clinicId);

        try {

            Response response =
                    clinicAdminFeign.getAllRecoverySupportsByClinicId(
                            clinicId);

            if (response != null && response.getData() != null) {

                List<RecoverySupportDTO> recoverySupports =
                        objectMapper.convertValue(
                                response.getData(),
                                new TypeReference<List<RecoverySupportDTO>>() {});

                log.info("Successfully fetched {} recovery supports for clinicId={}",
                        recoverySupports.size(), clinicId);

                response.setData(recoverySupports);
            } else {
                log.warn("No recovery supports returned for clinicId={}", clinicId);
            }

            return response;

        } catch (Exception e) {

            log.error("Failed to fetch recovery supports for clinicId={}",
                    clinicId, e);

            throw new RuntimeException("Unable to fetch recovery supports", e);
        }
    }

    public Response getRecoverySupportsFallback(
            String clinicId,
            Exception ex) {

        log.error("Rate limiter fallback triggered for getRecoverySupports. clinicId={}",
                clinicId, ex);

        return buildRateLimitResponse(ex);
    }

    private Response buildRateLimitResponse(Exception ex) {

        log.warn("Building rate limit response. reason={}",
                ex != null ? ex.getMessage() : "unknown");

        Response response = new Response();
        response.setSuccess(false);
        response.setStatus(429);
        response.setMessage("Rate limit exceeded. Please try again later.");
        response.setData(null);

        return response;
    }
}
