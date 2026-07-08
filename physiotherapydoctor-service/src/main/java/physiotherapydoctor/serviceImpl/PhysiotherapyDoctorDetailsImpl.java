package physiotherapydoctor.serviceImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import physiotherapydoctor.dto.ChangeDoctorPasswordDTO;
import physiotherapydoctor.dto.DoctorAvailabilityStatusDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.dto.TherapistResponseDTO;
import physiotherapydoctor.feign.BookingFeignClient;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.service.PhysiotherapyDoctorDetails;
import physiotherapydoctor.util.FeignImpl;
import physiotherapydoctor.util.KeyCloakTokenStore;


@Service
@Slf4j
public class PhysiotherapyDoctorDetailsImpl implements PhysiotherapyDoctorDetails {

	@Autowired
	private FeignImpl clinicAdminServiceClient;

	@Autowired
	private FeignImpl bookingFeignClient;
	
//	 @Autowired
//	 private KeyCloakTokenStore keyCloakTokenStore;


	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getPhysioDoctorDetailsFallback")
	@Secured("ROLE_DOCTOR")
	public Response getPhysioDoctorDetails(String clinicId, String branchId) {

	    log.info("Fetching therapist details. clinicId={}, branchId={}", clinicId, branchId);

	    ResponseEntity<Response> clinicdata =
	            clinicAdminServiceClient.getTherapistWithRequiredFileds(clinicId, branchId);

	    Object obj = clinicdata.getBody().getData();

	    ObjectMapper mapper = new ObjectMapper();

	    List<TherapistResponseDTO> dto =
	            mapper.convertValue(obj,
	                    new TypeReference<List<TherapistResponseDTO>>() {});

	    log.info("Successfully fetched {} therapist records", dto.size());

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

	    log.info("Change password request received for username={}", username);

	    Response validationResponse =
	            validateChangePasswordRequest(username, updateDTO);

	    if (validationResponse != null) {

	        log.warn(
	                "Password change validation failed for username={}",
	                username);

	        return validationResponse;
	    }

	    try {

	        log.debug(
	                "Calling clinic-admin service to change password for username={}",
	                username);

	        Response response =
	                clinicAdminServiceClient.changePassword(
	                        username,
	                        updateDTO);

	        log.info(
	                "Password changed successfully for username={}",
	                username);

	        return response;

	    } catch (Exception ex) {

	        log.error(
	                "Failed to change password for username={}. Error={}",
	                username,
	                ex.getMessage(),
	                ex);

	        return Response.builder()
	                .success(false)
	                .status(500)
	                .message("Failed to change password ")
	                .build();
	    }
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "updateDoctorAvailabilityFallback")
	@Secured("ROLE_DOCTOR")
	public Response updateDoctorAvailability(
	        String doctorId,
	        DoctorAvailabilityStatusDTO availabilityDTO) {

	    log.info(
	            "Update doctor availability request received. doctorId={}",
	            doctorId);

	    if (doctorId == null || doctorId.isBlank()) {

	        log.warn("Doctor ID is null or empty");

	        return Response.builder()
	                .success(false)
	                .status(400)
	                .message("Doctor ID must not be empty")
	                .build();
	    }

	    if (availabilityDTO == null) {

	        log.warn(
	                "Availability DTO is null for doctorId={}",
	                doctorId);

	        return Response.builder()
	                .success(false)
	                .status(400)
	                .message("Availability status is missing")
	                .build();
	    }

	    try {

	        log.debug(
	                "Calling clinic-admin service to update availability for doctorId={}",
	                doctorId);

	        Response response =
	                clinicAdminServiceClient.updateDoctorAvailability(
	                        doctorId,
	                        availabilityDTO);

	        log.info(
	                "Doctor availability updated successfully. doctorId={}",
	                doctorId);

	        return response;

	    } catch (Exception ex) {

	        log.error(
	                "Failed to update doctor availability. doctorId={}, error={}",
	                doctorId,
	                ex.getMessage(),
	                ex);

	        return Response.builder()
	                .success(false)
	                .status(500)
	                .message("Failed to update doctor availability status")
	                .build();
	    }
	}

