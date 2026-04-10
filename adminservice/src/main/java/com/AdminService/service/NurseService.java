package com.AdminService.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.AdminService.dto.NurseDTO;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

public interface NurseService {

	
	public ResponseEntity<Response> nurseOnBoarding(NurseDTO dto);

    ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllByHospital(String hospitalId);

    ResponseEntity<ResponseStructure<NurseDTO>> getNurse(String hospitalId, String nurseId);

    ResponseEntity<ResponseStructure<NurseDTO>> updateNurse(String nurseId, NurseDTO dto);

    ResponseEntity<ResponseStructure<String>> deleteNurse(String hospitalId, String nurseId);

    ResponseEntity<ResponseStructure<List<NurseDTO>>> getAllNursesByBranch(String hospitalId, String branchId);
}
