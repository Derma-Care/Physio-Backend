package com.dermacare.doctorservice.serviceimpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.dermacare.doctorservice.dto.ChangeDoctorPasswordDTO;
import com.dermacare.doctorservice.dto.DoctorAvailabilityStatusDTO;
import com.dermacare.doctorservice.dto.DoctorLoginDTO;
import com.dermacare.doctorservice.dto.Response;
import com.dermacare.doctorservice.feignclient.BookingFeignClient;
import com.dermacare.doctorservice.feignclient.ClinicAdminServiceClient;
import com.dermacare.doctorservice.service.DoctorService;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private final ClinicAdminServiceClient clinicAdminServiceClient;
    
    @Autowired
    private BookingFeignClient bookingFeignClient;
    
    private ObjectMapper objectMapper;

    private Response validateChangePasswordRequest(String username, ChangeDoctorPasswordDTO updateDTO) {
        if (username == null || username.isBlank()) {
       return Response.builder().success(false).status(400).message("Username must not be empty").build();
        }

        if (updateDTO == null) {
        return Response.builder().success(false).status(400) .message("Request body is missing").build();
        }

        if (updateDTO.getCurrentPassword() == null || updateDTO.getCurrentPassword().isBlank()) {
            return Response.builder().success(false).status(400).message("Current password must not be empty").build();
        }

        if (updateDTO.getNewPassword() == null || updateDTO.getNewPassword().isBlank()) {
          return Response.builder().success(false).status(400).message("New password must not be empty").build();
        }

        if (updateDTO.getConfirmPassword() == null || updateDTO.getConfirmPassword().isBlank()) {
         return Response.builder().success(false).status(400).message("Confirm password must not be empty").build();
        }

        if (!updateDTO.getNewPassword().equals(updateDTO.getConfirmPassword())) {
         return Response.builder().success(false).status(400).message("New password and confirm password do not match").build();
        }

        if (updateDTO.getNewPassword().length() < 6) {
         return Response.builder().success(false).status(400).message("Password must be at least 6 characters").build();
        }

        return null; 
    }

    @Override
    public Response changePassword(String username, ChangeDoctorPasswordDTO updateDTO) {
        Response validationResponse = validateChangePasswordRequest(username, updateDTO);
        if (validationResponse != null) {
            return validationResponse;
        }

        try {
            
            return clinicAdminServiceClient.changePassword(username, updateDTO);

        } catch (Exception ex) {
   
            return Response.builder().success(false).status(500).message("Failed to change password " ).build();
        }
    }


    @Override
    public Response login(DoctorLoginDTO loginDTO) {
    	try {
    	Response response=clinicAdminServiceClient.login(loginDTO);
    	return response;
    	}catch (FeignException fe) {
		    try {
		        String errorJson = fe.contentUTF8(); 
		        Response errorResponse = objectMapper.readValue(errorJson, Response.class);
                Response response = new Response();
		        response.setSuccess(false);
		        response.setData(null);
		        response.setMessage(errorResponse.getMessage()); 
		        response.setStatus(errorResponse.getStatus());   
		        return response;
		    } catch (Exception ex) {
		    	Response response = new Response();
		        response.setSuccess(false);
		        response.setData(null);
		        response.setMessage("Admin Service error: " + fe.getMessage());
		        response.setStatus(fe.status());
		        return response;
		        }
    	}
    }
	@Override
	public Response updateDoctorAvailability(String doctorId, DoctorAvailabilityStatusDTO availabilityDTO) {
	    if(doctorId==null || doctorId.isBlank()) {
	    	return Response.builder().success(false).status(400) .message("Doctor ID must not be empty").build();
	    }

    	if(availabilityDTO == null) {
    		return Response.builder().success(false).status(400).message("Availability status is missing").build();
    	}
    	try {
    		return clinicAdminServiceClient.updateDoctorAvailability(doctorId, availabilityDTO);
    	}
    	catch (Exception ex) {
			return Response.builder().success(false).status(500).message("Failed to update doctor availability status").build();
					
		}
    }
	
	
	///NEW DOCTOR APIS
	
			public ResponseEntity<?> getAllDoctors(){
				try {
				return clinicAdminServiceClient.getAllDoctors();
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			
			public ResponseEntity<?> getDoctorById(String id){
				try {
				return clinicAdminServiceClient.getDoctorById(id);
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			
			public ResponseEntity<?> getDoctorByClinicAndDoctorId(String clinicId,
					String doctorId){
				try {
				return clinicAdminServiceClient.getDoctorByClinicAndDoctorId(clinicId, doctorId);
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			public ResponseEntity<?> getDoctorsByHospitalById(String clinicId){
				try {
				return clinicAdminServiceClient.getDoctorsByHospitalById(clinicId);
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			
			public ResponseEntity<?> getDoctorsBySubServiceId(String hsptlId,String subServiceId){
				try {
				return clinicAdminServiceClient.getDoctorsBySubServiceId(hsptlId, subServiceId);
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			
			public ResponseEntity<?> getAllDoctorsBySubServiceId(String subServiceId){
				try {
				return clinicAdminServiceClient.getAllDoctorsBySubServiceId(subServiceId);
				}catch(Exception e) {
					return ResponseEntity.status(500).body(e.getMessage());
				}
			}
			
			 public ResponseEntity<?> getDoctorFutureAppointments(String doctorId) {
			        try {
			          
			            return bookingFeignClient.getDoctorFutureAppointments(doctorId);
			        } catch (Exception ex) {
			            
			            if (ex instanceof feign.FeignException feignEx) {
			                return ResponseEntity.status(feignEx.status()).body(feignEx.contentUTF8());
			            }
			            return ResponseEntity.status(500).body(ex.getMessage());
			        }
			    }
			 @Override
			    public ResponseEntity<Response> getDiseasesFromClinicAdmin(String hospitalId) {

			       
			        return clinicAdminServiceClient.getDiseasesByHospitalId(hospitalId);
			    }
			 
			  @Override
			    public ResponseEntity<Response> getLabTestsFromClinicAdmin(String hospitalId) {
			        return clinicAdminServiceClient.getLabTestsByHospitalId(hospitalId);
			    }
}

