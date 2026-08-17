package com.chiselon.physiotherapydoctor.serviceImpl;

import java.util.List;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.chiselon.physiotherapydoctor.dto.AccessTokenAndRefreshToken;
import com.chiselon.physiotherapydoctor.dto.DoctorLoginDTO;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.service.AuthService;
import com.chiselon.physiotherapydoctor.util.JwtUtil;
import com.chiselon.physiotherapydoctor.util.RolesStore;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {	
	
	@Autowired
	@Lazy
	private AuthenticationManager authManager;
	
	@Autowired
	public CustomDoctorLoginDetailsService customDoctorLoginDetailsService;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	public RolesStore rolesStore;

		
	  public ResponseEntity<Response> doctorLogin(DoctorLoginDTO doctorLoginDTO) {

	        log.info("Doctor login request received for username: {}", doctorLoginDTO.getUsername());

	        Response response = new Response();
	        customDoctorLoginDetailsService.deviceId = doctorLoginDTO.getDeviceId();

	        log.debug("Device ID set for authentication. Username: {}, DeviceId: {}",
	                doctorLoginDTO.getUsername(),
	                doctorLoginDTO.getDeviceId());

	        try {

	            log.info("Authenticating doctor: {}", doctorLoginDTO.getUsername());

	            authManager.authenticate(
	                    new UsernamePasswordAuthenticationToken(
	                            doctorLoginDTO.getUsername(),
	                            doctorLoginDTO.getPassword()));

	            log.info("Authentication successful for doctor: {}", doctorLoginDTO.getUsername());

	            List<String> roles = rolesStore.getRoles();

	            log.debug("Roles fetched for doctor {} : {}", doctorLoginDTO.getUsername(), roles);

	            String accessToken =
	                    jwtUtil.generateJwtToken(doctorLoginDTO.getUsername(), roles);

	            String refreshToken =
	                    jwtUtil.generateRefreshToken(doctorLoginDTO.getUsername(), roles);

	            log.info("JWT access token and refresh token generated successfully for doctor: {}",
	                    doctorLoginDTO.getUsername());

	            Response responseData = response;

	            responseData.setMessage("Login successful");
	            responseData.setStatus(200);

	            AccessTokenAndRefreshToken tokens = new AccessTokenAndRefreshToken();
	            tokens.setAccessToken(accessToken);
	            tokens.setRefreshToken(refreshToken);
	            tokens.setAccessTokenExpireTime(jwtUtil.formattedTimeByZone);

	            responseData.setData(tokens);
	            responseData.setSuccess(true);

	            log.info("Doctor login completed successfully for username: {}",
	                    doctorLoginDTO.getUsername());

	            log.debug("Access token expiry time: {}", jwtUtil.formattedTimeByZone);

	        } catch (Exception e) {

	            log.error("Doctor login failed for username: {}. Error: {}",
	                    doctorLoginDTO.getUsername(),
	                    e.getMessage(),
	                    e);

	            response.setMessage(e.getMessage());
	            response.setStatus(500);
	            response.setSuccess(false);
	        }

	        log.info("Returning login response with status: {}", response.getStatus());

	        return ResponseEntity.status(response.getStatus()).body(response);
	    }

	    public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken) {

	        log.info("Refresh token request received");

	        try {

	            log.debug("Validating refresh token");

	            Response response = jwtUtil.validateAndGenerateRefreshToken(refreshToken);

	            log.info("New JWT token generated successfully using refresh token");

	            return ResponseEntity.status(response.getStatus()).body(response);

	        } catch (Exception e) {

	            log.error("Failed to generate JWT token using refresh token. Error: {}",
	                    e.getMessage(),
	                    e);

	            Response response = new Response();
	            response.setMessage(e.getMessage());
	            response.setStatus(500);

	            return ResponseEntity.status(response.getStatus()).body(response);
	        }
	    }
	 
}