package com.AdminService.service;

import org.springframework.http.ResponseEntity;
import com.AdminService.dto.DoctorAvailabilityStatusDTO;
import com.AdminService.util.Response;

public interface DoctorAvailabilityService {

    ResponseEntity<Response> doctorAvailabilityStatus(String doctorId, DoctorAvailabilityStatusDTO status);
}
