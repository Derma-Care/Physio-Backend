package com.clinicadmin.service;

import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.CustomerOnbordingDTO;
import com.clinicadmin.dto.Response;

public interface CustomerOnboardingService {
	Response onboardCustomer(CustomerOnbordingDTO dto);

	Response getAllCustomers();

	Response getCustomerById(String customerId);

	Response updateCustomer(String customerId, CustomerOnbordingDTO dto);

	Response deleteCustomer(String customerId);

	Response getCustomersByHospitalId(String hospitalId);

	Response getCustomersByBranchId(String branchId);

	Response getCustomersByHospitalIdAndBranchId(String hospitalId, String branchId);

	Response login(CustomerLoginDTO dto);
	
	public Response getCustomersByPatientId(String patientId,String clinicId);
	
	public CustomerOnbordingDTO getCustomerByToken(String token);
	public Response getCustomerByMobiileNumber(String mobilenumber);
		
//
//	Response resetPassword(ChangeDoctorPasswordDTO dto);
}
