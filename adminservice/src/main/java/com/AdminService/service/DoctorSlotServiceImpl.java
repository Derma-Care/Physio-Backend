package com.AdminService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.AdminService.dto.DoctorSlotDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.KeyCloakTokenStore;
import com.AdminService.util.Response;

import feign.FeignException;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j

public class DoctorSlotServiceImpl implements DoctorSlotService {

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    
    @Override
    @Secured("ROLE_ADMIN")
    public Response addDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto) {
        log.info("Adding doctor slot. HospitalId: {}, BranchId: {}, DoctorId: {}", hospitalId, branchId, doctorId);

        Response response = new Response();
        try {
            response = clinicAdminFeign.addDoctorSlot(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId, branchId, doctorId, slotDto).getBody();

            log.info("Doctor slot added successfully for DoctorId: {}", doctorId);
        } catch (FeignException e) {
            log.error("Failed to add doctor slot. Status: {}, Message: {}", e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response getDoctorSlots(String hospitalId, String branchId, String doctorId) {
        log.info("Fetching doctor slots. HospitalId: {}, BranchId: {}, DoctorId: {}", hospitalId, branchId, doctorId);

        Response response = new Response();
        try {
            response = clinicAdminFeign.getDoctorSlots(
                    keyCloakTokenStore.getAccess_token(),
                    hospitalId, branchId, doctorId).getBody();

            log.info("Doctor slots fetched successfully for DoctorId: {}", doctorId);
        } catch (FeignException e) {
            log.error("Failed to fetch doctor slots. Status: {}, Message: {}", e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response updateDoctorSlot(UpdateSlotRequestDTO request) {
        log.info("Updating doctor slot for DoctorId: {}", request.getDoctorId());

        Response response = new Response();
        try {
            response = clinicAdminFeign.updateDoctorSlot(
                    keyCloakTokenStore.getAccess_token(), request).getBody();

            log.info("Doctor slot updated successfully for DoctorId: {}", request.getDoctorId());
        } catch (FeignException e) {
            log.error("Failed to update doctor slot. Status: {}, Message: {}", e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response deleteDoctorSlot(String doctorId, String branchId, String date, String slot) {
        log.info("Deleting doctor slot. DoctorId: {}, BranchId: {}, Date: {}, Slot: {}",
                doctorId, branchId, date, slot);

        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, branchId, date, slot).getBody();

            log.info("Doctor slot deleted successfully.");
        } catch (FeignException e) {
            log.error("Failed to delete doctor slot. Status: {}, Message: {}", e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response deleteDoctorSlot(String doctorId, String date, String slot) {
        log.info("Deleting doctor slot. DoctorId: {}, Date: {}, Slot: {}", doctorId, date, slot);

        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, date, slot);

            log.info("Doctor slot deleted successfully.");
        } catch (FeignException e) {
            log.error("Failed to delete doctor slot. Status: {}, Message: {}", e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

//    @Override
//    public Response deleteDoctorSlotsByDate(String doctorId, String date) {
//        Response response = new Response();
//        try {
//            response = clinicAdminFeign.deleteDoctorSlotsByDate(doctorId, date).getBody();
//        } catch (FeignException e) {
//            response.setSuccess(false);
//            response.setMessage(ExtractFeignMessage.clearMessage(e));
//            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//        return response;
//    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response deleteDoctorSlotsByDate(String doctorId, String branchId, String date) {
        log.info("Deleting all doctor slots. DoctorId: {}, BranchId: {}, Date: {}",
                doctorId, branchId, date);

        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlotsByDate(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, branchId, date).getBody();

            log.info("All doctor slots deleted successfully.");
        } catch (FeignException e) {
            log.error("Failed to delete doctor slots by date. Status: {}, Message: {}",
                    e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    @Secured("ROLE_ADMIN")
    public boolean updateSlotWhileBooking(String doctorId, String branchId, String date, String time) {
        log.info("Updating slot while booking. DoctorId: {}, BranchId: {}, Date: {}, Time: {}",
                doctorId, branchId, date, time);

        try {
            boolean updated = clinicAdminFeign.updateDoctorSlotWhileBooking(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, branchId, date, time);

            log.info("Slot updated while booking successfully.");
            return updated;
        } catch (FeignException e) {
            log.error("Failed to update slot while booking. Status: {}, Message: {}",
                    e.status(), e.getMessage());
            return false;
        }
    }
    @Override
    @Secured("ROLE_ADMIN")
    public boolean makingFalseSlot(String doctorId, String branchId, String date, String time) {
        log.info("Marking slot unavailable. DoctorId: {}, BranchId: {}, Date: {}, Time: {}",
                doctorId, branchId, date, time);

        try {
            boolean updated = clinicAdminFeign.makingFalseDoctorSlot(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, branchId, date, time);

            log.info("Slot marked unavailable successfully.");
            return updated;
        } catch (FeignException e) {
            log.error("Failed to mark slot unavailable. Status: {}, Message: {}",
                    e.status(), e.getMessage());
            return false;
        }
    }

    @Override
    @Secured("ROLE_ADMIN")
    public Response generateDoctorSlots(String doctorId, String branchId, String date,
                                        int intervalMinutes, String openingTime, String closingTime) {

        log.info("Generating doctor slots. DoctorId: {}, BranchId: {}, Date: {}",
                doctorId, branchId, date);

        Response response = new Response();
        try {
            response = clinicAdminFeign.generateSlots(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId, branchId, date, intervalMinutes,
                    openingTime, closingTime);

            log.info("Doctor slots generated successfully for DoctorId: {}", doctorId);
        } catch (FeignException e) {
            log.error("Failed to generate doctor slots. Status: {}, Message: {}",
                    e.status(), e.getMessage());

            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