	/// NEW DOCTOR APIS
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getAllDoctorsFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getAllDoctors() {

	    log.info("Fetching all doctors");

	    try {

	        ResponseEntity<?> response =
	                clinicAdminServiceClient.getAllDoctors();

	        log.info("Successfully fetched all doctors");

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching all doctors. Error={}",
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(500)
	                .body(e.getMessage());
	    }
	}


	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorById(String id) {

	    log.info("Fetching doctor details. doctorId={}", id);

	    try {

	        log.debug(
	                "Calling clinic-admin service to fetch doctor details. doctorId={}",
	                id);

	        ResponseEntity<?> response =
	                clinicAdminServiceClient.getDoctorById(id);

	        log.info(
	                "Successfully fetched doctor details. doctorId={}",
	                id);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching doctor details. doctorId={}, error={}",
	                id,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(500)
	                .body(e.getMessage());
	    }
	}


	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorByClinicAndDoctorIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorByClinicAndDoctorId(
	        String clinicId,
	        String doctorId) {

	    log.info(
	            "Fetching doctor by clinic and doctorId. clinicId={}, doctorId={}",
	            clinicId,
	            doctorId);

	    try {

	        log.debug(
	                "Calling clinic-admin service. clinicId={}, doctorId={}",
	                clinicId,
	                doctorId);

	        ResponseEntity<?> response =
	                clinicAdminServiceClient.getDoctorByClinicAndDoctorId(
	                        clinicId,
	                        doctorId);

	        log.info(
	                "Successfully fetched doctor. clinicId={}, doctorId={}",
	                clinicId,
	                doctorId);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching doctor. clinicId={}, doctorId={}, error={}",
	                clinicId,
	                doctorId,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(500)
	                .body(e.getMessage());
	    }
	}


	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorsByHospitalByIdFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorsByHospitalById(String clinicId) {

	    log.info(
	            "Fetching doctors by clinicId={}",
	            clinicId);

	    try {

	        log.debug(
	                "Calling clinic-admin service to fetch doctors. clinicId={}",
	                clinicId);

	        ResponseEntity<?> response =
	                clinicAdminServiceClient.getDoctorsByHospitalById(clinicId);

	        log.info(
	                "Successfully fetched doctors for clinicId={}",
	                clinicId);

	        return response;

	    } catch (Exception e) {

	        log.error(
	                "Error while fetching doctors by clinicId={}. Error={}",
	                clinicId,
	                e.getMessage(),
	                e);

	        return ResponseEntity.status(500)
	                .body(e.getMessage());
	    }
	}
	
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDoctorFutureAppointmentsFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<?> getDoctorFutureAppointments(String doctorId, int page) {

	    log.info(
	            "Fetching doctor future appointments. doctorId={}, page={}",
	            doctorId,
	            page);

	    try {

	        log.debug(
	                "Calling booking service for future appointments. doctorId={}, page={}, size={}",
	                doctorId,
	                page,
	                10);

	        ResponseEntity<?> response =
	                bookingFeignClient.getDoctorFutureAppointments(
	                        doctorId,
	                        page,
	                        10);

	        log.info(
	                "Successfully fetched future appointments. doctorId={}, page={}",
	                doctorId,
	                page);

	        return response;

	    } catch (Exception ex) {

	        log.error(
	                "Error while fetching future appointments. doctorId={}, page={}, error={}",
	                doctorId,
	                page,
	                ex.getMessage(),
	                ex);

	        if (ex instanceof feign.FeignException feignEx) {

	            log.error(
	                    "FeignException received. status={}, response={}",
	                    feignEx.status(),
	                    feignEx.contentUTF8());

	            return ResponseEntity.status(feignEx.status())
	                    .body(feignEx.contentUTF8());
	        }

	        return ResponseEntity.status(500)
	                .body(ex.getMessage());
	    }
	}

	@Override
	@RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getDiseasesFromClinicAdminFallback")
	@Secured("ROLE_DOCTOR")
	public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId) {

	    log.info(
	            "Fetching diseases from clinic admin. hospitalId={}",
	            hospitalId);

	    try {

	        log.debug(
	                "Calling clinic-admin service to fetch diseases. hospitalId={}",
	                hospitalId);

	        ResponseEntity<Response> response =
	                clinicAdminServiceClient.getDiseasesByHospitalId(hospitalId);

	        log.info(
	                "Successfully fetched diseases. hospitalId={}",
	                hospitalId);

	        return response;

	    } catch (Exception ex) {

	        log.error(
	                "Error while fetching diseases. hospitalId={}, error={}",
	                hospitalId,
	                ex.getMessage(),
	                ex);

	        throw ex;
	    }
	}

//	@Override
//	 @RateLimiter(name = "physiotherapydoctorService", fallbackMethod = "getLabTestsFromClinicAdminFallback")
//@Secured("ROLE_DOCTOR")
//	public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId) {
//		return clinicAdminServiceClient.getLabTestsByHospitalId(hospitalId);
//	}



    private Response buildRateLimitResponse(Exception ex) {
        return Response.builder()
                .success(false)
                .status(429)
                .message("Rate limit exceeded. Please try again later.")
                .build();
    }

    public Response getPhysioDoctorDetailsFallback(String clinicId, String branchId, Exception ex){ return buildRateLimitResponse(ex); }
    public Response changePasswordFallback(String username, ChangeDoctorPasswordDTO updateDTO, Exception ex){ return buildRateLimitResponse(ex); }
    public Response updateDoctorAvailabilityFallback(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO, Exception ex){ return buildRateLimitResponse(ex); }

    public ResponseEntity<?> getAllDoctorsFallback(Exception ex){ return ResponseEntity.status(429).body(buildRateLimitResponse(ex)); }
    public ResponseEntity<?> getDoctorByIdFallback(String id, Exception ex){ return ResponseEntity.status(429).body(buildRateLimitResponse(ex)); }
    public ResponseEntity<?> getDoctorByClinicAndDoctorIdFallback(String clinicId, String doctorId, Exception ex){ return ResponseEntity.status(429).body(buildRateLimitResponse(ex)); }
    public ResponseEntity<?> getDoctorsByHospitalByIdFallback(String clinicId, Exception ex){ return ResponseEntity.status(429).body(buildRateLimitResponse(ex)); }
    public ResponseEntity<?> getDoctorFutureAppointmentsFallback(String doctorId, int page, Exception ex){ return ResponseEntity.status(429).body(buildRateLimitResponse(ex)); }

    public ResponseEntity<Response> getDiseasesFromClinicAdminFallback(String hospitalId, Exception ex){
        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
    }

    public ResponseEntity<Response> getLabTestsFromClinicAdminFallback(String hospitalId, Exception ex){
        return ResponseEntity.status(429).body(buildRateLimitResponse(ex));
    }

}