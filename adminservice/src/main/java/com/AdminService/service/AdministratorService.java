package com.AdminService.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.AdminService.dto.AdministratorDTO;
import com.AdminService.util.ResponseStructure;

public interface AdministratorService {

    ResponseStructure addAdministrator(AdministratorDTO dto);

    ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinic(String clinicId);

    ResponseEntity<ResponseStructure<List<AdministratorDTO>>> getAllAdministratorsByClinicAndBranch(String clinicId, String branchId);

    ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicAndId(String clinicId, String adminId);

    ResponseEntity<ResponseStructure<AdministratorDTO>> getAdministratorByClinicBranchAndAdminId(String clinicId, String branchId, String adminId);

    ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministrator(String clinicId, String adminId, AdministratorDTO dto);

    ResponseEntity<ResponseStructure<AdministratorDTO>> updateAdministratorUsingClinicBranchAndAdminId(String clinicId, String branchId, String adminId, AdministratorDTO dto);

    ResponseEntity<ResponseStructure<String>> deleteAdministrator(String clinicId, String adminId);

    ResponseEntity<ResponseStructure<String>> deleteAdministratorUsingClinicBranchAndAdminId(String clinicId, String branchId, String adminId);
}