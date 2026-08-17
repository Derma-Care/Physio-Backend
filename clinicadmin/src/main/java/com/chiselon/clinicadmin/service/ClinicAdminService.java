package com.chiselon.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.clinicadmin.dto.ClinicDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;

public interface ClinicAdminService {

	///public Response login(ClinicLoginRequestDTO credentials);
	public Response updateClinicCredentials(UpdateClinicLoginCredentialsDTO updatedCredentials,String userName);
	public Response getClinicById(String hospitalId);
	public Response updateClinic(String hospitalId, ClinicDTO dto);
	public Response deleteClinic(String hospitalId);
	ResponseEntity<?> getBranchesByClinicId(String clinicId);

	  public String getDeviceId(String clinicId,String branchId);
		   
	 public Response getStaffInfo(String hospitalId, String branchId);
	
}
