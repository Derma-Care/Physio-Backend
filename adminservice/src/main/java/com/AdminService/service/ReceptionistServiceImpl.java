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

@Service
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
        try {
            return clinicAdminFeign.createReceptionist(keyCloakTokenStore.getAccess_token(),dto);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionist by ID
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistById(String id) {
        try {
            return clinicAdminFeign.getReceptionistById(keyCloakTokenStore.getAccess_token(),id);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Get All Receptionists
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getAllReceptionists() {
        try {
            return clinicAdminFeign.getAllReceptionists(keyCloakTokenStore.getAccess_token());
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Update Receptionist
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> updateReceptionist(String id, ReceptionistRequestDTO dto) {
        try {
            return clinicAdminFeign.updateReceptionist(keyCloakTokenStore.getAccess_token(),id, dto);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Delete Receptionist
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<String>> deleteReceptionist(String id) {
        try {
            return clinicAdminFeign.deleteReceptionist(keyCloakTokenStore.getAccess_token(),id);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionists by Clinic ID
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinic(String clinicId) {
        try {
            return clinicAdminFeign.getReceptionistsByClinic(keyCloakTokenStore.getAccess_token(),clinicId);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionist by Clinic ID and Receptionist ID
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<ReceptionistRequestDTO>> getReceptionistByClinicAndId(String clinicId, String receptionistId) {
        try {
            return clinicAdminFeign.getReceptionistByClinicAndId(keyCloakTokenStore.getAccess_token(),clinicId, receptionistId);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ✅ Get Receptionists by Clinic and Branch
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<ReceptionistRequestDTO>>> getReceptionistsByClinicAndBranch(String clinicId, String branchId) {
        try {
            return clinicAdminFeign.getReceptionistsByClinicAndBranch(keyCloakTokenStore.getAccess_token(),clinicId, branchId);
        } catch (FeignException ex) {
            return handleFeignException(ex);
        }
    }

    // ⚙️ Common Feign Exception Handler — Extracts message from Clinic Admin Response
    private <T> ResponseEntity<ResponseStructure<T>> handleFeignException(FeignException ex) {
        try {
            String body = ex.responseBody().isPresent()
                    ? new String(ex.responseBody().get().array(), StandardCharsets.UTF_8)
                    : null;

            if (body != null) {
                ResponseStructure<?> errorResponse = objectMapper.readValue(body, ResponseStructure.class);

                // ✅ Forward the same error structure from Clinic Admin
                return ResponseEntity
                        .status(errorResponse.getStatusCode())
                        .body(ResponseStructure.buildResponse(
                                null,
                                errorResponse.getMessage(),
                                errorResponse.getHttpStatus(),
                                errorResponse.getStatusCode()
                        ));
            }

            // If no response body found
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseStructure.buildResponse(
                            null,
                            "Unknown error from Clinic Admin Service",
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500));
        } catch (Exception e) {
            // Parsing failed
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseStructure.buildResponse(
                            null,
                            "Failed to parse Clinic Admin error",
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500));
        }
    }
}
