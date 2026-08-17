package com.chiselon.clinicadmin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chiselon.clinicadmin.dto.PatientEmailDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.service.PatientEmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class PatientEmailController {

    private final PatientEmailService patientEmailService;

    @PostMapping("/send-patient-email")
    public ResponseEntity<Response> sendPatientEmail(

            @RequestBody PatientEmailDTO dto) {

        Response response = new Response();

        try {

            patientEmailService.sendPatientEmail(dto);

            response.setSuccess(true);

            response.setMessage(
                    "Patient PDF email sent successfully");

            response.setStatus(
                    HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            response.setSuccess(false);

            response.setMessage(
                    "Failed to send patient PDF email");

            response.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}