package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clinicadmin.dto.PostProcedureFormDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.PostProcedureFormService;

@RestController
@RequestMapping("/clinic-admin")
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PostProcedureFormController {

    @Autowired
    PostProcedureFormService postProcedureFormService;

    // Add Post Procedure Form
    @PostMapping("/addPostProcedureForm/{hospitalId}/{subServiceId}")
    public ResponseEntity<Response> addPostProcedureForm(
            @PathVariable String hospitalId,
            @PathVariable String subServiceId,
            @RequestBody PostProcedureFormDTO dto) {

        Response response = postProcedureFormService.addPostProcedureForm(hospitalId, subServiceId, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // Get by hospitalId and postProcedureFormId
    @GetMapping("/getPostProcedureFormByHospitalIdAndPostProcedureId/{hospitalId}/{postProcedureId}")
    public ResponseEntity<Response> getPostProcedureFormByHospitalIdAndPostProcedureId(
            @PathVariable String hospitalId,
            @PathVariable String postProcedureId) {

        Response response = postProcedureFormService.getPostProcedureFormById(hospitalId, postProcedureId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // Get all
    @GetMapping("/getAllPostProcedureForms")
    public ResponseEntity<Response> getAllPostProcedureForms() {
        Response response = postProcedureFormService.getAllPostProcedureForms();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // Update
    @PutMapping("/update-postprocedure-forms/{hospitalId}/{postProcedureFormId}")
    public ResponseEntity<Response> updatePostProcedureForm(
            @PathVariable String hospitalId,
            @PathVariable String postProcedureFormId,
            @RequestBody PostProcedureFormDTO dto) {

        Response response = postProcedureFormService.updatePostProcedureForm(hospitalId, postProcedureFormId, dto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // Delete
    @DeleteMapping("/delete-postprocedure-form/{hospitalId}/{postProcedureFormId}")
    public ResponseEntity<Response> deletePostProcedureForm(
            @PathVariable String hospitalId,
            @PathVariable String postProcedureFormId) {

        Response response = postProcedureFormService.deletePostProcedureForm(hospitalId, postProcedureFormId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
