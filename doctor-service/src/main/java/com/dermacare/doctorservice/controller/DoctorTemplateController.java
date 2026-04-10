package com.dermacare.doctorservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermacare.doctorservice.dto.DoctorTemplateDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.service.DoctorTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})

public class DoctorTemplateController {

    private final DoctorTemplateService doctorTemplateService;

    @PostMapping(value = "/createDoctorTemplate", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Response> createDoctorTemplate(@Valid @RequestBody DoctorTemplateDTO dto) {
        return ResponseEntity.status(201).body(doctorTemplateService.createTemplate(dto));
    }

    @GetMapping("/getDoctorTemplateById/{id}")
    public ResponseEntity<Response> getDoctorTemplateById(@PathVariable String id) {
        return ResponseEntity.ok(doctorTemplateService.getTemplateById(id));
    }

    @GetMapping("getAllDoctorTemplates")
    public ResponseEntity<Response> getAllDoctorTemplates() {
        return ResponseEntity.ok(doctorTemplateService.getAllTemplates());
    }

    @DeleteMapping("deleteDoctorTemplate/{id}")
    public ResponseEntity<Response> deleteDoctorTemplate(@PathVariable String id) {
        return ResponseEntity.ok(doctorTemplateService.deleteTemplate(id));
    }
    @GetMapping("/searchTemplate/{keyword}")
    public ResponseEntity<Response> searchTemplateByTitle(@PathVariable String keyword) {
        Response response = doctorTemplateService.searchTemplatesByTitle(keyword);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    @GetMapping("/getTemplatesByClinicId/{clinicId}")
    public ResponseEntity<Response> getTemplatesByClinicId(@PathVariable String clinicId) {
        Response response = doctorTemplateService.getTemplatesByClinicId(clinicId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    @GetMapping("/getTemplatesByClinicIdAndTitle/{clinicId}/{title}")
    public ResponseEntity<Response> getTemplatesByClinicIdAndTitle(
            @PathVariable String clinicId,
            @PathVariable String title) {
        Response response = doctorTemplateService.getTemplatesByClinicIdAndTitle(clinicId, title);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

}
