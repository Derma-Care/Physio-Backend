package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TherapistResponseDTO;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;
import physiotherapydoctor.util.AdminFeignImpl;
import physiotherapydoctor.util.BookingFeignImpl;
import physiotherapydoctor.util.ClinicAdminFeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;

@Service
public class PhysiotherapyDoctorDetailsImpl implements PhysiotherapyDoctorDetails {

	@Autowired
	private ClinicAdminFeignImpl clinicAdminServiceClient;

	@Autowired
	private BookingFeignImpl bookingFeignClient;

	@Autowired
	private AdminFeignImpl adminFeignClient;
	@Autowired
	private KeyCloakTokenStore keyCloakTokenStore;

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getPhysioDoctorDetailsFallback")
	@Secured("ROLE_DOCTOR")
	public Response getPhysioDoctorDetails(String clinicId, String branchId) {
		ResponseEntity<Response> clinicdata = clinicAdminServiceClient.getTherapistWithRequiredFileds(clinicId,

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
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "changePasswordFallback")
	@Secured("ROLE_DOCTOR")
	public Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO) {
		Response validationResponse = validateChangePasswordRequest(username, updateDTO);
		if (validationResponse != null) {
			return validationResponse;
		}

		try {
			return clinicAdminServiceClient.changePassword(username, updateDTO);

		} catch (Exception ex) {

			return Response.builder().success(false).status(500).message("Failed to change password ").build();
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorAvailabilityFallback")
	@Secured("ROLE_DOCTOR")
	public Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO) {
		if (doctorId == null || doctorId.isBlank()) {
			return Response.builder().success(false).status(400).message("Doctor ID must not be empty").build();
		}

		if (availabilityDTO == null) {
			return Response.builder().success(false).status(400).message("Availability status is missing").build();
		}
		try {
			return clinicAdminServiceClient.updateDoctorAvailability(doctorId, availabilityDTO);
		} catch (Exception ex) {
			return Response.builder().success(false).status(500).message("Failed to update doctor availability status")
					.build();

		}
	}

	/// NEW DOCTOR APIS
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllDoctorsFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getAllDoctors() {
		try {
			return clinicAdminServiceClient.getAllDoctors();
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorById(String id) {
		try {
			return clinicAdminServiceClient.getDoctorById(id);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId, String doctorId) {
		try {
			return clinicAdminServiceClient.getDoctorByClinicAndDoctorId(clinicId, doctorId);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorsByHospitalById(String clinicId) {
		try {
			return clinicAdminServiceClient.getDoctorsByHospitalById(clinicId);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}

	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page) {
		try {

			return bookingFeignClient.getDoctorFutureAppointments(doctorId, page, 10);
		} catch (Exception ex) {

			if (ex instanceof feign.FeignException feignEx) {
				return ResponseEntity.status(feignEx.status()).body(feignEx.contentUTF8());
			}
			return ResponseEntity.status(500).body(ex.getMessage());
		}
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDiseasesFromClinicAdminFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId) {

		return clinicAdminServiceClient.getDiseasesByHospitalId(hospitalId);
	}

//	@Override
//	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getLabTestsFromClinicAdminFallback")
//@Secured("ROLE_DOCTOR")
//	public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId) {
//		return clinicAdminServiceClient.getLabTestsByHospitalId(hospitalId);
//	}

	private Response buildRateLimitResponse(Exception ex) {
		return Response.builder().success(false).status(429).message("Rate limit exceeded. Please try again later.")
				.build();
	}

	public Response getPhysioDoctorDetailsFallback(String clinicId, String branchId, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response changePasswordFallback(String username, ChangeDoctorPasswordDTO updateDTO, Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public Response updateDoctorAvailabilityFallback(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO,
			Exception ex) {
		return buildRateLimitResponse(ex);
	}

	public ResponseEntity<?> getAllDoctorsFallback(Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<?> getDoctorByIdFallback(String id, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<?> getDoctorByClinicAndDoctorIdFallback(String clinicId, String doctorId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<?> getDoctorsByHospitalByIdFallback(String clinicId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<?> getDoctorFutureAppointmentsFallback(String doctorId, int page, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> getDiseasesFromClinicAdminFallback(String hospitalId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

	public ResponseEntity<Response> getLabTestsFromClinicAdminFallback(String hospitalId, Exception ex) {
		return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
	}

}