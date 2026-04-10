package com.clinicadmin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.ResponseStructure;
import com.clinicadmin.dto.TherapistDTO;
import com.clinicadmin.dto.TherapistLoginDTO;
import com.clinicadmin.dto.TherapistLoginResponseDTO;
import com.clinicadmin.service.TherapistService;

@RestController
@RequestMapping("/clinic-admin")
public class TherapistController {

    @Autowired
    private TherapistService service;

 // ================= CREATE =================
    @PostMapping("/addTherapist")
    public ResponseEntity<Response> createTherapist(
            @RequestBody TherapistDTO dto) {

        Response response = service.therapistOnboarding(dto);

        return ResponseEntity.status(response.getStatus())
                .body(response);
    }

//    // ================= LOGIN =================
//    @PostMapping("/login")
//    public ResponseEntity<ResponseStructure<TherapistLoginResponseDTO>> login(
//            @RequestBody TherapistLoginDTO dto) {
//
//        ResponseStructure<TherapistLoginResponseDTO> response = service.login(dto);
//
//        return ResponseEntity.status(response.getHttpStatus())
//                .body(response);
//    }

    // ================= GET BY therapistId =================
    @GetMapping("/getByTherapistId/{therapistId}")
    public ResponseEntity<ResponseStructure<TherapistDTO>> getByTherapistId(
            @PathVariable String therapistId) {

        ResponseStructure<TherapistDTO> response =
                service.getBytherapistId(therapistId);

        return ResponseEntity.status(response.getHttpStatus())
                .body(response);
    }

    // ================= GET BY CLINIC + BRANCH =================
    @GetMapping("/getByTherapistClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<TherapistDTO>>> getByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        ResponseStructure<List<TherapistDTO>> response =
                service.getByClinicIdAndBranchId(clinicId, branchId);

        return ResponseEntity.status(response.getHttpStatus())
                .body(response);
    }

    // ================= GET BY CLINIC + BRANCH + THERAPIST =================
    @GetMapping("/getByClinicIdBranchIdAndTherapistId/{clinicId}/{branchId}/{therapistId}")
    public ResponseEntity<ResponseStructure<List<TherapistDTO>>> getByClinicIdBranchIdAndTherapistId(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String therapistId) {

        ResponseStructure<List<TherapistDTO>> response =
                service.getByClinicIdBranchIdAndTherapistId(clinicId, branchId, therapistId);

        return ResponseEntity.status(response.getHttpStatus())
                .body(response);
    }

    // ================= UPDATE =================
    @PutMapping("/updateByTherapistId/{therapistId}")
    public ResponseEntity<ResponseStructure<TherapistDTO>> updateByTherapistId(
            @PathVariable String therapistId,
            @RequestBody TherapistDTO dto) {

        ResponseStructure<TherapistDTO> response =
                service.updateBytherapistId(therapistId, dto);

        return ResponseEntity.status(response.getHttpStatus())
                .body(response);
    }

    // ================= DELETE =================
    @DeleteMapping("/deleteByTherapistId/{therapistId}")
    public ResponseEntity<ResponseStructure<String>> deleteByTherapistId(
            @PathVariable String therapistId) {

        ResponseStructure<String> response =
                service.deleteBytherapistId(therapistId);

        return ResponseEntity.status(response.getHttpStatus())
                .body(response);
    }
}