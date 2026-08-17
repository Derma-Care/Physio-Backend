package com.chiselon.adminservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.adminservice.dto.DoctorAvailabilityStatusDTO;
import com.chiselon.adminservice.service.DoctorAvailabilityService;
import com.chiselon.adminservice.util.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")

 class DoctorAvailabilityController {

    private final DoctorAvailabilityService doctorAvailabilityService;

    @PostMapping("/doctorAvailabilityStatus/{doctorId}")
    public ResponseEntity<Response> doctorAvailabilityStatus(@PathVariable String doctorId,
                                                             @RequestBody DoctorAvailabilityStatusDTO status) {
        return doctorAvailabilityService.doctorAvailabilityStatus(doctorId, status);
    }
}
