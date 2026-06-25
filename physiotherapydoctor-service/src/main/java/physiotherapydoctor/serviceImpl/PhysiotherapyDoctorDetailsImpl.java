package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TherapistResponseDTO;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
public class PhysiotherapyDoctorDetailsImpl implements PhysiotherapyDoctorDetails {

	@Autowired
	private ClinicAdminFeign clinicAdminServiceClient;

	@Autowired
	private BookingFeignClient bookingFeignClient;
	
	 @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	    

	private ObjectMapper objectMapper;

	@Override
	 @Secured("ROLE_DOCTOR")
	public Response getPhysioDoctorDetails(String clinicId, String branchId) {
		ResponseEntity<Response> clinicdata = clinicAdminServiceClient.getTherapistWithRequiredFileds(keyCloakTokenStore.getAccess_token(),clinicId,
				branchId);
		Object obj = clinicdata.getBody().getData();
		ObjectMapper mapper = new ObjectMapper();
		List<TherapistResponseDTO> dto = mapper.convertValue(obj, new TypeReference<List<TherapistResponseDTO>>() {
		});

		Response response = new Response();
		response.setSuccess(true);
		response.setData(dto);
		response.setMessage("Successfully fetched therapist details");
		response.setStatus(HttpStatus.OK.value());

		return response;

	}

//	===================from doctor serviceiml ccms================================

	private Response validateChangePasswordRequest(String username, ChangeDoctorPasswordDTO updateDTO) {
		if (username == null || username.isBlank()) {
			return Response.builder().success(false).status(400).message("Username must not be empty").build();
		}

		if (updateDTO == null) {
			return Response.builder().success(false).status(400).message("Request body is missing").build();
		}

		if (updateDTO.getCurrentPassword() == null || updateDTO.getCurrentPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("Current password must not be empty").build();
		}

		if (updateDTO.getNewPassword() == null || updateDTO.getNewPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("New password must not be empty").build();
		}

		if (updateDTO.getConfirmPassword() == null || updateDTO.getConfirmPassword().isBlank()) {
			return Response.builder().success(false).status(400).message("Confirm password must not be empty").build();
		}

		if (!updateDTO.getNewPassword().equals(updateDTO.getConfirmPassword())) {
			return Response.builder().success(false).status(400)
					.message("New password and confirm password do not match").build();
		}

		if (updateDTO.getNewPassword().length() < 6) {
			return Response.builder().success(false).status(400).message("Password must be at least 6 characters")
					.build();
		}

		return null;
	}

	@Override
	 @Secured("ROLE_DOCTOR")
	public Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO) {
		Response validationResponse = validateChangePasswordRequest(username, updateDTO);
		if (validationResponse != null) {
			return validationResponse;
		}

		try {

			return clinicAdminServiceClient.changePassword(keyCloakTokenStore.getAccess_token(),username, updateDTO);

		} catch (Exception ex) {

			return Response.builder().success(false).status(500).message("Failed to change password ").build();
		}
	}

	@Override
	 @Secured("ROLE_DOCTOR")
	public Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO) {
		if (doctorId == null || doctorId.isBlank()) {
			return Response.builder().success(false).status(400).message("Doctor ID must not be empty").build();
		}

		if (availabilityDTO == null) {
			return Response.builder().success(false).status(400).message("Availability status is missing").build();
		}
		try {
			return clinicAdminServiceClient.updateDoctorAvailability(keyCloakTokenStore.getAccess_token(),doctorId, availabilityDTO);
		} catch (Exception ex) {
			return Response.builder().success(false).status(500).message("Failed to update doctor availability status")
					.build();

		}
	}

	/// NEW DOCTOR APIS
	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getAllDoctors() {
		try {
			return clinicAdminServiceClient.getAllDoctors(keyCloakTokenStore.getAccess_token());
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorById(String id) {
		try {
			return clinicAdminServiceClient.getDoctorById(keyCloakTokenStore.getAccess_token(),id);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId, String doctorId) {
		try {
			return clinicAdminServiceClient.getDoctorByClinicAndDoctorId(keyCloakTokenStore.getAccess_token(),clinicId, doctorId);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorsByHospitalById(String clinicId) {
		try {
			return clinicAdminServiceClient.getDoctorsByHospitalById(keyCloakTokenStore.getAccess_token(),clinicId);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

//	public ResponseEntity<?> getDoctorsBySubServiceId(String hsptlId, String subServiceId) {
//		try {
//			return clinicAdminServiceClient.getDoctorsBySubServiceId(keyCloakTokenStore.getAccess_token(),hsptlId, subServiceId);
//		} catch (Exception e) {
//			return ResponseEntity.status(500).body(e.getMessage());
//		}
//	}
//
//	public ResponseEntity<?> getAllDoctorsBySubServiceId(String subServiceId) {
//		try {
//			return clinicAdminServiceClient.getAllDoctorsBySubServiceId(keyCloakTokenStore.getAccess_token(),subServiceId);
//		} catch (Exception e) {
//			return ResponseEntity.status(500).body(e.getMessage());
//		}
//	}

	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page) {
		try {

			return bookingFeignClient.getDoctorFutureAppointments(keyCloakTokenStore.getAccess_token(),doctorId,page,10);

		} catch (Exception ex) {

			if (ex instanceof feign.FeignException feignEx) {
				return ResponseEntity.status(feignEx.status()).body(feignEx.contentUTF8());
			}
			return ResponseEntity.status(500).body(ex.getMessage());
		}
	}

	@Override
	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId) {

		return clinicAdminServiceClient.getDiseasesByHospitalId(keyCloakTokenStore.getAccess_token(),hospitalId);
	}

	@Override
	 @Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId) {
		return clinicAdminServiceClient.getLabTestsByHospitalId(hospitalId);
	}

}
