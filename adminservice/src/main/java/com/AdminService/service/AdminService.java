package com.AdminService.service;

import java.util.List;
import org.springframework.http.ResponseEntity;
import com.AdminService.dto.AdminHelper;
import com.AdminService.dto.CategoryDto;
import com.AdminService.dto.ClinicCredentialsDTO;
import com.AdminService.dto.ClinicDTO;
import com.AdminService.dto.CustomerDTO;
import com.AdminService.dto.ServicesDto;
import com.AdminService.dto.SubServicesDto;
import com.AdminService.dto.SubServicesInfoDto;
import com.AdminService.dto.UpdateClinicCredentials;
import com.AdminService.util.Response;
import com.AdminService.util.ResponseStructure;

public interface AdminService {

//ADMIN
	
public Response adminRegister(AdminHelper helperAdmin);
	
public Response adminLogin(String userName,String password);
	
//CLINIC MANAGEMENT
public Response createClinic(ClinicDTO clinic);
Response getClinicById(String clinicId);
public Response getAllClinics();
Response updateClinic(String clinicId, ClinicDTO clinic);
Response deleteClinic(String clinicId);

////================= CLINIC VERIFICATION FLOW =================
//
////Start verification (Admin action)
//Response startVerificationProcess(String clinicId);
//
////Verify clinic (Approve)
//Response verifyClinic(String clinicId);
//
////Reject clinic
//Response rejectClinic(String clinicId, String reason);

//CLINIC CREDENTIALS
public Response getClinicCredentials(String userName);

public Response updateClinicCredentials(UpdateClinicCredentials credentials,String userName) ;

public Response deleteClinicCredentials(String userName );

public Response login(ClinicCredentialsDTO credentials);

//category
public Response addNewCategory(CategoryDto dto);

public Response getAllCategory();

public Response deleteCategoryById(
		 String categoryId);

public Response updateCategory(String categoryId,CategoryDto updatedCategory);
public Response getCategoryById(String CategoryId);

//SERVICE MANAGEMENT
public Response addService( ServicesDto dto);
public Response getServiceById( String categoryId);
public Response getServiceByServiceId( String serviceId);
public Response deleteService( String serviceId);
public Response updateByServiceId( String serviceId,
	 ServicesDto domainServices);
public Response getAllServices();

//SUBSERVICE MANAGEMENT
public  Response addSubService( SubServicesInfoDto dto);
public Response getSubServiceByIdCategory(String categoryId);
public Response getSubServicesByServiceId(String serviceId);
public Response getSubServiceBySubServiceId(String subServiceId);
public Response deleteSubService(String subServiceId);
public Response updateBySubServiceId(String subServiceId, SubServicesInfoDto domainServices);
public Response getAllSubServices();

//CUSTOMER MANAGEMENT
public Response saveCustomerBasicDetails(CustomerDTO customerDTO );
public ResponseEntity<?> getCustomerByUsernameMobileEmail(String input);
public Response getCustomerBasicDetails(String mobileNumber );
public Response getAllCustomers();
public Response updateCustomerBasicDetails(CustomerDTO customerDTO,String mobileNumber );
public Response deleteCustomerBasicDetails(String mobileNumber);

//SUBSERVICES
public Response getAllSubServicesFromClincAdmin();

//BOOKINGS


//DOCTORS
public Response getDoctorInfoByDoctorId(String doctorId);

public Response getClinicsByRecommondation();

Response getAllRecommendClinicThenAnotherClincs();

//SUBSERVICES DETAILS

ResponseEntity<ResponseStructure<SubServicesDto>> addService(String subServiceId, SubServicesDto dto);


ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId(String subServiceId);

ResponseEntity<ResponseStructure<SubServicesDto>> deleteSubService(String hospitalId, String subServiceId);

ResponseEntity<ResponseStructure<SubServicesDto>> updateBySubServiceId(String hospitalId, String serviceId,
		SubServicesDto domainServices);
ResponseEntity<ResponseStructure<SubServicesDto>> getSubServiceByServiceId( String hospitalId, String subServiceId);

ResponseEntity<ResponseStructure<List<SubServicesDto>>> getSubServiceByHospitalId(String hospitalId);

Response rejectClinic(String clinicId, String reason);

Response verifyClinic(String clinicId);

Response startVerificationProcess(String clinicId);


}



