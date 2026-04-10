package com.AdminService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.AdminService.dto.DoctorSlotDTO;
import com.AdminService.dto.UpdateSlotRequestDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.Response;

import feign.FeignException;

@Service
public class DoctorSlotServiceImpl implements DoctorSlotService {

    @Autowired
    private ClinicAdminFeign clinicAdminFeign;

    @Override
    public Response addDoctorSlot(String hospitalId, String branchId, String doctorId, DoctorSlotDTO slotDto) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.addDoctorSlot(hospitalId, branchId, doctorId, slotDto).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response getDoctorSlots(String hospitalId, String branchId, String doctorId) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.getDoctorSlots(hospitalId, branchId, doctorId).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response updateDoctorSlot(UpdateSlotRequestDTO request) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.updateDoctorSlot(request).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response deleteDoctorSlot(String doctorId, String branchId, String date, String slot) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(doctorId, branchId, date, slot).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response deleteDoctorSlot(String doctorId, String date, String slot) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlot(doctorId, date, slot);
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response deleteDoctorSlotsByDate(String doctorId, String date) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlotsByDate(doctorId, date).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public Response deleteDoctorSlotsByDate(String doctorId, String branchId, String date) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.deleteDoctorSlotsByDate(doctorId, branchId, date).getBody();
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    @Override
    public boolean updateSlotWhileBooking(String doctorId, String branchId, String date, String time) {
        try {
            return clinicAdminFeign.updateDoctorSlotWhileBooking(doctorId, branchId, date, time);
        } catch (FeignException e) {
            return false;
        }
    }

    @Override
    public boolean makingFalseSlot(String doctorId, String branchId, String date, String time) {
        try {
            return clinicAdminFeign.makingFalseDoctorSlot(doctorId, branchId, date, time);
        } catch (FeignException e) {
            return false;
        }
    }

    @Override
    public Response generateDoctorSlots(String doctorId, String branchId, String date,
                                        int intervalMinutes, String openingTime, String closingTime) {
        Response response = new Response();
        try {
            response = clinicAdminFeign.generateSlots(doctorId, branchId, date, intervalMinutes, openingTime, closingTime);
        } catch (FeignException e) {
            response.setSuccess(false);
            response.setMessage(ExtractFeignMessage.clearMessage(e));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
