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

import feign.FeignException;

@Service
public class DoctorSlotServiceImpl implements DoctorSlotService {

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    

    @Override
	@Secured("ROLE_ADMIN")
    public Response addDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.addDoctorSlot(keyCloakTokenStore.getAccess_token(),hospitalId, branchId, doctorId, slotDto).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
	@Secured("ROLE_ADMIN")
    public Response getDoctorSlots(String hospitalId, String branchId, String doctorId) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.getDoctorSlots(keyCloakTokenStore.getAccess_token(),hospitalId, branchId, doctorId).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
	@Secured("ROLE_ADMIN")
    public Response updateDoctorSlot(UpdateSlotRequestDTO request) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.updateDoctorSlot(keyCloakTokenStore.getAccess_token(),request).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
	@Secured("ROLE_ADMIN")
    public Response deleteDoctorSlot(String doctorId, String branchId, String date, String slot) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(keyCloakTokenStore.getAccess_token(),doctorId, branchId, date, slot).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
	@Secured("ROLE_ADMIN")
    public Response deleteDoctorSlot(String doctorId, String date, String slot) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(keyCloakTokenStore.getAccess_token(),doctorId, date, slot);
        } catch (FeignException e) {
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
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlotsByDate(keyCloakTokenStore.getAccess_token(),doctorId, branchId, date).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
	@Secured("ROLE_ADMIN")
    public boolean updateSlotWhileBooking(String doctorId, String branchId, String date, String time) {
        try {
            return clinicAdminFeign.updateDoctorSlotWhileBooking(keyCloakTokenStore.getAccess_token(),doctorId, branchId, date, time);
        } catch (FeignException e) {
            return false;
        }
    }

    @Override
	@Secured("ROLE_ADMIN")
    public boolean makingFalseSlot(String doctorId, String branchId, String date, String time) {
        try {
            return clinicAdminFeign.makingFalseDoctorSlot(keyCloakTokenStore.getAccess_token(),doctorId, branchId, date, time);
        } catch (FeignException e) {
            return false;
        }
    }

    @Override
	@Secured("ROLE_ADMIN")
    public Response generateDoctorSlots(String doctorId, String branchId, String date,
                                        int intervalMinutes, String openingTime, String closingTime) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.generateSlots(keyCloakTokenStore.getAccess_token(),doctorId, branchId, date, intervalMinutes, openingTime, closingTime);
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
