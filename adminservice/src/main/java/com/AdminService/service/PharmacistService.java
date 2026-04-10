package com.AdminService.service;

import com.AdminService.dto.PharmacistDTO;
import com.AdminService.util.ResponseStructure;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PharmacistService {

    ResponseEntity<ResponseStructure<PharmacistDTO>> pharmacistOnBoarding(PharmacistDTO dto);

    ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getAllByDepartment(String hospitalId);

    ResponseEntity<ResponseStructure<PharmacistDTO>> getPharmacist(String pharmacistId);

    ResponseEntity<ResponseStructure<PharmacistDTO>> updatePharmacist(String pharmacistId, PharmacistDTO dto);

    ResponseEntity<ResponseStructure<String>> deletePharmacist(String pharmacistId);

    ResponseEntity<ResponseStructure<List<PharmacistDTO>>> getPharmacistsByHospitalIdAndBranchId(String hospitalId, String branchId);
}
