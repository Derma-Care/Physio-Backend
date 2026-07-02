package com.AdminService.service;

import com.AdminService.dto.ReceptionistRequestDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.KeyCloakTokenStore;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import feign.FeignException;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

@RequiredArgsConstructor
public class ReceptionistServiceImpl implements ReceptionistService {
	
	
	 @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
	   
    private final ClinicAdminFeign clinicAdminFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Create Receptionist
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> createReceptionist(ReceptionistRequestDTO dto) {

        log.info("Creating receptionist for ClinicId: {}, BranchId: {}", dto.getClinicId(), dto.getBranchId());

        try {
            ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> response =
                    clinicAdminFeign.createReceptionist(keyCloakTokenStore.getAccess_token(), dto);

            log.info("Receptionist created successfully.");
            return response;

        } catch (FeignException ex) {
            log.error("Failed to create receptionist. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionist by ID
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(String id) {

        log.info("Fetching receptionist with Id: {}", id);

        try {
            ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> response =
                    clinicAdminFeign.getReceptionistById(keyCloakTokenStore.getAccess_token(), id);

            log.info("Receptionist fetched successfully. Id: {}", id);
            return response;

        } catch (FeignException ex) {
            log.error("Failed to fetch receptionist. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Get All Receptionists
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists() {

        log.info("Fetching all receptionists.");

        try {
            ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> response =
                    clinicAdminFeign.getAllReceptionists(keyCloakTokenStore.getAccess_token());

            log.info("Successfully fetched all receptionists.");
            return response;

        } catch (FeignException ex) {
            log.error("Failed to fetch all receptionists. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Update Receptionist
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(String id, ReceptionistRequestDTO dto) {

        log.info("Updating receptionist with Id: {}", id);

        try {
            ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> response =
                    clinicAdminFeign.updateReceptionist(keyCloakTokenStore.getAccess_token(), id, dto);

            log.info("Receptionist updated successfully. Id: {}", id);
            return response;

        } catch (FeignException ex) {
            log.error("Failed to update receptionist. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Delete Receptionist
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<String>> deleteReceptionist(String id) {

        log.info("Deleting receptionist with Id: {}", id);

        try {
            ResponseEntity<ResponseStructure<String>> response =
                    clinicAdminFeign.deleteReceptionist(keyCloakTokenStore.getAccess_token(), id);

            log.info("Receptionist deleted successfully. Id: {}", id);
            return response;

        } catch (FeignException ex) {
            log.error("Failed to delete receptionist. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionists by Clinic ID
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(String clinicId) {

        log.info("Fetching receptionists for ClinicId: {}", clinicId);

        try {
            ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> response =
                    clinicAdminFeign.getReceptionistsByClinic(keyCloakTokenStore.getAccess_token(), clinicId);

            log.info("Receptionists fetched successfully for ClinicId: {}", clinicId);
            return response;

        } catch (FeignException ex) {
            log.error("Failed to fetch receptionists by clinic. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionist by Clinic ID and Receptionist ID
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(String clinicId, String receptionistId) {

        log.info("Fetching receptionist. ClinicId: {}, ReceptionistId: {}", clinicId, receptionistId);

        try {
            ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> response =
                    clinicAdminFeign.getReceptionistByClinicAndId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            receptionistId);

            log.info("Receptionist fetched successfully.");
            return response;

        } catch (FeignException ex) {
            log.error("Failed to fetch receptionist by clinic and id. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionists by Clinic and Branch
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(String clinicId, String branchId) {

        log.info("Fetching receptionists. ClinicId: {}, BranchId: {}", clinicId, branchId);

        try {
            ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> response =
                    clinicAdminFeign.getReceptionistsByClinicAndBranch(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            log.info("Receptionists fetched successfully.");
            return response;

        } catch (FeignException ex) {
            log.error("Failed to fetch receptionists by clinic and branch. Status: {}, Message: {}", ex.status(), ex.getMessage());
            return handleFeignException(ex);
        }
    }

    // ⚙️ Common Feign Exception Handler — Extracts message from Clinic Admin Response
    private <T> ResponseEntity<ResponseStructure<T>> handleFeignException(FeignException ex) {

        log.error("Handling Feign exception. Status: {}, Message: {}", ex.status(), ex.getMessage());

        try {
            String body = ex.responseBody().isPresent()
                    ? new String(ex.responseBody().get().array(), StandardCharsets.UTF_8)
                    : null;

            if (body != null) {
                ResponseStructure<?> errorResponse = objectMapper.readValue(body, ResponseStructure.class);

                log.error("Clinic Admin Error: {}", errorResponse.getMessage());

                return ResponseEntity
                        .status(errorResponse.getStatusCode())
                        .body(ResponseStructure.buildResponse(
                                null,
                                errorResponse.getMessage(),
                                errorResponse.getHttpStatus(),
                                errorResponse.getStatusCode()
                        ));
            }

            log.error("No response body received from Clinic Admin Service.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseStructure.buildResponse(
                            null,
                            "Unknown error from Clinic Admin Service",
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500));

        } catch (Exception e) {

            log.error("Failed to parse Clinic Admin error response.", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseStructure.buildResponse(
                            null,
                            "Failed to parse Clinic Admin error",
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500));
        }
    }
}