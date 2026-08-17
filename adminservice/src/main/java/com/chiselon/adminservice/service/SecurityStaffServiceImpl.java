package com.chiselon.adminservice.service;

import com.chiselon.adminservice.dto.SecurityStaffDTO;
import com.chiselon.adminservice.util.ClinicAdminFeignImpl;
import com.chiselon.adminservice.util.KeyCloakTokenStore;
import com.chiselon.adminservice.util.ResponseStructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

@RequiredArgsConstructor
public class SecurityStaffServiceImpl implements SecurityStaffService {
	
	   @Autowired
	  private KeyCloakTokenStore keyCloakTokenStore;
	   
    private final ClinicAdminFeignImpl clinicAdminFeign;


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> addSecurityStaff(SecurityStaffDTO dto) {

        log.info("Received request to add Security Staff. Name: {},  ClinicId: {}",
                dto.getSecurityStaffId(), dto.getClinicId());

        try {
            log.debug("Calling ClinicAdmin service to add Security Staff");

            ResponseStructure<SecurityStaffDTO> response =
                    clinicAdminFeign.addSecurityStaff(
                            keyCloakTokenStore.getAccess_token(), dto);

            log.info("Security Staff added successfully. StaffId: {}, Status: {}",
                    response.getData() != null ? response.getData().getSecurityStaffId() : "N/A",
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to add Security Staff. Status: {}, Error: {}",
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while adding Security Staff: {}",
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> updateSecurityStaff(String staffId,
                                                                                   SecurityStaffDTO dto) {

        log.info("Received request to update Security Staff. StaffId: {}", staffId);

        try {

            log.debug("Calling ClinicAdmin service to update Security Staff. StaffId: {}", staffId);

            ResponseStructure<SecurityStaffDTO> response =
                    clinicAdminFeign.updateSecurityStaff(
                            keyCloakTokenStore.getAccess_token(),
                            staffId,
                            dto);

            log.info("Security Staff updated successfully. StaffId: {}, Status: {}",
                    staffId,
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to update Security Staff. StaffId: {}, Status: {}, Error: {}",
                    staffId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while updating Security Staff. StaffId: {}, Error: {}",
                    staffId,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> getSecurityStaffById(String staffId) {

        log.info("Received request to fetch Security Staff. StaffId: {}", staffId);

        try {

            log.debug("Calling ClinicAdmin service to fetch Security Staff. StaffId: {}", staffId);

            ResponseStructure<SecurityStaffDTO> response =
                    clinicAdminFeign.getSecurityStaffById(
                            keyCloakTokenStore.getAccess_token(),
                            staffId);

            log.info("Security Staff fetched successfully. StaffId: {}, Status: {}",
                    staffId,
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch Security Staff. StaffId: {}, Status: {}, Error: {}",
                    staffId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while fetching Security Staff. StaffId: {}, Error: {}",
                    staffId,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getAllByClinicId(String clinicId) {

        log.info("Received request to fetch all Security Staff for ClinicId: {}", clinicId);

        try {

            log.debug("Calling ClinicAdmin service to fetch Security Staff list for ClinicId: {}", clinicId);

            ResponseStructure<List<SecurityStaffDTO>> response =
                    clinicAdminFeign.getAllByClinicId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId);

            int count = response.getData() != null ? response.getData().size() : 0;

            log.info("Successfully fetched {} Security Staff record(s) for ClinicId: {}. Status: {}",
                    count,
                    clinicId,
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch Security Staff list for ClinicId: {}. Status: {}, Error: {}",
                    clinicId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while fetching Security Staff list for ClinicId: {}. Error: {}",
                    clinicId,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<String>> deleteSecurityStaff(String staffId) {

        log.info("Received request to delete Security Staff. StaffId: {}", staffId);

        try {

            log.debug("Calling ClinicAdmin service to delete Security Staff. StaffId: {}", staffId);

            ResponseStructure<String> response =
                    clinicAdminFeign.deleteSecurityStaff(
                            keyCloakTokenStore.getAccess_token(),
                            staffId);

            log.info("Security Staff deleted successfully. StaffId: {}, Status: {}",
                    staffId,
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to delete Security Staff. StaffId: {}, Status: {}, Error: {}",
                    staffId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while deleting Security Staff. StaffId: {}, Error: {}",
                    staffId,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }


    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getSecurityStaffByClinicIdAndBranchId(
            String clinicId, String branchId) {

        log.info("Received request to fetch Security Staff. ClinicId: {}, BranchId: {}",
                clinicId, branchId);

        try {

            log.debug("Calling ClinicAdmin service to fetch Security Staff for ClinicId: {}, BranchId: {}",
                    clinicId, branchId);

            ResponseStructure<List<SecurityStaffDTO>> response =
                    clinicAdminFeign.getSecurityStaffByClinicIdAndBranchId(
                            keyCloakTokenStore.getAccess_token(),
                            clinicId,
                            branchId);

            int count = response.getData() != null ? response.getData().size() : 0;

            log.info("Successfully fetched {} Security Staff record(s). ClinicId: {}, BranchId: {}, Status: {}",
                    count,
                    clinicId,
                    branchId,
                    response.getHttpStatus());

            return ResponseEntity.status(response.getHttpStatus()).body(response);

        } catch (FeignException ex) {

            log.error("Failed to fetch Security Staff. ClinicId: {}, BranchId: {}, Status: {}, Error: {}",
                    clinicId,
                    branchId,
                    ex.status(),
                    ex.contentUTF8(),
                    ex);

            return mapFeignException(ex);

        } catch (Exception ex) {

            log.error("Unexpected error while fetching Security Staff. ClinicId: {}, BranchId: {}, Error: {}",
                    clinicId,
                    branchId,
                    ex.getMessage(),
                    ex);

            throw ex;
        }
    }

    // ------------------- Map FeignException -------------------
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ResponseStructure<T>> mapFeignException(FeignException ex) {

        log.error("Feign exception occurred. Status: {}, Message: {}",
                ex.status(),
                ex.getMessage(),
                ex);

        try {

            if (ex.responseBody().isPresent()) {

                log.debug("Feign response body received. Parsing response body.");

                String body = new String(
                        ex.responseBody().get().array(),
                        StandardCharsets.UTF_8);

                ResponseStructure<T> clinicResponse =
                        new ObjectMapper().readValue(body, ResponseStructure.class);

                log.info("Returning error response received from Clinic Admin Service. Status: {}, Message: {}",
                        clinicResponse.getHttpStatus(),
                        clinicResponse.getMessage());

                return ResponseEntity
                        .status(clinicResponse.getHttpStatus())
                        .body(clinicResponse);
            }

            log.warn("Feign exception received without a response body.");

        } catch (Exception e) {

            log.error("Failed to parse Feign error response. Error: {}",
                    e.getMessage(),
                    e);
        }

        log.error("Returning fallback response. Unable to connect to Clinic Admin Service.");

        ResponseStructure<T> fallback = ResponseStructure.buildResponse(
                null,
                "Unable to connect to Clinic Admin Service",
                null,
                500
        );

        return ResponseEntity.status(500).body(fallback);
    }
}
