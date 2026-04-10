package com.AdminService.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.AdminService.dto.WardBoyDTO;
import com.AdminService.feign.ClinicAdminFeign;
import com.AdminService.util.ExtractFeignMessage;
import com.AdminService.util.ResponseStructure;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WardBoyServiceImpl implements WardBoyService {

    private final ClinicAdminFeign clinicAdminFeign;

    // ✅ Add WardBoy
    @Override
    public ResponseEntity<ResponseStructure<WardBoyDTO>> addWardBoy(WardBoyDTO dto) {
        try {
            ResponseStructure<WardBoyDTO> response = clinicAdminFeign.addWardBoy(dto);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Update WardBoy
    @Override
    public ResponseEntity<ResponseStructure<WardBoyDTO>> updateWardBoy(String id, WardBoyDTO dto) {
        try {
            ResponseStructure<WardBoyDTO> response = clinicAdminFeign.updateWardBoy(id, dto);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoy by ID
    @Override
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyById(String id) {
        try {
            ResponseStructure<WardBoyDTO> response = clinicAdminFeign.getWardBoyById(id);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get All WardBoys
    @Override
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getAllWardBoys() {
        try {
            ResponseStructure<List<WardBoyDTO>> response = clinicAdminFeign.getAllWardBoys();
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoys by Clinic ID
    @Override
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicId(String clinicId) {
        try {
            ResponseStructure<List<WardBoyDTO>> response = clinicAdminFeign.getWardBoysByClinicId(clinicId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoy by ID and Clinic ID
    @Override
    public ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyByIdAndClinicId(String wardBoyId, String clinicId) {
        try {
            ResponseStructure<WardBoyDTO> response = clinicAdminFeign.getWardBoyByIdAndClinicId(wardBoyId, clinicId);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<WardBoyDTO> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Delete WardBoy
    @Override
    public ResponseEntity<ResponseStructure<Void>> deleteWardBoy(String id) {
        try {
            ResponseStructure<Void> response = clinicAdminFeign.deleteWardBoy(id);
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (FeignException e) {
            ResponseStructure<Void> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }

    // ✅ Get WardBoys by ClinicId + BranchId (already returns ResponseEntity)
    @Override
    public ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(
            String clinicId, String branchId) {
        try {
            return clinicAdminFeign.getWardBoysByClinicIdAndBranchId(clinicId, branchId);
        } catch (FeignException e) {
            ResponseStructure<List<WardBoyDTO>> res = new ResponseStructure<>(
                    null,
                    ExtractFeignMessage.clearMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.status()
            );
            return ResponseEntity.status(res.getStatusCode()).body(res);
        }
    }
}
