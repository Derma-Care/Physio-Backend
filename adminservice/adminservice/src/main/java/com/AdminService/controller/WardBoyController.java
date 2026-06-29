package com.AdminService.controller;

import com.AdminService.dto.WardBoyDTO;
import com.AdminService.service.WardBoyService;
import com.AdminService.util.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class WardBoyController {

    private final WardBoyService wardBoyService;

    @PostMapping("/addWardBoy")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> addWardBoy(@RequestBody WardBoyDTO dto) {
        return wardBoyService.addWardBoy(dto);
    }

    @PutMapping("/updateWardBoy/{id}")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> updateWardBoy(@PathVariable String id, @RequestBody WardBoyDTO dto) {
        return wardBoyService.updateWardBoy(id, dto);
    }

    @GetMapping("/getWardBoyById/{id}")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyById(@PathVariable String id) {
        return wardBoyService.getWardBoyById(id);
    }

    @GetMapping("/getAllWardBoys")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getAllWardBoys() {
        return wardBoyService.getAllWardBoys();
    }

    @GetMapping("/getWardBoysByClinicId/{clinicId}")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicId(@PathVariable String clinicId) {
        return wardBoyService.getWardBoysByClinicId(clinicId);
    }

    @GetMapping("/getWardBoyByIdAndClinicId/{wardBoyId}/{clinicId}")
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyByIdAndClinicId(
            @PathVariable String wardBoyId,
            @PathVariable String clinicId) {
        return wardBoyService.getWardBoyByIdAndClinicId(wardBoyId, clinicId);
    }

    @DeleteMapping("/deleteWardBoy/{id}")
    public ResponseEntity<ResponseStructure<Void>> deleteWardBoy(@PathVariable String id) {
        return wardBoyService.deleteWardBoy(id);
    }

    @GetMapping("/getWardBoysByClinicIdAndBranchId/{clinicId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(
            @PathVariable String clinicId,
            @PathVariable String branchId) {
        return wardBoyService.getWardBoysByClinicIdAndBranchId(clinicId, branchId);
    }
}
