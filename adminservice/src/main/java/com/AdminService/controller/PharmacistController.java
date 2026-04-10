package com.AdminService.controller;

import com.AdminService.dto.PharmacistDTO;
import com.AdminService.service.PharmacistService;
import com.AdminService.util.ResponseStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
////@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class PharmacistController {

    @Autowired
    private PharmacistService pharmacistService;

    // ------------------- Add Pharmacist -------------------
    @PostMapping("/addPharmacist")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(
            @Validated @RequestBody PharmacistDTO dto) {
        return pharmacistService.pharmacistOnBoarding(dto);
    }

    // ------------------- Get All Pharmacists by Hospital -------------------
    @GetMapping("/getAllPharmacists/{hospitalId}")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(@PathVariable String hospitalId) {
        return pharmacistService.getAllByDepartment(hospitalId);
    }

    // ------------------- Get Single Pharmacist -------------------
    @GetMapping("/getPharmacist/{pharmacistId}")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(@PathVariable String pharmacistId) {
        return pharmacistService.getPharmacist(pharmacistId);
    }

    // ------------------- Update Pharmacist -------------------
    @PutMapping("/updatePharmacist/{pharmacistId}")
    public ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(
            @PathVariable String pharmacistId,
            @RequestBody PharmacistDTO dto) {
        return pharmacistService.updatePharmacist(pharmacistId, dto);
    }

    // ------------------- Delete Pharmacist -------------------
    @DeleteMapping("/deletePharmacist/{pharmacistId}")
    public ResponseEntity<ResponseStructure<String>> deletePharmacist(@PathVariable String pharmacistId) {
        return pharmacistService.deletePharmacist(pharmacistId);
    }

    // ------------------- Get Pharmacists by Hospital and Branch -------------------
    @GetMapping("/getPharmacistsByHospitalIdAndBranchId/{hospitalId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(
            @PathVariable String hospitalId,
            @PathVariable String branchId) {
        return pharmacistService.getPharmacistsByHospitalIdAndBranchId(hospitalId, branchId);
    }
}
