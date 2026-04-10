package com.AdminService.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.AdministratorDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdministratorServiceImpl implements AdministratorService {

    private final ClinicAdminFeign clinicAdminFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseStructure<AdministratorDTO> addAdministrator(AdministratorDTO dto) {
        ResponseStructure<AdministratorDTO> response = new ResponseStructure<>();
        try {
            ResponseStructure<AdministratorDTO> res = clinicAdminFeign.addAdministrator(dto);

            response.setData(res.getData());
            response.setHttpStatus(res.getHttpStatus());
            response.setMessage(res.getMessage());
            response.setStatusCode(res.getStatusCode());
            return response;
        } catch (FeignException ex) {
            String errorMessage = "An unexpected error occurred";

            // Try to extract readable message from Feign response body
            if (ex.contentUTF8() != null && !ex.contentUTF8().isEmpty()) {
                try {
                    // Parse JSON response (Clinic Admin error format)
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(ex.contentUTF8());

                    if (node.has("message")) {
                        errorMessage = node.get("message").asText();
                    }
                } catch (Exception parseEx) {
                    errorMessage = ex.getMessage(); // fallback if parsing fails
                }
            }

            response.setData(null);
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            response.setMessage(errorMessage);
            response.setStatusCode(400);

            return response;
        }
    }



    @Override
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinic(String clinicId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.getAllAdministratorsByClinic(clinicId));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch administrators by clinic");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinicAndBranch(String clinicId, String branchId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.getAllAdministratorsByClinicAndBranch(clinicId, branchId));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch administrators by branch");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicAndId(String clinicId, String adminId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.getAdministratorByClinicAndId(clinicId, adminId));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch administrator");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicBranchAndAdminId(String clinicId, String branchId, String adminId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.getAdministratorByClinicBranchAndAdminId(clinicId, branchId, adminId));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch administrator by clinic and branch");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministrator(String clinicId, String adminId, AdministratorDTO dto) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.updateAdministrator(clinicId, adminId, dto));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to update administrator");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministratorUsingClinicBranchAndAdminId(String clinicId, String branchId, String adminId, AdministratorDTO dto) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.updateAdministratorUsingClinicBranchAndAdminId(clinicId, branchId, adminId, dto));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to update administrator");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<String>> deleteAdministrator(String clinicId, String adminId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.deleteAdministrator(clinicId, adminId));
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to delete administrator");
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<String>> deleteAdministratorUsingClinicBranchAndAdminId(String clinicId, String branchId, String adminId) {
        try {
            return ResponseEntity.ok(clinicAdminFeign.deleteAdministratorUsingClinicBranchAndAdminId(clinicId, branchId, adminId));
        } catch (FeignException ex) {
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