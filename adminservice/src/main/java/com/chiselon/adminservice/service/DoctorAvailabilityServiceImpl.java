package com.chiselon.adminservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import com.chiselon.adminservice.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.adminservice.util.ClinicAdminFeignImpl;
import com.chiselon.adminservice.util.ExtractFeignMessage;
import com.chiselon.adminservice.util.KeyCloakTokenStore;
import com.chiselon.adminservice.util.Response;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    private final ClinicAdminFeignImpl clinicAdminFeign;
    
    @Autowired
    private KeyCloakTokenStore keyCloakTokenStore;
    

    @Override
    @Secured("ROLE_ADMIN")
    public ResponseEntity<Response> doctorAvailabilityStatus(String doctorId, DoctorAvailabilityStatusDTO status) {

        log.info("Received request to update doctor availability status. DoctorId: {}, Status: {}",
                doctorId, status.isDoctorAvailabilityStatus());

        try {

            log.debug("Calling Clinic Admin Feign client to update doctor availability.");

            ResponseEntity<Response> responseEntity = clinicAdminFeign.doctorAvailabilityStatus(
                    keyCloakTokenStore.getAccess_token(),
                    doctorId,
                    status);

            log.info("Doctor availability updated successfully. DoctorId: {}, Response Status: {}",
                    doctorId, responseEntity.getStatusCode());

            return ResponseEntity.status(responseEntity.getStatusCode())
                    .body(responseEntity.getBody());

        } catch (FeignException e) {

            log.error("Error while updating doctor availability. DoctorId: {}, Error: {}",
                    doctorId, ExtractFeignMessage.clearMessage(e), e);

            Response errorResponse = new Response();
            errorResponse.setMessage(ExtractFeignMessage.clearMessage(e));
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
}
