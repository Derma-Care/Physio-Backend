package com.clinicadmin.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.AccessTokenAndRefreshToken;
import com.clinicadmin.dto.ClinicCredentialsDTO;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.DoctorLoginDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.ClinicAdminDeviceTokenEntity;
import com.clinicadmin.entity.CustomerCredentials;
import com.clinicadmin.entity.CustomerOnbording;
import com.clinicadmin.entity.DoctorLoginCredentials;
import com.clinicadmin.entity.Doctors;
import com.clinicadmin.repository.ClinicAdminWebFcmTokenRepository;
import com.clinicadmin.repository.CustomerCredentialsRepository;
import com.clinicadmin.repository.CustomerOnboardingRepository;
import com.clinicadmin.repository.DoctorLoginCredentialsRepository;
import com.clinicadmin.repository.DoctorsRepository;
import com.clinicadmin.service.AuthService;
import com.clinicadmin.utils.ClinicRelatedInfo;
import com.clinicadmin.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {	
	
	@Autowired
	@Qualifier("customAuthenticationManager")
	private AuthenticationManager authManager;
	
	@Autowired
	@Qualifier("customAuthenticationManageForOtherRoles")
	private AuthenticationManager otherRoles;
		
	@Autowired
	private JwtUtil jwtUtil;
	
	
	@Autowired
	private DoctorLoginCredentialsRepository credentialsRepository;
	
	@Autowired
	private ClinicAdminWebFcmTokenRepository clinicAdminWebFcmTokenRepository;
	
	@Autowired
	private CustomerCredentialsRepository customerCredentialsRepository;

	@Autowired
	public ClinicRelatedInfo rolesStore;
			
	
	@Override
	public ResponseEntity<Response> cliniLogin(ClinicCredentialsDTO clinicCredentialsDTO) {
		//System.out.println("hlo");
				Response response = new Response();
				//System.out.println(response);
				try {			
					authManager.authenticate(new UsernamePasswordAuthenticationToken(clinicCredentialsDTO.getUsername(),clinicCredentialsDTO.getPassword()));
					//System.out.println("invoked after auth");
					List<String> roles = rolesStore.getRoles();	
					//System.out.println(rolesStore);
					 String accessToken = jwtUtil.generateJwtToken(clinicCredentialsDTO.getUsername(),roles);	
					  String refreshToken = jwtUtil.generateRefreshToken(clinicCredentialsDTO.getUsername(),roles);
					   response.setMessage("Login successful");
					   response.setStatus(200);
					   response.setPermissions(rolesStore.getPermissions());
					  // System.out.println(rolesStore.getPermissions());
					   AccessTokenAndRefreshToken tokens = new AccessTokenAndRefreshToken();
					   tokens.setAccessToken(accessToken);
		               tokens.setRefreshToken(refreshToken);
		               tokens.setAccessTokenExpireTime(jwtUtil.formattedTimeByZone);
					   response.setData(tokens);
					   	//System.out.println(tokens);
					   response.setSuccess(true);
					   ClinicAdminDeviceTokenEntity c = new ClinicAdminDeviceTokenEntity();
					  Optional<ClinicAdminDeviceTokenEntity> obj = clinicAdminWebFcmTokenRepository.findByUsername(clinicCredentialsDTO.getUsername());
					   if(obj.isPresent()) {
						   if(clinicCredentialsDTO.getClinicAdminWebFcmToken() != null) {
							   if(!clinicCredentialsDTO.getClinicAdminWebFcmToken().equals( obj.get().getClinicAdminWebFcmToken())) {
							   obj.get().setClinicAdminWebFcmToken(clinicCredentialsDTO.getClinicAdminWebFcmToken());
							   clinicAdminWebFcmTokenRepository.save( obj.get());}}
					   }else{
					  c.setUsername(clinicCredentialsDTO.getUsername());
					   if(clinicCredentialsDTO.getClinicAdminWebFcmToken() != null) {
					   c.setClinicAdminWebFcmToken(clinicCredentialsDTO.getClinicAdminWebFcmToken());}
					   clinicAdminWebFcmTokenRepository.save(c);}
					   return ResponseEntity.status(response.getStatus()).body(response);	
				}catch(Exception e) {
					response.setMessage(e.getMessage());
			        response.setStatus(500);
			        response.setSuccess(false);
			        return ResponseEntity.status(response.getStatus()).body(response);	
					
				}
			}
	
	
	@Override
	public ResponseEntity<Response> doctorLogin( Map<String,String> dto) {
		Response responseDTO = new Response();
        try {
        	///System.out.println(dto);
		Optional<DoctorLoginCredentials> credentialsOptional = credentialsRepository
				.findByUsername(dto.get("username"));
		//System.out.println(credentialsOptional);
		if (credentialsOptional.isPresent()) {
			DoctorLoginCredentials credentials = credentialsOptional.get();	
			credentials.setDeviceId(dto.get("deviceId"));
			credentialsRepository.save(credentials);
			DoctorLoginDTO  cred = new ObjectMapper().convertValue(credentials, DoctorLoginDTO.class);
			cred.setRoles(Collections.singletonList(cred.getRole()));
			cred.setUserName(credentials.getUsername());
			 responseDTO.setData(cred);
  				//System.out.println(responseDTO.getData());
				responseDTO.setStatus(HttpStatus.OK.value());
				responseDTO.setMessage("Login successful");
				responseDTO.setSuccess(true);
			}else{
				responseDTO.setData(null);
				responseDTO.setStatus(HttpStatus.NOT_FOUND.value());
				responseDTO.setMessage("Invalid Credentials");
				responseDTO.setSuccess(false);
			}
		}catch(Exception e) {
			responseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			responseDTO.setMessage(e.getMessage());
			responseDTO.setSuccess(false);
		}

		return ResponseEntity.status(responseDTO.getStatus()).body(responseDTO);
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
	 
	 @Override
		public Response login(Map<String,String> dto) {
			Response response = new Response();
       ///System.out.println(dto);
			try {
				Optional<CustomerCredentials> optional = customerCredentialsRepository.findByUserName(dto.get("username"));
				if (optional.isEmpty()) {
					response.setSuccess(false);
					response.setMessage("Invalid username");
					response.setStatus(200);
					return response;}
		      if (!optional.isEmpty()) {
		    	  CustomerCredentials customer = optional.get();
					customer.setDeviceId(dto.get("deviceId"));
					customerCredentialsRepository.save(customer);}				
				CustomerLoginDTO resDTO = new CustomerLoginDTO();
				resDTO.setUserName(optional.get().getUserName());
				resDTO.setPassword(optional.get().getPassword());
				resDTO.setRoles(Collections.singletonList("ROLE_CUSTOMER"));
				// final response
				response.setSuccess(true);
				response.setMessage("Login successful");
				response.setData(resDTO);
				response.setStatus(200);
			} catch (Exception e) {
				response.setSuccess(false);
				response.setMessage("Login error: " + e.getMessage());
				response.setStatus(500);
			}

			return response;
		}
	 
	 @Override
		public Response loginUsingRoles(DoctorLoginDTO dto) {
			Response response = new Response();
			DoctorLoginDTO resDto = rolesStore.getDoctorLoginDTO();
			try {
				otherRoles.authenticate(new UsernamePasswordAuthenticationToken(dto.getUsername(),dto.getPassword()));
				//System.out.println("invoked after auth");
				List<String> roles = resDto.getRoles();	
				//System.out.println(rolesStore);
				 String accessToken = jwtUtil.generateJwtToken(dto.getUsername(),roles);	
				  String refreshToken = jwtUtil.generateRefreshToken(dto.getUsername(),roles);
				   AccessTokenAndRefreshToken tokens = new AccessTokenAndRefreshToken();
				   tokens.setAccessToken(accessToken);
	               tokens.setRefreshToken(refreshToken);
	               tokens.setPermissions(resDto.getPermissions());
	               tokens.setAccessTokenExpireTime(jwtUtil.formattedTimeByZone);
	               response.setMessage("Login successful");
				   response.setStatus(200);
				   response.setSuccess(true);
					response.setData(tokens);	
			} catch (Exception e) {
				response.setSuccess(false);
				response.setMessage("Login error: " + e.getMessage());
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			}
	
			return response;
		}
}