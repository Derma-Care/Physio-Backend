package com.chiselon.adminservice.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.adminservice.dto.BranchDTO;
import com.chiselon.adminservice.util.Response;

public interface BranchService {
	public Response createBranch(BranchDTO branch);
	ResponseEntity<?> getBranchById(String branchId);
	Response updateBranch(String branchId, BranchDTO branch);
	Response deleteBranch(String branchId);
	Response getAllBranches();
	 ResponseEntity<?> getBranchByClinicId(String clinicId);
	 Response getBranchesByClinicId(String clinicId);
	 ResponseEntity<?> getBranchByClinicAndBranchId(String clinicId, String branchId);
//	 public Response startBranchVerification(String branchId);
//	 public Response verifyBranch(String branchId);
//	public Response rejectBranch(String branchId, String reason);
     
		
}
