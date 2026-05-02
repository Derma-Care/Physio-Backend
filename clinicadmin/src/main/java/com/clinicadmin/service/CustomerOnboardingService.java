package com.clinicadmin.service;

import java.util.List;
import java.util.Map;

import com.clinicadmin.dto.BookingInfoByInput;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.CustomerOnbordingDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.CustomerOnbording;

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
	public Map<String,String> getCustomerByMobilenumberAndName(String mobilenumber,String name);	
	public CustomerOnbordingDTO getCustomerByMobileNumberAndClinicId(String mobilenumber,String clinicId);
	//public  List<CustomerOnbordingDTO> getCustomerByNameAndClinicId(String name,String clinicId);	
	public List<BookingInfoByInput> bookingByInput(String input,String clinicId);
			
			
//
//	Response resetPassword(ChangeDoctorPasswordDTO dto);
}
