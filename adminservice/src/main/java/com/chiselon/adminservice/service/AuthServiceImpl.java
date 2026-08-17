package com.chiselon.adminservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.chiselon.adminservice.dto.AccessTokenAndRefreshToken;
import com.chiselon.adminservice.dto.ClinicCredentialsDTO;
import com.chiselon.adminservice.dto.RegisterAndLoginDto;
import com.chiselon.adminservice.entity.ClinicCredentials;
import com.chiselon.adminservice.entity.RegisterAndLoginEntity;
import com.chiselon.adminservice.repository.ClinicCredentialsRepository;
import com.chiselon.adminservice.repository.CredentialsRepository;
import com.chiselon.adminservice.util.JwtUtil;
import com.chiselon.adminservice.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AuthServiceImpl implements AuthService {
	
	@Autowired
	private CredentialsRepository adminRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private JwtUtil jwtUtil;
	
    @Autowired
	private ClinicCredentialsRepository clinicCredentialsRepository;
	
	private String refreshToken;
	
	@Override
	public Response adminRegister(RegisterAndLoginDto helperAdmin) {
		Response response = new Response();
	try {
		Optional<RegisterAndLoginEntity> userName = adminRepository.findByUserName(helperAdmin.getUserName());
		   RegisterAndLoginEntity mobileNumber = adminRepository.findByMobileNumber(helperAdmin.getMobileNumber());
		   if(mobileNumber != null ) {
			   response.setMessage("MobileNumber is Already Exist");
		        response.setStatus(409);
		        response.setSuccess(false);
		        return response;}
		        if(userName.isPresent()) {
		        	response.setMessage("UserName already exist");
			        response.setStatus(409);
			        response.setSuccess(false);
			        return response;
		        	}else {
		        	RegisterAndLoginEntity entityAdmin = new RegisterAndLoginEntity();
		 		    entityAdmin.setUserName(helperAdmin.getUserName());		 		    
		 		    entityAdmin.setPassword(passwordEncoder.encode(helperAdmin.getPassword()));
		 		    entityAdmin.setMobileNumber(helperAdmin.getMobileNumber());
		 		    List<String> roles = new ArrayList<>();
		 		    roles.add("ROLE_ADMIN");
		 		   entityAdmin.setRoles(roles);
		        adminRepository.save(entityAdmin);
		        response.setMessage("Credentials Are saved successfully");
		        response.setStatus(200);
		        response.setSuccess(true);
		        return response;
		}}catch(Exception e) {
		response.setMessage(e.getMessage());
        response.setStatus(500);
        response.setSuccess(false);
        return response;
	}
	}
	
	@Override
	public Response adminLogin(String userName, String password) {
		//System.out.println("hlo");
		Response response = new Response();
		//System.out.println(response);
		try {			
			authManager.authenticate(new UsernamePasswordAuthenticationToken(userName,password));
			//System.out.println("invoked after auth");
			List<String> roles = new ArrayList<>();
			roles.add("ROLE_ADMIN");
			 String accessToken = jwtUtil.generateJwtToken(userName,roles);	
			   refreshToken = jwtUtil.generateRefreshToken(userName,roles);
			   response.setMessage("Login successful");
			   response.setStatus(200);
			   AccessTokenAndRefreshToken tokens = new AccessTokenAndRefreshToken();
			   tokens.setAccessToken(accessToken);
               tokens.setRefreshToken(refreshToken);
               tokens.setAccessTokenExpireTime(jwtUtil.formattedTimeByZone);
			   response.setData(tokens);
			   	//System.out.println(tokens);
			   response.setSuccess(true);
			   return response;	
		}catch(Exception e) {
			response.setMessage(e.getMessage());
	        response.setStatus(500);
	        response.setSuccess(false);
	        return response;
		}
		
	}
		
	 @Override
	    public Response clinicLogin(String userName ) {
	    	Response response = new Response();
	    	try {
	    	ClinicCredentials existUserName = clinicCredentialsRepository.findByUserName(userName);
	    	///System.out.println(existUserName);
	    		if(existUserName != null) {
	    			response.setSuccess(true);
	    			response.setData(new ObjectMapper().convertValue(existUserName,ClinicCredentialsDTO.class ));
	        		response.setMessage("Valid Credentials");
	        		response.setStatus(200);}	       
	    	//System.out.println(response);
	    	}catch(Exception e){
	    		response.setSuccess(false);
	    		response.setMessage(e.getMessage());
	    		response.setStatus(500);
	    	}
	    	return response;
	    }	
	 
	 public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken){
		   try {
			  Response res = jwtUtil.validateAndGenerateRefreshToken(refreshToken);
			  return ResponseEntity.status(res.getStatus()).body(res);
		   }catch(Exception e) {
			   Response respnse = new Response();
			   respnse.setMessage(e.getMessage());
			   respnse.setStatus(500);
			   return ResponseEntity.status(respnse.getStatus()).body(respnse);
		   }}
	 
}
