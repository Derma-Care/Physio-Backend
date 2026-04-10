package com.AdminService.service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.AdminService.dto.LabTechnicianRequestDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LabTechnicianServiceImpl implements LabTechnicianService {

    private final ClinicAdminFeign clinicAdminFeign;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ✅ Add Lab Technician
    @Override
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> addLabTechnician(LabTechnicianRequestDTO dto) {
        try {
            return clinicAdminFeign.createLabTechnician(dto);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to add lab technician");
        }
    }

    // ✅ Get All Lab Technicians
    @Override
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians() {
        try {
            return clinicAdminFeign.getAllLabTechnicians();
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch lab technician list");
        }
    }

    // ✅ Update Lab Technician
    @Override
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnicianById(
            String id, LabTechnicianRequestDTO dto) {
        try {
            return clinicAdminFeign.updateLabTechnician(id, dto);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to update lab technician");
        }
    }

    // ✅ Delete Lab Technician
    @Override
    public ResponseEntity<ResponseStructure<String>> deleteLabTechnicianById(String id) {
        try {
            return clinicAdminFeign.deleteLabTechnician(id);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to delete lab technician");
        }
    }

    // ✅ Get Technicians by Clinic
    @Override
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(String clinicId) {
        try {
            return clinicAdminFeign.getLabTechniciansByClinic(clinicId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch lab technicians by clinic");
        }
    }

    // ✅ Get Technician by Clinic + Technician Id
    @Override
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(
            String clinicId, String technicianId) {
        try {
            return clinicAdminFeign.getLabTechnicianByClinicAndId(clinicId, technicianId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch lab technician by clinic and id");
        }
    }

    // ✅ Get Technicians by Clinic + Branch
    @Override
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(
            String clinicId, String branchId) {
        try {
            return clinicAdminFeign.getLabTechniciansByClinicAndBranch(clinicId, branchId);
        } catch (FeignException ex) {
            return handleFeignException(ex, "Failed to fetch lab technicians by branch");
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

                ResponseStructure<T> errorResponse = ResponseStructure.buildResponse(
                        null,
                        message,
                        status,
                        status.value()
                );

                return (ResponseEntity<ResponseStructure<T>>) (ResponseEntity<?>) 
                        ResponseEntity.status(status).body(errorResponse);
            }
        } catch (Exception ignored) {}

        // ✅ fallback
        ResponseStructure<T> fallback = ResponseStructure.buildResponse(
                null,
                defaultMessage,
                status,
                status.value()
        );
        return ResponseEntity.status(status).body(fallback);
    }
}
