package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import physiotherapydoctor.dto.RecoverySupportDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.RecoverySupportService;
import physiotherapydoctor.util.KeyCloakTokenStore;


@Service
public class RecoverySupportServiceImpl implements RecoverySupportService {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ClinicAdminFeign clinicAdminFeign;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;


	@Override
	@Secured("ROLE_DOCTOR")
	public Response getRecoverySupports(String clinicId) {
	    try {
	        Response response =
	                clinicAdminFeign.getAllRecoverySupportsByClinicId(keyCloakTokenStore.getAccess_token(),clinicId);

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
}