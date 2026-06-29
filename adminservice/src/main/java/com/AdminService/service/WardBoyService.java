package com.AdminService.service;

import com.AdminService.dto.WardBoyDTO;
import com.AdminService.util.ResponseStructure;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface WardBoyService {

    ResponseEntity<ResponseStructure<WardBoyDTO>> addWardBoy(WardBoyDTO dto);

    ResponseEntity<ResponseStructure<WardBoyDTO>> updateWardBoy(String id, WardBoyDTO dto);

    ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyById(String id);

    ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getAllWardBoys();

    ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicId(String clinicId);

    ResponseEntity<ResponseStructure<WardBoyDTO>> getWardBoyByIdAndClinicId(String wardBoyId, String clinicId);

    ResponseEntity<ResponseStructure<Void>> deleteWardBoy(String id);

    ResponseEntity<ResponseStructure<List<WardBoyDTO>>> getWardBoysByClinicIdAndBranchId(String clinicId, String branchId);
}
