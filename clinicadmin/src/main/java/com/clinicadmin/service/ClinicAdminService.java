package com.clinicadmin.service;

import org.springframework.http.ResponseEntity;

import com.clinicadmin.dto.ClinicDTO;
import com.clinicadmin.dto.ClinicLoginRequestDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;

public interface ClinicAdminService {

	public Response login(ClinicLoginRequestDTO credentials);
	public Response updateClinicCredentials(UpdateClinicLoginCredentialsDTO updatedCredentials,String userName);
	public Response getClinicById(String hospitalId);
	public Response updateClinic(String hospitalId, ClinicDTO dto);
	public Response deleteClinic(String hospitalId);
	ResponseEntity<?> getBranchesByClinicId(String clinicId);
}
