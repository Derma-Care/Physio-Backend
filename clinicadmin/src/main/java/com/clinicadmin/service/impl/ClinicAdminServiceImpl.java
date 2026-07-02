package com.clinicadmin.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.ClinicDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.StaffInfoDTO;
import com.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;
import com.clinicadmin.entity.ClinicAdminDeviceTokenEntity;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.repository.ClinicAdminWebFcmTokenRepository;
import com.clinicadmin.repository.AdministratorRepository;
import com.clinicadmin.repository.ClinicAdminWebFcmTokenRepository;
import com.clinicadmin.repository.DoctorsRepository;
import com.clinicadmin.repository.ReceptionistRepository;
import com.clinicadmin.repository.SecurityStaffRepository;
import com.clinicadmin.repository.TherapistRepository;
import com.clinicadmin.repository.WardBoyRepository;
import com.clinicadmin.service.ClinicAdminService;
import com.clinicadmin.utils.ExtractFeignMessage;
import com.clinicadmin.utils.KeyCloakTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class ClinicAdminServiceImpl implements ClinicAdminService {
	
    @Autowired
    private AdminServiceClient adminServiceClient;
    
    @Autowired
    private  AdministratorRepository administratorRepository;
    
    @Autowired
    private DoctorsRepository doctorsRepository;
    
    @Autowired
    private ReceptionistRepository receptionistRepository;
    
    @Autowired
    private SecurityStaffRepository securityStaffRepository;
    
    @Autowired
    private TherapistRepository therapistRepository;
    
    @Autowired
    private WardBoyRepository wardBoyRepository;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
   
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ClinicAdminWebFcmTokenRepository deviceIdRepo;
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response updateClinicCredentials(UpdateClinicLoginCredentialsDTO updatedCredentials,
                                            String userName) {

        log.info("Received request to update clinic credentials. Username: {}", userName);

        try {

            Response response = adminServiceClient.updateClinicCredentials(
                    keyCloakTokenStore.getAccess_token(),
                    updatedCredentials,
                    userName);

            log.info("Clinic credentials updated successfully. Username: {}", userName);

            return response;

        } catch (FeignException e) {

            log.error("Failed to update clinic credentials. Username: {}, Status: {}, Error: {}",
                    userName,
                    e.status(),
                    e.getMessage(),
                    e);

            Response res = new Response();
            res.setStatus(e.status());
            res.setMessage(ExtractFeignMessage.clearMessage(e));
            res.setSuccess(false);

            return res;
        }
    }

    @Override
    @Secured({"ROLE_CLINICADMIN", "ROLE_DOCTOR"})
    public Response getClinicById(String hospitalId) {

        log.info("Fetching clinic details. HospitalId: {}", hospitalId);

        try {

            ResponseEntity<Response> response = adminServiceClient.getClinicById(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId);

            log.info("Clinic details fetched successfully. HospitalId: {}", hospitalId);

            return response.getBody();

        } catch (FeignException e) {

            log.error("Failed to fetch clinic details. HospitalId: {}, Status: {}, Error: {}",
                    hospitalId,
                    e.status(),
                    e.getMessage(),
                    e);

            Response res = new Response();
            res.setStatus(e.status());
            res.setMessage(ExtractFeignMessage.clearMessage(e));
            res.setSuccess(false);

            return res;
        }
    }

    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response updateClinic(String hospitalId, ClinicDTO dto) {

        log.info("Received request to update clinic. HospitalId: {}", hospitalId);

        try {

            Response response = adminServiceClient.updateClinic(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId,
                    dto);

            log.info("Clinic updated successfully. HospitalId: {}", hospitalId);

            return response;

        } catch (FeignException e) {

            log.error("Failed to update clinic. HospitalId: {}, Status: {}, Error: {}",
                    hospitalId,
                    e.status(),
                    e.getMessage(),
                    e);

            Response res = new Response();
            res.setStatus(e.status());
            res.setMessage(ExtractFeignMessage.clearMessage(e));
            res.setSuccess(false);

            return res;
        }
    }
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response deleteClinic(String hospitalId) {

        log.info("Received request to delete clinic. HospitalId: {}", hospitalId);

        try {

            Response response = adminServiceClient.deleteClinic(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId);

            log.info("Clinic deleted successfully. HospitalId: {}", hospitalId);

            return response;

        } catch (FeignException e) {

            log.error("Failed to delete clinic. HospitalId: {}, Status: {}, Error: {}",
                    hospitalId,
                    e.status(),
                    e.getMessage(),
                    e);

            Response res = new Response();
            res.setStatus(e.status());
            res.setMessage(ExtractFeignMessage.clearMessage(e));
            res.setSuccess(false);

            return res;
        }
    }

    
    @Override
    @Secured("ROLE_CLINICADMIN")
    public ResponseEntity<?> getBranchesByClinicId(String clinicId) {

        log.info("Received request to fetch branches. ClinicId: {}", clinicId);

        try {

            ResponseEntity<?> response = adminServiceClient.getBranchByClinicId(
                    keyCloakTokenStore.getAccess_token(),
                    clinicId);

            log.info("Branches fetched successfully. ClinicId: {}", clinicId);

            return response;

        } catch (FeignException e) {

            log.error("Failed to fetch branches. ClinicId: {}, Status: {}, Error: {}",
                    clinicId,
                    e.status(),
                    e.getMessage(),
                    e);

            try {

                String errorJson = e.contentUTF8();

                log.debug("Parsing AdminService error response. ClinicId: {}", clinicId);

                Response response = objectMapper.readValue(errorJson, Response.class);

                log.info("Returning parsed error response from AdminService. ClinicId: {}", clinicId);

                return ResponseEntity.status(e.status()).body(response);

            } catch (Exception ex) {

                log.error("Failed to parse AdminService error response. ClinicId: {}, Error: {}",
                        clinicId,
                        ex.getMessage(),
                        ex);

                Response fallback = new Response();
                fallback.setSuccess(false);
                fallback.setMessage("Error parsing AdminService response");
                fallback.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallback);
            }
        }
    }
    
    
    @Override
    @Secured("ROLE_CLINICADMIN")
    public Response getStaffInfo(String hospitalId, String branchId) {

        log.info("Fetching staff information. HospitalId: {}, BranchId: {}",
                hospitalId, branchId);

        Response response = new Response();

        try {

            Map<String, List<StaffInfoDTO>> staffMap = new HashMap<>();

            // Administrators
            List<StaffInfoDTO> admins = new ArrayList<>();
            administratorRepository.findByClinicIdAndBranchId(hospitalId, branchId)
                    .forEach(admin -> admins.add(
                            new StaffInfoDTO(
                                    admin.getAdminId(),
                                    admin.getFullName(),
                                    admin.getRole()
                            )));
            staffMap.put("ADMINISTRATOR", admins);

            log.info("Administrators fetched: {}", admins.size());

            // Doctors
            List<StaffInfoDTO> doctors = new ArrayList<>();
            doctorsRepository.findByHospitalIdAndBranchId(hospitalId, branchId)
                    .forEach(doc -> doctors.add(
                            new StaffInfoDTO(
                                    doc.getDoctorId(),
                                    doc.getDoctorName(),
                                    doc.getRole()
                            )));
            staffMap.put("DOCTOR", doctors);

            log.info("Doctors fetched: {}", doctors.size());

            // Receptionists
            List<StaffInfoDTO> receptionists = new ArrayList<>();
            receptionistRepository.findByClinicIdAndBranchId(hospitalId, branchId)
                    .forEach(rec -> receptionists.add(
                            new StaffInfoDTO(
                                    rec.getId(),
                                    rec.getFullName(),
                                    rec.getRole()
                            )));
            staffMap.put("RECEPTIONIST", receptionists);

            log.info("Receptionists fetched: {}", receptionists.size());

            // Security Staff
            List<StaffInfoDTO> securityStaffs = new ArrayList<>();
            securityStaffRepository.findByClinicIdAndBranchId(hospitalId, branchId)
                    .forEach(sec -> securityStaffs.add(
                            new StaffInfoDTO(
                                    sec.getSecurityStaffId(),
                                    sec.getFullName(),
                                    sec.getRole()
                            )));
            staffMap.put("SECURITY_STAFF", securityStaffs);

            log.info("Security staff fetched: {}", securityStaffs.size());

            // Therapists
            List<StaffInfoDTO> therapists = new ArrayList<>();
            therapistRepository.findByClinicIdAndBranchId(hospitalId, branchId)
                    .forEach(therapist -> therapists.add(
                            new StaffInfoDTO(
                                    therapist.getTherapistId(),
                                    therapist.getFullName(),
                                    therapist.getRole()
                            )));
            staffMap.put("THERAPIST", therapists);

            log.info("Therapists fetched: {}", therapists.size());

            // Ward Boys
            List<StaffInfoDTO> wardBoys = new ArrayList<>();
            wardBoyRepository.findByClinicIdAndBranchId(hospitalId, branchId)
                    .forEach(wardBoy -> wardBoys.add(
                            new StaffInfoDTO(
                                    wardBoy.getWardBoyId(),
                                    wardBoy.getFullName(),
                                    wardBoy.getRole()
                            )));
            staffMap.put("WARD_BOY", wardBoys);

            log.info("Ward boys fetched: {}", wardBoys.size());

            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Staff information fetched successfully");
            response.setData(staffMap);

            log.info("Staff information fetched successfully. HospitalId: {}, BranchId: {}",
                    hospitalId, branchId);

        } catch (Exception e) {

            log.error("Failed to fetch staff information. HospitalId: {}, BranchId: {}, Error: {}",
                    hospitalId,
                    branchId,
                    e.getMessage(),
                    e);

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage(e.getMessage());
        }

        return response;
    }
    
    @Override
    @Secured({"ROLE_CLINICADMIN", "ROLE_NOTIFICATIONSERVICE"})
    public String getDeviceId(String clinicId, String branchId) {

        log.info("Fetching device ID. ClinicId: {}, BranchId: {}", clinicId, branchId);

        Optional<ClinicAdminDeviceTokenEntity> obj = null;
        String deviceId = null;

        try {

            obj = deviceIdRepo.findByUsername(clinicId);

            if (obj.isPresent()) {

                deviceId = obj.get().getClinicAdminWebFcmToken();

                log.info("Device ID found for ClinicId: {}", clinicId);

            } else {

                log.info("No device ID found for ClinicId: {}. Checking BranchId: {}",
                        clinicId, branchId);

                obj = deviceIdRepo.findByUsername(branchId);

                if (obj.isPresent()) {

                    deviceId = obj.get().getClinicAdminWebFcmToken();

                    log.info("Device ID found for BranchId: {}", branchId);

                } else {

                    log.warn("No device ID found for ClinicId: {} or BranchId: {}",
                            clinicId, branchId);
                }
            }

        } catch (Exception e) {

            log.error("Failed to fetch device ID. ClinicId: {}, BranchId: {}, Error: {}",
                    clinicId,
                    branchId,
                    e.getMessage(),
                    e);

            return null;
        }

        log.info("Returning device ID for ClinicId: {}, BranchId: {}",
                clinicId, branchId);

        return deviceId;
    }
}
