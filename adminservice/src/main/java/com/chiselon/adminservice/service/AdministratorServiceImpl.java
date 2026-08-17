package com.chiselon.adminservice.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.chiselon.adminservice.dto.AdministratorDTO;
import com.chiselon.adminservice.util.ClinicAdminFeignImpl;
import com.chiselon.adminservice.util.KeyCloakTokenStore;
import com.chiselon.adminservice.util.ResponseStructure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministratorServiceImpl implements AdministratorService {

    private final ClinicAdminFeignImpl clinicAdminFeignImpl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
   
    
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseStructure<AdministratorDTO> addAdministrator(AdministratorDTO dto) {

        log.info("Received request to add Administrator. ClinicId: {}, BranchId: {}, Email: {}",
                dto.getClinicId(), dto.getBranchId(), dto.getEmailId());

        ResponseStructure<AdministratorDTO> response = new ResponseStructure<>();

        try {

            log.info("Calling ClinicAdmin service to create Administrator.");

            ResponseStructure<AdministratorDTO> res =
            		clinicAdminFeignImpl.addAdministrator(
                            keyCloakTokenStore.getAccess_token(),
                            dto);

            log.info("Administrator created successfully. AdminId: {}, ClinicId: {}",
                    res.getData() != null ? res.getData().getAdminId() : "N/A",
                    dto.getClinicId());

            response.setData(res.getData());
            response.setHttpStatus(res.getHttpStatus());
            response.setMessage(res.getMessage());
            response.setStatusCode(res.getStatusCode());

            return response;

        } catch (FeignException ex) {

            log.error("Failed to create Administrator. ClinicId: {}, BranchId: {}. Error: {}",
                    dto.getClinicId(),
                    dto.getBranchId(),
                    ex.getMessage(),
                    ex);

            String errorMessage = "An unexpected error occurred";

            if (ex.contentUTF8() != null && !ex.contentUTF8().isEmpty()) {
                try {

                    JsonNode node = objectMapper.readTree(ex.contentUTF8());

                    if (node.has("message")) {
                        errorMessage = node.get("message").asText();
                    }

                } catch (Exception parseEx) {

                    log.error("Failed to parse Feign error response.", parseEx);

                    errorMessage = ex.getMessage();
                }
            }

            log.warn("Returning error response: {}", errorMessage);

            response.setData(null);
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            response.setMessage(errorMessage);
            response.setStatusCode(400);

            return response;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinic(String clinicId) {

        log.info("Received request to fetch all administrators for ClinicId: {}", clinicId);

        try {

            ResponseStructure<List<AdministratorDTO>> response =
            		clinicAdminFeignImpl.getAllAdministratorsByClinic(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId);

            if (response.getData() != null) {
                log.info("Successfully fetched {} administrator(s) for ClinicId: {}",
                        response.getData().size(), clinicId);
            } else {
                log.info("No administrators found for ClinicId: {}", clinicId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch administrators for ClinicId: {}. Error: {}",
                    clinicId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch administrators by clinic");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinicAndBranch(String clinicId, String branchId) {

        log.info("Received request to fetch administrators for ClinicId: {} and BranchId: {}",
                clinicId, branchId);

        try {

            ResponseStructure<List<AdministratorDTO>> response =
            		clinicAdminFeignImpl.getAllAdministratorsByClinicAndBranch(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            if (response.getData() != null) {
                log.info("Successfully fetched {} administrator(s) for ClinicId: {} and BranchId: {}",
                        response.getData().size(), clinicId, branchId);
            } else {
                log.info("No administrators found for ClinicId: {} and BranchId: {}",
                        clinicId, branchId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch administrators for ClinicId: {} and BranchId: {}. Error: {}",
                    clinicId, branchId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch administrators by branch");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicAndId(String clinicId, String adminId) {

        log.info("Received request to fetch Administrator. ClinicId: {}, AdminId: {}",
                clinicId, adminId);

        try {

            ResponseStructure<AdministratorDTO> response =
            		clinicAdminFeignImpl.getAdministratorByClinicAndId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            adminId);

            if (response.getData() != null) {
                log.info("Successfully fetched Administrator. ClinicId: {}, AdminId: {}",
                        clinicId, adminId);
            } else {
                log.info("No Administrator found for ClinicId: {}, AdminId: {}",
                        clinicId, adminId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch Administrator. ClinicId: {}, AdminId: {}. Error: {}",
                    clinicId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch administrator");
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicBranchAndAdminId(
            String clinicId,
            String branchId,
            String adminId) {

        log.info("Received request to fetch Administrator. ClinicId: {}, BranchId: {}, AdminId: {}",
                clinicId, branchId, adminId);

        try {

            ResponseStructure<AdministratorDTO> response =
            		clinicAdminFeignImpl.getAdministratorByClinicBranchAndAdminId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId,
                            adminId);

            if (response.getData() != null) {
                log.info("Successfully fetched Administrator. ClinicId: {}, BranchId: {}, AdminId: {}",
                        clinicId, branchId, adminId);
            } else {
                log.info("No Administrator found. ClinicId: {}, BranchId: {}, AdminId: {}",
                        clinicId, branchId, adminId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch Administrator. ClinicId: {}, BranchId: {}, AdminId: {}. Error: {}",
                    clinicId, branchId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to fetch administrator by clinic and branch");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministrator(
            String clinicId,
            String adminId,
            AdministratorDTO dto) {

        log.info("Received request to update Administrator. ClinicId: {}, AdminId: {}",
                clinicId, adminId);

        try {

            ResponseStructure<AdministratorDTO> response =
            		clinicAdminFeignImpl.updateAdministrator(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            adminId,
                            dto);

            if (response.getData() != null) {
                log.info("Administrator updated successfully. ClinicId: {}, AdminId: {}",
                        clinicId, adminId);
            } else {
                log.warn("Administrator update returned no data. ClinicId: {}, AdminId: {}",
                        clinicId, adminId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to update Administrator. ClinicId: {}, AdminId: {}. Error: {}",
                    clinicId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to update administrator");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministratorUsingClinicBranchAndAdminId(
            String clinicId,
            String branchId,
            String adminId,
            AdministratorDTO dto) {

        log.info("Received request to update Administrator. ClinicId: {}, BranchId: {}, AdminId: {}",
                clinicId, branchId, adminId);

        try {

            ResponseStructure<AdministratorDTO> response =
            		clinicAdminFeignImpl.updateAdministratorUsingClinicBranchAndAdminId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId,
                            adminId,
                            dto);

            if (response.getData() != null) {
                log.info("Administrator updated successfully. ClinicId: {}, BranchId: {}, AdminId: {}",
                        clinicId, branchId, adminId);
            } else {
                log.warn("Administrator update returned no data. ClinicId: {}, BranchId: {}, AdminId: {}",
                        clinicId, branchId, adminId);
            }

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to update Administrator. ClinicId: {}, BranchId: {}, AdminId: {}. Error: {}",
                    clinicId, branchId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to update administrator");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<String>> deleteAdministrator(
            String clinicId,
            String adminId) {

        log.info("Received request to delete Administrator. ClinicId: {}, AdminId: {}",
                clinicId, adminId);

        try {

            ResponseStructure<String> response =
            		clinicAdminFeignImpl.deleteAdministrator(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            adminId);

            log.info("Administrator deleted successfully. ClinicId: {}, AdminId: {}",
                    clinicId, adminId);

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to delete Administrator. ClinicId: {}, AdminId: {}. Error: {}",
                    clinicId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to delete administrator");
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<String>> deleteAdministratorUsingClinicBranchAndAdminId(
            String clinicId,
            String branchId,
            String adminId) {

        log.info("Received request to delete Administrator. ClinicId: {}, BranchId: {}, AdminId: {}",
                clinicId, branchId, adminId);

        try {

            ResponseStructure<String> response =
            		clinicAdminFeignImpl.deleteAdministratorUsingClinicBranchAndAdminId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId,
                            adminId);

            log.info("Administrator deleted successfully. ClinicId: {}, BranchId: {}, AdminId: {}",
                    clinicId, branchId, adminId);

            return ResponseEntity.ok(response);

        } catch (FeignException ex) {

            log.error("Failed to delete Administrator. ClinicId: {}, BranchId: {}, AdminId: {}. Error: {}",
                    clinicId, branchId, adminId, ex.getMessage(), ex);

            return handleFeignException(ex, "Failed to delete administrator");
        }
    }

    // 🔒 Centralized Feign Exception Handler
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ResponseStructure<T>> handleFeignException(FeignException ex, String defaultMessage) {
        HttpStatus status = HttpStatus.resolve(ex.status()) != null
                ? HttpStatus.valueOf(ex.status())
                : HttpStatus.INTERNAL_SERVER_ERROR;

        try {
            if (ex.responseBody().isPresent()) {
                String responseBody = new String(ex.responseBody().get().array(), StandardCharsets.UTF_8);
                ResponseStructure<?> clinicResponse = objectMapper.readValue(responseBody, ResponseStructure.class);
                String message = (clinicResponse.getMessage() != null && !clinicResponse.getMessage().isEmpty())
                        ? clinicResponse.getMessage()
                        : defaultMessage;
                ResponseStructure<T> errorResponse = ResponseStructure.buildResponse(null, message, status, status.value());
                return (ResponseEntity<ResponseStructure<T>>) (ResponseEntity<?>) ResponseEntity.status(status).body(errorResponse);
            }
        } catch (Exception ignored) {}

        ResponseStructure<T> fallback = ResponseStructure.buildResponse(null, defaultMessage, status, status.value());
        return ResponseEntity.status(status).body(fallback);
    }
}