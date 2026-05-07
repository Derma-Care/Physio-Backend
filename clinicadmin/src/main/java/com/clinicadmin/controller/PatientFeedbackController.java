package com.clinicadmin.controller;

import com.clinicadmin.dto.PatientFeedbackDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.PatientFeedbackService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clinic-admin")
public class PatientFeedbackController {

    @Autowired
    private PatientFeedbackService service;

    // ================= CREATE =================

    @PostMapping("/createPatientFeedback")
    public Response createFeedback(
            @RequestBody PatientFeedbackDTO dto) {

        return service.createFeedback(dto);
    }

    // ================= GET ALL =================

    @GetMapping("/getAllPatientFeedback")
    public Response getAllPatientFeedbacks() {

        return service.getAllFeedbacks();
    }

    // ================= GET BY ID =================

    @GetMapping("/getPatientFeedbackById/{id}")
    public Response getPatientFeedbackById(
            @PathVariable String id) {

        return service.getFeedbackById(id);
    }

    // ================= UPDATE =================

    @PutMapping("/updatePatientFeedback/{id}")
    public Response updatePatientFeedback(
            @PathVariable String id,
            @RequestBody PatientFeedbackDTO dto) {

        return service.updateFeedback(id, dto);
    }

    // ================= DELETE =================

    @DeleteMapping("/deletePatientFeedback/{id}")
    public Response deletePatientFeedback(
            @PathVariable String id) {

        return service.deleteFeedback(id);
    }
}