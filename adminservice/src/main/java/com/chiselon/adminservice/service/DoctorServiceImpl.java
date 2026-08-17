package com.chiselon.adminservice.service;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.chiselon.adminservice.dto.DoctorsDTO;
import com.chiselon.adminservice.util.ClinicAdminFeignImpl;
import com.chiselon.adminservice.util.KeyCloakTokenStore;
import com.chiselon.adminservice.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j

@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
	
	  @Autowired
	 private KeyCloakTokenStore keyCloakTokenStore;
	    

    private final ClinicAdminFeignImpl clinicAdminFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<Response> addDoctor(DoctorsDTO dto) {
        try {
            return clinicAdminFeign.addDoctor(keyCloakTokenStore.getAccess_token(),dto);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to add doctor");
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> getAllDoctors() {

        log.info("Received request to fetch all doctors.");

        try {

            log.debug("Calling Clinic Admin Feign client to retrieve doctors list.");

            ResponseEntity<Response> response = clinicAdminFeign.getAllDoctors(
                    keyCloakTokenStore.getAccess_token());

            log.info("Successfully fetched doctors list. Response Status: {}",
                    response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to fetch doctors", ex);

            return handleFeignException(ex, "Failed to fetch doctors list");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> getDoctorById(String doctorId) {

        log.info("Received request to fetch doctor details. DoctorId: {}", doctorId);

        try {

            log.debug("Calling Clinic Admin Feign client to fetch doctor details for DoctorId: {}", doctorId);

            ResponseEntity<Response> response = clinicAdminFeign.getDoctorById(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId);

            log.info("Successfully fetched doctor details. DoctorId: {}, Response Status: {}",
                    doctorId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to fetch doctor details for DoctorId: {}. Error: {}",
                    doctorId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch doctor details");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> updateDoctorById(String doctorId, DoctorsDTO dto) {

        log.info("Received request to update doctor. DoctorId: {}", doctorId);

        try {

            log.debug("Calling Clinic Admin Feign client to update doctor details. DoctorId: {}", doctorId);

            ResponseEntity<Response> response = clinicAdminFeign.updateDoctorById(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId,
                    dto);

            log.info("Doctor updated successfully. DoctorId: {}, Response Status: {}",
                    doctorId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to update doctor. DoctorId: {}, Error: {}",
                    doctorId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to update doctor");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> deleteDoctorById(String doctorId) {

        log.info("Received request to delete doctor. DoctorId: {}", doctorId);

        try {

            log.debug("Calling Clinic Admin Feign client to delete doctor. DoctorId: {}", doctorId);

            ResponseEntity<Response> response = clinicAdminFeign.deleteDoctorById(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId);

            log.info("Doctor deleted successfully. DoctorId: {}, Response Status: {}",
                    doctorId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to delete doctor. DoctorId: {}, Error: {}",
                    doctorId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to delete doctor");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> deleteDoctorsByClinic(String clinicId) {

        log.info("Received request to delete all doctors for ClinicId: {}", clinicId);

        try {

            log.debug("Calling Clinic Admin Feign client to delete doctors for ClinicId: {}", clinicId);

            ResponseEntity<Response> response = clinicAdminFeign.deleteDoctorsByClinic(
                    keyCloakTokenStore.getAccess_token(),
                    clinicId);

            log.info("Successfully deleted doctors for ClinicId: {}. Response Status: {}",
                    clinicId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to delete doctors for ClinicId: {}. Error: {}",
                    clinicId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to delete doctors by clinic");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> getDoctorByClinicAndDoctorId(String clinicId, String doctorId) {

        log.info("Received request to fetch doctor details. ClinicId: {}, DoctorId: {}",
                clinicId, doctorId);

        try {

            log.debug("Calling Clinic Admin Feign client to fetch doctor details for ClinicId: {}, DoctorId: {}",
                    clinicId, doctorId);

            ResponseEntity<Response> response = clinicAdminFeign.getDoctorByClinicAndDoctorId(
                    keyCloakTokenStore.getAccess_token(),
                    clinicId,
                    doctorId);

            log.info("Successfully fetched doctor details. ClinicId: {}, DoctorId: {}, Response Status: {}",
                    clinicId, doctorId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to fetch doctor details. ClinicId: {}, DoctorId: {}, Error: {}",
                    clinicId, doctorId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch doctor for clinic");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> getDoctorsByHospitalId(String hospitalId) {

        log.info("Received request to fetch doctors by HospitalId: {}", hospitalId);

        try {

            log.debug("Calling Clinic Admin Feign client to fetch doctors for HospitalId: {}", hospitalId);

            ResponseEntity<Response> response = clinicAdminFeign.getDoctorsByHospitalId(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId);

            log.info("Successfully fetched doctors for HospitalId: {}. Response Status: {}",
                    hospitalId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to fetch doctors for HospitalId: {}. Error: {}",
                    hospitalId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch doctors by hospital");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> getDoctorsByHospitalIdAndBranchId(String hospitalId, String branchId) {

        log.info("Received request to fetch doctors. HospitalId: {}, BranchId: {}",
                hospitalId, branchId);

        try {

            log.debug("Calling Clinic Admin Feign client to fetch doctors for HospitalId: {}, BranchId: {}",
                    hospitalId, branchId);

            ResponseEntity<Response> response = clinicAdminFeign.getDoctorsByHospitalIdAndBranchId(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId,
                    branchId);

            log.info("Successfully fetched doctors. HospitalId: {}, BranchId: {}, Response Status: {}",
                    hospitalId, branchId, response.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to fetch doctors. HospitalId: {}, BranchId: {}, Error: {}",
                    hospitalId, branchId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch doctors by branch");
        }
    }

    private ResponseEntity<Response> handleFeignException(FeignException ex, String adminMessage) {

        HttpStatus status = HttpStatus.resolve(ex.status()) != null
                ? HttpStatus.valueOf(ex.status())
                : HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Feign exception occurred. Status: {}, Message: {}",
                status.value(), ex.getMessage(), ex);

        try {
            if (ex.responseBody().isPresent()) {

                log.debug("Feign response body found. Parsing response body.");

                String body = new String(ex.responseBody().get().array(), StandardCharsets.UTF_8);

                Response clinicResponse = objectMapper.readValue(body, Response.class);

                String finalMessage = (clinicResponse.getMessage() != null
                        && !clinicResponse.getMessage().isEmpty())
                        ? clinicResponse.getMessage()
                        : adminMessage;

                clinicResponse.setMessage(finalMessage);
                clinicResponse.setSuccess(false);

                log.info("Returning Feign error response. Status: {}, Message: {}",
                        status.value(), finalMessage);

                return ResponseEntity.status(status).body(clinicResponse);
            }

            log.warn("Feign exception response body is empty. Returning fallback response.");

        } catch (Exception e) {

            log.error("Failed to parse Feign response body. Error: {}",
                    e.getMessage(), e);
        }

        Response fallback = new Response();
        fallback.setSuccess(false);
        fallback.setMessage(adminMessage);
        fallback.setStatus(status.value());
        fallback.setData(null);

        log.info("Returning fallback error response. Status: {}, Message: {}",
                status.value(), adminMessage);

        return ResponseEntity.status(status).body(fallback);
    }
}
