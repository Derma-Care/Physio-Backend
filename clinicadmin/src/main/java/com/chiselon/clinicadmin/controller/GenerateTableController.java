package com.chiselon.clinicadmin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chiselon.clinicadmin.dto.PhysiotherapyRecordDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.service.GenerateTableService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class GenerateTableController {

    private final GenerateTableService service;

    @PostMapping("/generate-table")
    public ResponseEntity<Response> generateTable(
            @RequestBody PhysiotherapyRecordDTO request) {

        Response response = service.generateTable(request);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }
}