package com.AdminService.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.AdminService.dto.WardBoyDTO;
import com.AdminService.util.ClinicAdminFeignImpl;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.KeyCloakTokenStore;
import com.AdminService.util.ResponseStructure;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WardBoyServiceImpl implements WardBoyService {

    private final ClinicAdminFeignImpl clinicAdminFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    

    // ✅ Add WardBoy
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> addWardBoy(WardBoyDTO dto) {

        log.info("Started addWardBoy()");

        try {

            log.info("Sending request to Clinic Admin Service to add WardBoy. Name: {}, ClinicId: {}, BranchId: {}",
                    dto.getFullName(), dto.getClinicId(), dto.getBranchId());

            ResponseStructure<WardBoyDTO> response =
                    clinicAdminFeign.addWardBoy(keyCloakTokenStore.getAccess_token(), dto);

            log.info("WardBoy created successfully. Status Code: {}", response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (FeignException e) {

            log.error("FeignException occurred while adding WardBoy. Status: {}, Error: {}",
                    e.status(), ExtractFeignMessage.clearMessage(e), e);

            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Update WardBoy
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> updateWardBoy(String id, WardBoyDTO dto) {

        log.info("Started updateWardBoy() for WardBoy ID: {}", id);

        try {

            log.info("Sending request to Clinic Admin Service to update WardBoy. ID: {}, Name: {}, ClinicId: {}, BranchId: {}",
                    id, dto.getFullName(), dto.getClinicId(), dto.getBranchId());

            ResponseStructure<WardBoyDTO> response =
                    clinicAdminFeign.updateWardBoy(keyCloakTokenStore.getAccess_token(), id, dto);

            log.info("WardBoy updated successfully. ID: {}, Status Code: {}",
                    id, response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (FeignException e) {

            log.error("FeignException occurred while updating WardBoy. ID: {}, Status: {}, Error: {}",
                    id, e.status(), ExtractFeignMessage.clearMessage(e), e);

            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoy by ID
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyById(String id) {

        log.info("Started getWardBoyById() for WardBoy ID: {}", id);

        try {

            log.info("Sending request to Clinic Admin Service to fetch WardBoy with ID: {}", id);

            ResponseStructure<WardBoyDTO> response =
                    clinicAdminFeign.getWardBoyById(keyCloakTokenStore.getAccess_token(), id);

            log.info("WardBoy retrieved successfully. ID: {}, Status Code: {}",
                    id, response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (FeignException e) {

            log.error("FeignException occurred while retrieving WardBoy. ID: {}, Status: {}, Error: {}",
                    id, e.status(), ExtractFeignMessage.clearMessage(e), e);

            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get All WardBoys
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getAllWardBoys() {

        log.info("Started getAllWardBoys()");

        try {

            log.info("Sending request to Clinic Admin Service to retrieve all WardBoy records");

            ResponseStructure<List<WardBoyDTO>> response =
                    clinicAdminFeign.getAllWardBoys(keyCloakTokenStore.getAccess_token());

            int totalRecords = response.getData() != null ? response.getData().size() : 0;

            log.info("Successfully retrieved {} WardBoy record(s). Status Code: {}",
                    totalRecords, response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (FeignException e) {

            log.error("FeignException occurred while retrieving all WardBoy records. Status: {}, Error: {}",
                    e.status(), ExtractFeignMessage.clearMessage(e), e);

            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }
    // ✅ Get WardBoys by Clinic ID
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicId(String clinicId) {
        try {
            ResponseStructure<List<WardBoyDTO>> response = clinicAdminFeign.getWardBoysByClinicId(keyCloakTokenStore.getAccess_token(),clinicId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoy by ID and Clinic ID
    @Override
	@Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyByIdAndClinicId(String wardBoyId, String clinicId) {
        try {
            ResponseStructure<WardBoyDTO> response = clinicAdminFeign.getWardBoyByIdAndClinicId(keyCloakTokenStore.getAccess_token(),wardBoyId, clinicId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

 // ✅ Delete WardBoy
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<Void>> deleteWardBoy(String id) {

        log.info("Started deleteWardBoy() for WardBoy ID: {}", id);

        try {

            log.info("Sending request to Clinic Admin Service to delete WardBoy with ID: {}", id);

            ResponseStructure<Void> response =
                    clinicAdminFeign.deleteWardBoy(keyCloakTokenStore.getAccess_token(), id);

            log.info("WardBoy deleted successfully. ID: {}, Status Code: {}",
                    id, response.getStatusCode());

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (FeignException e) {

            log.error("FeignException occurred while deleting WardBoy. ID: {}, Status: {}, Error: {}",
                    id, e.status(), ExtractFeignMessage.clearMessage(e), e);

            ResponseStructure<Void> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoys by ClinicId + BranchId (already returns ResponseEntity)
    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(
            String clinicId, String branchId) {

        log.info("Started getWardBoysByClinicIdAndBranchId(). ClinicId: {}, BranchId: {}",
                clinicId, branchId);

        try {

            log.info("Sending request to Clinic Admin Service to retrieve WardBoy records for ClinicId: {}, BranchId: {}",
                    clinicId, branchId);

            ResponseEntity<ResponseStructure<List<WardBoyDTO>>> response =
                    clinicAdminFeign.getWardBoysByClinicIdAndBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            int totalRecords = response.getBody() != null && response.getBody().getData() != null
                    ? response.getBody().getData().size()
                    : 0;

            log.info("Successfully retrieved {} WardBoy record(s) for ClinicId: {}, BranchId: {}. Status Code: {}",
                    totalRecords,
                    clinicId,
                    branchId,
                    response.getStatusCode().value());

            return response;

        } catch (FeignException e) {

            log.error("FeignException occurred while retrieving WardBoy records for ClinicId: {}, BranchId: {}. Status: {}, Error: {}",
                    clinicId,
                    branchId,
                    e.status(),
                    ExtractFeignMessage.clearMessage(e),
                    e);

            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );

            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }
}
