package com.chiselon.customerservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chiselon.customerservice.dto.AccessTokenAndRefreshToken;
import com.chiselon.customerservice.dto.CustomerLoginDTO;
import com.chiselon.customerservice.service.AuthService;
import com.chiselon.customerservice.util.Response;


@RestController
@RequestMapping("/customer")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	 @PostMapping("/customerHospitalLogin")
	   public ResponseEntity<?> customerLogin(@RequestBody CustomerLoginDTO dto){
		   return authService.customerLogin(dto.getUsername(), dto.getPassword(),dto.getDeviceId());
	 }
	 
	 @PostMapping("/newAccessTokenForClinicAdminService")
		public  ResponseEntity<Response> requestForNewAccessTokenByRefreshToken(@RequestBody AccessTokenAndRefreshToken accessTokenAndRefreshToken ){
			return authService.requestForNewJwtTokenByRefreshToken(accessTokenAndRefreshToken.getRefreshToken());
		}
}
