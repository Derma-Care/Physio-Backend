package com.AdminService.controller;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.AdminService.dto.NurseDTO;
import com.AdminService.service.NurseService;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class NurseController {

    private final NurseService nurseService;

    // ✅ Add Nurse
    @PostMapping("/addNurse")
    public ResponseEntity<Response> addNurse(@RequestBody NurseDTO dto) {
        return nurseService.nurseOnBoarding(dto);
    }

    // ✅ Get All Nurses by Hospital
    @GetMapping("/getAllNurses/{hospitalId}")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(@PathVariable String hospitalId) {
        return nurseService.getAllByHospital(hospitalId);
    }

    // ✅ Get Nurse by Hospital and Nurse ID
    @GetMapping("/getNurse/{hospitalId}/{nurseId}")
    public ResponseEntity<ResponseStructure<NurseDTO>> getNurse(
            @PathVariable String hospitalId,
            @PathVariable String nurseId) {
        return nurseService.getNurse(hospitalId, nurseId);
    }

    // ✅ Update Nurse
    @PutMapping("/updateNurse/{nurseId}")
    public ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(
            @PathVariable String nurseId,
            @RequestBody NurseDTO dto) {
        return nurseService.updateNurse(nurseId, dto);
    }

    // ✅ Delete Nurse
    @DeleteMapping("/deleteNurse/{hospitalId}/{nurseId}")
    public ResponseEntity<ResponseStructure<String>> deleteNurse(
            @PathVariable String hospitalId,
            @PathVariable String nurseId) {
        return nurseService.deleteNurse(hospitalId, nurseId);
    }

    // ✅ Get All Nurses by Hospital and Branch
    @GetMapping("/getAllNursesByBranch/{hospitalId}/{branchId}")
    public ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(
            @PathVariable String hospitalId,
            @PathVariable String branchId) {
        return nurseService.getAllNursesByBranch(hospitalId, branchId);
    }
}
