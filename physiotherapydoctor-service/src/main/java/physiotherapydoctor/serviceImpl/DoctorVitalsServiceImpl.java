package physiotherapydoctor.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
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
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> addVitals(String bookingId, VitalsDTO dto) {
		// Directly forward Clinic Admin response
		return clinicAdminServiceClient.addVitals(keyCloakTokenStore.getAccess_token(),bookingId, dto);
	}

	/**
	 * Get Vitals by bookingId and patientId
	 */
	@Override
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getVitals(String bookingId, String patientId) {
		return clinicAdminServiceClient.getVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId);
	}

	/**
	 * Delete Vitals by bookingId and patientId
	 */
	@Override
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> deleteVitals(String bookingId, String patientId) {
		return clinicAdminServiceClient.delVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId);
	}

	/**
	 * Update Vitals by bookingId and patientId
	 */
	@Override
    @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> updateVitals(String bookingId, String patientId, VitalsDTO dto) {
		return clinicAdminServiceClient.updateVitals(keyCloakTokenStore.getAccess_token(),bookingId, patientId, dto);
	}
}
