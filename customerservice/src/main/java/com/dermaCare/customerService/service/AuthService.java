package com.dermaCare.customerService.service;

import org.springframework.http.ResponseEntity;

import com.dermaCare.customerService.util.Response;


public interface AuthService {
	
	public ResponseEntity<?> customerLogin(String userName, String password,String deviceId);
	public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);
	

}
