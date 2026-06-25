package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import feign.FeignException;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TreatmentDTO;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.TreatmentService;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
public class TreatmentServiceImpl implements TreatmentService {

	@Autowired
	private ClinicAdminFeign clinicAdminServiceClient;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;


	@Override
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> addTreatment(TreatmentDTO dto) {
		try {
			return clinicAdminServiceClient.addTreatment(dto);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getAllTreatments() {
		try {
			return clinicAdminServiceClient.getAllTreatments(keyCloakTokenStore.getAccess_token());
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getTreatmentById(String id, String hospitalId) {
		try {
			return clinicAdminServiceClient.getTreatmentById(keyCloakTokenStore.getAccess_token(),id, hospitalId);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> deleteTreatmentById(String id, String hospitalId) {
		try {
			return clinicAdminServiceClient.deleteTreatmentById(keyCloakTokenStore.getAccess_token(),id, hospitalId);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}

	@Override
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> updateTreatmentById(String id, String hospitalId, TreatmentDTO dto) {
		try {
			return clinicAdminServiceClient.updateTreatmentById(keyCloakTokenStore.getAccess_token(),id, hospitalId, dto);
		} catch (FeignException ex) {
			return ResponseEntity.status(ex.status()).body(new Response(false, null, ex.getMessage(), ex.status()));
		}
	}
}
