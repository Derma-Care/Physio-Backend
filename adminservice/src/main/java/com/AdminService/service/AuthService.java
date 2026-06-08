package com.AdminService.service;

import org.springframework.http.ResponseEntity;
import com.AdminService.dto.RegisterAndLoginDto;
import com.AdminService.util.Response;


public interface AuthService {
	
	public Response adminRegister(RegisterAndLoginDto helperAdmin);
		
	public Response adminLogin(String userName,String password);
	
	 public Response clinicLogin(String userName ); 
	 
	 public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);
	 
	
}
