package com.dermaCare.customerService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dermaCare.customerService.dto.AccessTokenAndRefreshToken;
import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.service.AuthService;
import com.dermaCare.customerService.util.Response;


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
