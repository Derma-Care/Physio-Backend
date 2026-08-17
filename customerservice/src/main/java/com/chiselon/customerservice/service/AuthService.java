package com.chiselon.customerservice.service;

import org.springframework.http.ResponseEntity;

import com.chiselon.customerservice.util.Response;


public interface AuthService {
	
	public ResponseEntity<?> customerLogin(String userName, String password,String deviceId);
	public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);
	

}
