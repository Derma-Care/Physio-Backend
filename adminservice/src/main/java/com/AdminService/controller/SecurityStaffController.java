package com.AdminService.controller;

import com.AdminService.dto.SecurityStaffDTO;
import com.AdminService.service.SecurityStaffService;
import com.AdminService.util.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SecurityStaffController {

    private final SecurityStaffService securityStaffService;

    // ------------------- Add Security Staff -------------------
    @PostMapping("/addSecurityStaff")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> addSecurityStaff(@RequestBody SecurityStaffDTO dto) {
        return securityStaffService.addSecurityStaff(dto);
    }

    // ------------------- Update Security Staff -------------------
    @PutMapping("/updateSecurityStaff/{staffId}")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> updateSecurityStaff(
            @PathVariable String staffId,
            @RequestBody SecurityStaffDTO dto) {
        return securityStaffService.updateSecurityStaff(staffId, dto);
    }

    // ------------------- Get Security Staff by ID -------------------
    @GetMapping("/getSecurityStaffById/{staffId}")
    public ResponseEntity<ResponseStructure<SecurityStaffDTO>> getSecurityStaffById(@PathVariable String staffId) {
        return securityStaffService.getSecurityStaffById(staffId);
    }

    // ------------------- Get All Security Staff by Clinic ID -------------------
    @GetMapping("/getAllSecurityStaffByClinicId/{clinicId}")
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getAllByClinicId(@PathVariable String clinicId) {
        return securityStaffService.getAllByClinicId(clinicId);
    }

    // ------------------- Delete Security Staff -------------------
    @DeleteMapping("/deleteSecurityStaff/{staffId}")
    public ResponseEntity<ResponseStructure<String>> deleteSecurityStaff(@PathVariable String staffId) {
        return securityStaffService.deleteSecurityStaff(staffId);
    }

    // ------------------- Get Security Staff by Clinic ID & Branch ID -------------------
    @GetMapping("/getSecurityStaffByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getSecurityStaffByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {
        return securityStaffService.getSecurityStaffByClinicIdAndBranchId(clinicId, branchId);
    }
}
