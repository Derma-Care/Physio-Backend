package com.AdminService.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.PharmacistDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PharmacistServiceImpl implements PharmacistService {

    private final ClinicAdminFeign clinicAdminFeign;

    @Override
    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(PharmacistDTO dto) {
        try {
            return clinicAdminFeign.pharmacistOnBoarding(dto);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(String hospitalId) {
        try {
            return clinicAdminFeign.getAllByDepartment(hospitalId);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(String pharmacistId) {
        try {
            return clinicAdminFeign.getPharmacist(pharmacistId);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(String pharmacistId, PharmacistDTO dto) {
        try {
            return clinicAdminFeign.updatePharmacist(pharmacistId, dto);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<String>> deletePharmacist(String pharmacistId) {
        try {
            return clinicAdminFeign.deletePharmacist(pharmacistId);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    @Override
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(String hospitalId, String branchId) {
        try {
            return clinicAdminFeign.getPharmacistsByHospitalIdAndBranchId(hospitalId, branchId);
        } catch (FeignException ex) {
            return mapFeignException(ex);
        }
    }

    /**
     * Map FeignException to ResponseEntity, preserving Clinic Admin's response.
     * Handles cases when Clinic Admin returns validation or error messages.
     */
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
