package com.clinicadmin.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.ClinicDTO;
import com.clinicadmin.dto.ClinicLoginRequestDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.dto.UpdateClinicLoginCredentialsDTO;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.service.ClinicAdminService;
import com.clinicadmin.utils.ExtractFeignMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
@Service
public class ClinicAdminServiceImpl implements ClinicAdminService {
    @Autowired
    private AdminServiceClient adminServiceClient;

    @Autowired
    private ObjectMapper objectMapper;
  

    @Override
    public Response login(ClinicLoginRequestDTO credentials) {
    	try {
    	Response response=adminServiceClient.login(credentials);
    	return response;
    	}catch(FeignException e) {
    	Response res = new Response();
    	res.setStatus(e.status());
    	res.setMessage(ExtractFeignMessage.clearMessage(e));
    	res.setSuccess(false);
       return res;
       }
    }

    @Override
    public Response updateClinicCredentials(UpdateClinicLoginCredentialsDTO updatedCredentials, String userName) {
    	try {
        	Response response=adminServiceClient.updateClinicCredentials(updatedCredentials, userName);
        	return response;
        	}catch(FeignException e) {
        	Response res = new Response();
        	res.setStatus(e.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(e));
        	res.setSuccess(false);
           return res;}
        }

    @Override
    public Response getClinicById(String hospitalId) {
    	try {
        	ResponseEntity<Response> response=adminServiceClient.getClinicById(hospitalId);
        	return response.getBody();
        	}catch(FeignException e) {
        	Response res = new Response();
        	res.setStatus(e.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(e));
        	res.setSuccess(false);
           return res;
           }
        }

    @Override
    public Response updateClinic(String hospitalId, ClinicDTO dto) {
    	try {
        	Response response=adminServiceClient.updateClinic(hospitalId, dto);
        	return response;
        	}catch(FeignException e) {
        	Response res = new Response();
        	res.setStatus(e.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(e));
        	res.setSuccess(false);
           return res;}
        }

    @Override
    public Response deleteClinic(String hospitalId) {
    	try {
        	Response response=adminServiceClient.deleteClinic(hospitalId);
        	return response;
        	}catch(FeignException e) {
        	Response res = new Response();
        	res.setStatus(e.status());
        	res.setMessage(ExtractFeignMessage.clearMessage(e));
        	res.setSuccess(false);
           return res;
           
        	
        	
        	}
        }


    @Override
    public ResponseEntity<?> getBranchesByClinicId(String clinicId) {
        try {
          
            return adminServiceClient.getBranchByClinicId(clinicId);

        } catch (FeignException e) {
            try {
                String errorJson = e.contentUTF8();
                Response response = objectMapper.readValue(errorJson, Response.class);

  
                return ResponseEntity.status(e.status()).body(response);

            } catch (Exception ex) {
                Response fallback = new Response();
                fallback.setSuccess(false);
                fallback.setMessage("Error parsing AdminService response");
                fallback.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallback);
            }
        }
    }
}
