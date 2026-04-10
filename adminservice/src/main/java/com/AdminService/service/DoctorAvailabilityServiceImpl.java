package com.AdminService.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.DoctorAvailabilityStatusDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.Response;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private final ClinicAdminFeign clinicAdminFeign;

    @Override
    public ResponseEntity<Response> doctorAvailabilityStatus(String doctorId, DoctorAvailabilityStatusDTO status) {
        try {
            // Call the existing Feign client logic here
            // Assume your Feign client already has some method returning ResponseEntity<Response>
            ResponseEntity<Response> responseEntity = clinicAdminFeign.doctorAvailabilityStatus(doctorId, status);

            // Return the same response
            return ResponseEntity.status(responseEntity.getStatusCode()).body(responseEntity.getBody());

        } catch (FeignException e) {
            Response errorResponse = new Response();
            errorResponse.setMessage(ExtractFeignMessage.clearMessage(e));
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
