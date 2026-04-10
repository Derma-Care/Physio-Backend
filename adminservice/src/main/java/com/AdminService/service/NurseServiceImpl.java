package com.AdminService.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.NurseDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NurseServiceImpl implements NurseService {

    private final ClinicAdminFeign clinicAdminFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ResponseEntity<Response> nurseOnBoarding(NurseDTO dto) {
        try {
            return clinicAdminFeign.nurseOnBoarding(dto);
        } catch (FeignException ex) {
            return handleFeignExceptionResponse(ex, "Failed to add nurse", new TypeReference<Response>() {});
        }
    }





    


    // ✅ Get All Nurses by Hospital
    @Override
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(String hospitalId) {
        try {
            return clinicAdminFeign.getAllByHospital(hospitalId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch nurses by hospital", new TypeReference<ResponseStructure<List<NurseDTO>>>() {});
        }
    }

    // ✅ Get Nurse by Hospital and Nurse ID
    @Override
    public ResponseEntity<ResponseStructure<NurseDTO>> getNurse(String hospitalId, String nurseId) {
        try {
            return clinicAdminFeign.getNurse(hospitalId, nurseId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch nurse", new TypeReference<ResponseStructure<NurseDTO>>() {});
        }
    }

    // ✅ Update Nurse
    @Override
    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(String nurseId, NurseDTO dto) {
        try {
            return clinicAdminFeign.updateNurse(nurseId, dto);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to update nurse", new TypeReference<ResponseStructure<NurseDTO>>() {});
        }
    }

    // ✅ Delete Nurse
    @Override
    public ResponseEntity<ResponseStructure<String>> deleteNurse(String hospitalId, String nurseId) {
        try {
            return clinicAdminFeign.deleteNurse(hospitalId, nurseId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to delete nurse", new TypeReference<ResponseStructure<String>>() {});
        }
    }

    // ✅ Get All Nurses by Hospital and Branch
    @Override
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(String hospitalId, String branchId) {
        try {
            return clinicAdminFeign.getAllNursesByBranch(hospitalId, branchId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch nurses by branch", new TypeReference<ResponseStructure<List<NurseDTO>>>() {});
        }
    }

    // ⚙️ Common Feign Exception Handler
    private <T> ResponseEntity<ResponseStructure<T>> handleFeignException(
            FeignException ex, String adminMessage, TypeReference<ResponseStructure<T>> typeRef) {

        try {
            String body = ex.responseBody().isPresent()
                    ? new String(ex.responseBody().get().array(), StandardCharsets.UTF_8)
                    : null;

            ResponseStructure<T> feignResponse =
                    body != null ? objectMapper.readValue(body, typeRef) : null;

            String message = (feignResponse != null && feignResponse.getMessage() != null)
                    ? feignResponse.getMessage()
                    : adminMessage;

            HttpStatus status = ex.status() != 0 ? HttpStatus.valueOf(ex.status()) : HttpStatus.INTERNAL_SERVER_ERROR;

            return ResponseEntity.status(status)
                    .body(ResponseStructure.buildResponse(
                            null,
                            message,
                            status,
                            status.value()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseStructure.buildResponse(
                            null,
                            adminMessage,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            500
                    ));
        }
    }
    private ResponseEntity<Response> handleFeignExceptionResponse(
            FeignException ex, String defaultMessage, TypeReference<Response> typeRef) {

        try {
            String body = ex.responseBody().isPresent()
                    ? new String(ex.responseBody().get().array(), StandardCharsets.UTF_8)
                    : null;

            if (body != null && !body.isEmpty()) {
                Response clinicResponse = objectMapper.readValue(body, typeRef);
                return ResponseEntity.status(clinicResponse.getStatus()).body(clinicResponse);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Response fallback = new Response();
        fallback.setSuccess(false);
        fallback.setMessage(defaultMessage);
        fallback.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallback);
    }


}
