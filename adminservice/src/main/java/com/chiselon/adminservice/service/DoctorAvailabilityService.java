package com.chiselon.adminservice.service;

import org.springframework.http.ResponseEntity;
import com.chiselon.adminservice.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.adminservice.util.Response;

public interface DoctorAvailabilityService {

    ResponseEntity<Response> doctorAvailabilityStatus(String doctorId, DoctorAvailabilityStatusDTO status);
}
