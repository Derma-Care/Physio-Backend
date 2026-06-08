package com.clinicadmin.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class AuthServiceImpl implements AuthService {	
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private DoctorsRepository doctorsRepository;
	
	@Autowired
	private DoctorLoginCredentialsRepository credentialsRepository;
	
	@Autowired
	private ClinicAdminWebFcmTokenRepository clinicAdminWebFcmTokenRepository;
	
	@Autowired
	private CustomerOnboardingRepository onboardingRepository;

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
					System.out.println(rolesStore);
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
					   ClinicAdminDeviceTokenEntity obj = clinicAdminWebFcmTokenRepository.findByUsername(clinicCredentialsDTO.getUsername());
					   if(obj != null) {
						   if(clinicCredentialsDTO.getClinicAdminWebFcmToken() != null) {
							   obj.setClinicAdminWebFcmToken(clinicCredentialsDTO.getClinicAdminWebFcmToken());
							   clinicAdminWebFcmTokenRepository.save(obj);} 
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
	public ResponseEntity<Response> doctorLogin(DoctorLoginDTO loginDTO) {
		Response responseDTO = new Response();
        try {
		Optional<DoctorLoginCredentials> credentialsOptional = credentialsRepository
				.findByUsername(loginDTO.getUserName());
		//System.out.println(credentialsOptional);
		Doctors doctors = doctorsRepository.findByDoctorName(loginDTO.getUserName()).get();
		doctors.setDeviceId(loginDTO.getDeviceId());
		if (credentialsOptional.isPresent()) {
			doctorsRepository.save(doctors);
			DoctorLoginCredentials credentials = credentialsOptional.get();		
  				responseDTO.setData(new ObjectMapper().convertValue(credentials, DoctorLoginDTO.class));
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
		public Response login(CustomerLoginDTO dto) {
			Response response = new Response();

			try {
				Optional<CustomerCredentials> optional = customerCredentialsRepository.findByUserName(dto.getUserName());
				if (optional.isEmpty()) {
					response.setSuccess(false);
					response.setMessage("Invalid username");
					response.setStatus(404);
					return response;}
				Optional<CustomerOnbording> customerOpt = onboardingRepository.findByCustomerId(dto.getUserName());
				if (!customerOpt.isEmpty()) {
					CustomerOnbording customer = customerOpt.get();
					customer.setDeviceId(dto.getDeviceId());
					CustomerOnbording cs = onboardingRepository.save(customer);}	
				
				CustomerLoginDTO resDTO = new CustomerLoginDTO();
				resDTO.setUserName(optional.get().getUserName());
				resDTO.setDeviceId(optional.get().getPassword());
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
}