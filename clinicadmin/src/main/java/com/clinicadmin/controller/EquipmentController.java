package com.clinicadmin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clinicadmin.dto.EquipmentDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.EquipmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinic-admin")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService service;

    @PostMapping("/createEquipment")
    public ResponseEntity<Response> createEquipment(
            @RequestBody EquipmentDTO dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createEquipment(dto));
    }

    @GetMapping("/getEquipmentById/{equipmentId}")
    public ResponseEntity<Response> getEquipmentById(
            @PathVariable String equipmentId) {

        return ResponseEntity.ok(
                service.getEquipmentById(equipmentId));
    }

    @GetMapping("/getAllEquipment")
    public ResponseEntity<Response> getAllEquipment() {

        return ResponseEntity.ok(
                service.getAllEquipment());
    }

    @GetMapping("/getEquipmentByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<Response> getEquipmentByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {

        return ResponseEntity.ok(
                service.getEquipmentByClinicIdAndBranchId(
                        clinicId,
                        branchId));
    }

    @PutMapping("/updateEquipmentByEquipmentId/{equipmentId}")
    public ResponseEntity<Response> updateEquipment(
            @PathVariable String equipmentId,
            @RequestBody EquipmentDTO dto) {

        return ResponseEntity.ok(
                service.updateEquipment(
                        equipmentId,
                        dto));
    }

    @DeleteMapping("/deleteEquipmentByEquipmentId/{equipmentId}")
    public ResponseEntity<Response> deleteEquipment(
            @PathVariable String equipmentId) {

        return ResponseEntity.ok(
                service.deleteEquipment(equipmentId));
    }
}