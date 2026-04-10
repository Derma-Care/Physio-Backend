package com.AdminService.controller;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.AdminService.dto.LabTechnicianRequestDTO;
import com.AdminService.service.LabTechnicianService;
import com.AdminService.util.ResponseStructure;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class LabTechnicianController {

    private final LabTechnicianService labTechnicianService;

    // ✅ Add Lab Technician
    @PostMapping("/addLabTechnician")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> addLabTechnician(
            @Valid @RequestBody LabTechnicianRequestDTO dto) {
        return labTechnicianService.addLabTechnician(dto);
    }

    // ✅ Get All Lab Technicians
    @GetMapping("/getAllLabTechnicians")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getAllLabTechnicians() {
        return labTechnicianService.getAllLabTechnicians();
    }

    // ✅ Update Lab Technician By Id
    @PutMapping("/updateById/{id}")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> updateLabTechnicianById(
            @PathVariable String id,
            @Valid @RequestBody LabTechnicianRequestDTO dto) {
        return labTechnicianService.updateLabTechnicianById(id, dto);
    }

    // ✅ Delete Lab Technician By Id
    @DeleteMapping("/deleteById/{id}")
    public ResponseEntity<ResponseStructure<String>> deleteLabTechnicianById(@PathVariable String id) {
        return labTechnicianService.deleteLabTechnicianById(id);
    }

    // ✅ Get Lab Technicians By Clinic Id
    @GetMapping("/getLabTechniciansByClinicById/{clinicId}")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinic(
            @PathVariable String clinicId) {
        return labTechnicianService.getLabTechniciansByClinic(clinicId);
    }

    // ✅ Get Lab Technician By Clinic Id & Technician Id
    @GetMapping("/getLabTechnicianByIdAndClinicId/{clinicId}/{technicianId}")
    public ResponseEntity<ResponseStructure<LabTechnicianRequestDTO>> getLabTechnicianByClinicAndId(
            @PathVariable String clinicId,
            @PathVariable String technicianId) {
        return labTechnicianService.getLabTechnicianByClinicAndId(clinicId, technicianId);
    }

    // ✅ Get Lab Technicians By Clinic Id & Branch Id
    @GetMapping("/getLabTechniciansByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<LabTechnicianRequestDTO>>> getLabTechniciansByClinicAndBranch(
            @PathVariable String clinicId,
            @PathVariable String branchId) {
        return labTechnicianService.getLabTechniciansByClinicAndBranch(clinicId, branchId);
    }
}
