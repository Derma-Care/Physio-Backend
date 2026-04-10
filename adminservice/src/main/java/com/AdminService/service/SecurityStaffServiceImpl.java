package com.AdminService.service;

import com.AdminService.dto.SecurityStaffDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityStaffServiceImpl implements SecurityStaffService {

    private final ClinicAdminFeign clinicAdminFeign;

    @Override
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> addSecurityStaff(SecurityStaffDTO dto) {
        try {
            ResponseStructure<SecurityStaffDTO> response = clinicAdminFeign.addSecurityStaff(dto);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> updateSecurityStaff(String staffId, SecurityStaffDTO dto) {
        try {
            ResponseStructure<SecurityStaffDTO> response = clinicAdminFeign.updateSecurityStaff(staffId, dto);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> getSecurityStaffById(String staffId) {
        try {
            ResponseStructure<SecurityStaffDTO> response = clinicAdminFeign.getSecurityStaffById(staffId);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getAllByClinicId(String clinicId) {
        try {
            ResponseStructure<List<SecurityStaffDTO>> response = clinicAdminFeign.getAllByClinicId(clinicId);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<String>> deleteSecurityStaff(String staffId) {
        try {
            ResponseStructure<String> response = clinicAdminFeign.deleteSecurityStaff(staffId);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getSecurityStaffByClinicIdAndBranchId(String clinicId, String branchId) {
        try {
            ResponseStructure<List<SecurityStaffDTO>> response = clinicAdminFeign.getSecurityStaffByClinicIdAndBranchId(clinicId, branchId);
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    // ------------------- Map FeignException -------------------
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ResponseStructure<T>> mapFeignException(FeignException ex) {
        try {
            if (ex.responseBody().isPresent()) {
                String body = new String(ex.responseBody().get().array(), StandardCharsets.UTF_8);
                ResponseStructure<T> clinicResponse = new ObjectMapper().readValue(body, ResponseStructure.class);
                return ResponseEntity.status(clinicResponse.getHttpStatus()).body(clinicResponse);
            }
        } catch (Exception ignored) {}

        // fallback for network issues, service down, etc.
        ResponseStructure<T> fallback = ResponseStructure.buildResponse(
                null,
                "Unable to connect to Clinic Admin Service",
                null,
                500
        );
        return ResponseEntity.status(500).body(fallback);
    }
}
