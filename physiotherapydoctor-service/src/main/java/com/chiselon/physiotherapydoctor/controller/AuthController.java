package com.chiselon.physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.physiotherapydoctor.dto.AccessTokenAndRefreshToken;
import com.chiselon.physiotherapydoctor.dto.DoctorLoginDTO;
import com.chiselon.physiotherapydoctor.dto.Response;
import com.chiselon.physiotherapydoctor.service.AuthService;


@RestController
@RequestMapping("/physiotherapy-doctor")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	@PostMapping("/doctorLogin")
	public ResponseEntity<Response> doctorLogin(@RequestBody DoctorLoginDTO doctorLoginDTO) {
		return authService.doctorLogin(doctorLoginDTO);}
	
		
	@PostMapping("/newAccessTokenForClinicAdminService")
	public  ResponseEntity<Response> requestForNewAccessTokenByRefreshToken(@RequestBody AccessTokenAndRefreshToken accessTokenAndRefreshToken ){
		return authService.requestForNewJwtTokenByRefreshToken(accessTokenAndRefreshToken.getRefreshToken());
	}
		
	}
			

