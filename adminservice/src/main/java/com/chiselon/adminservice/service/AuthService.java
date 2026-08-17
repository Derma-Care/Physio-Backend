package com.chiselon.adminservice.service;

import org.springframework.http.ResponseEntity;
import com.chiselon.adminservice.dto.RegisterAndLoginDto;
import com.chiselon.adminservice.util.Response;


public interface AuthService {
	
	public Response adminRegister(RegisterAndLoginDto helperAdmin);
		
	public Response adminLogin(String userName,String password);
	
	 public Response clinicLogin(String userName ); 
	 
	 public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);
	 
	
}
