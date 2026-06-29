package com.AdminService.service;


import com.AdminService.dto.SecurityStaffDTO;
import com.AdminService.util.ResponseStructure;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface SecurityStaffService {

    ResponseEntity<ResponseStructure<SecurityStaffDTO>> addSecurityStaff(SecurityStaffDTO dto);

    ResponseEntity<ResponseStructure<SecurityStaffDTO>> updateSecurityStaff(String staffId, SecurityStaffDTO dto);

    ResponseEntity<ResponseStructure<SecurityStaffDTO>> getSecurityStaffById(String staffId);

    ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getAllByClinicId(String clinicId);

    ResponseEntity<ResponseStructure<String>> deleteSecurityStaff(String staffId);

    ResponseEntity<ResponseStructure<List<SecurityStaffDTO>>> getSecurityStaffByClinicIdAndBranchId(String clinicId, String branchId);
}
