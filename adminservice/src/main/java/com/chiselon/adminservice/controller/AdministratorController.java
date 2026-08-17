package com.chiselon.adminservice.controller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.chiselon.adminservice.dto.AdministratorDTO;
import com.chiselon.adminservice.service.AdministratorService;
import com.chiselon.adminservice.util.ResponseStructure;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
//@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class AdministratorController {

    private final AdministratorService administratorService;

    // ➕ Add Administrator
    @PostMapping("/addAdministrator")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> addAdministrator(
            @RequestBody AdministratorDTO dto) {
        
        ResponseStructure<AdministratorDTO> response = administratorService.addAdministrator(dto);

        // ✅ Prevent HttpStatus null
        HttpStatus status = response.getHttpStatus() != null ? response.getHttpStatus() : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }

    // 📋 Get All Administrators by Clinic
    @GetMapping("/getAllAdministrators/{clinicId}")
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinic(
            @PathVariable String clinicId) {
        return administratorService.getAllAdministratorsByClinic(clinicId);
    }

    // 📋 Get All Administrators by Clinic and Branch
    @GetMapping("/getAllAdministratorsByClinicIdBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinicAndBranch(
            @PathVariable String clinicId,
            @PathVariable String branchId) {
        return administratorService.getAllAdministratorsByClinicAndBranch(clinicId, branchId);
    }

    // 🔍 Get Administrator by Clinic and Admin ID
    @GetMapping("/getAdministratorByClinicIdAdminId/{clinicId}/{adminId}")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicAndId(
            @PathVariable String clinicId,
            @PathVariable String adminId) {
        return administratorService.getAdministratorByClinicAndId(clinicId, adminId);
    }

    // 🔍 Get Administrator by Clinic, Branch, and Admin ID
    @GetMapping("/getAdministratorByclinicIdBranchIdAdminId/{clinicId}/{branchId}/{adminId}")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicBranchAndAdminId(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String adminId) {
        return administratorService.getAdministratorByClinicBranchAndAdminId(clinicId, branchId, adminId);
    }

    // ✏️ Update Administrator by Clinic and Admin ID
    @PutMapping("/updateAdministratorByClinicIdAdminId/{clinicId}/{adminId}")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministrator(
            @PathVariable String clinicId,
            @PathVariable String adminId,
            @RequestBody AdministratorDTO dto) {
        return administratorService.updateAdministrator(clinicId, adminId, dto);
    }

    // ✏️ Update Administrator by Clinic, Branch, and Admin ID
    @PutMapping("/updateAdministratorByClinicIdBranchIdAdminId/{clinicId}/{branchId}/{adminId}")
    public ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministratorUsingClinicBranchAndAdminId(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String adminId,
            @RequestBody AdministratorDTO dto) {
        return administratorService.updateAdministratorUsingClinicBranchAndAdminId(clinicId, branchId, adminId, dto);
    }

    // ❌ Delete Administrator by Clinic and Admin ID
    @DeleteMapping("/deleteAdministratorByClinicIdAdminId/{clinicId}/{adminId}")
    public ResponseEntity<ResponseStructure<String>> deleteAdministrator(
            @PathVariable String clinicId,
            @PathVariable String adminId) {
        return administratorService.deleteAdministrator(clinicId, adminId);
    }

    // ❌ Delete Administrator by Clinic, Branch, and Admin ID
    @DeleteMapping("/deleteAdministratorByClinicIdBranchIdAdminId/{clinicId}/{branchId}/{adminId}")
    public ResponseEntity<ResponseStructure<String>> deleteAdministratorUsingClinicBranchAndAdminId(
            @PathVariable String clinicId,
            @PathVariable String branchId,
            @PathVariable String adminId) {
        return administratorService.deleteAdministratorUsingClinicBranchAndAdminId(clinicId, branchId, adminId);
    }
}