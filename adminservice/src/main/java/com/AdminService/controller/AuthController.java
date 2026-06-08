package com.AdminService.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.AdminService.dto.AccessTokenAndRefreshToken;
import com.AdminService.dto.RegisterAndLoginDto;
import com.AdminService.service.AuthService;
import com.AdminService.util.Response;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	
	@PostMapping("/adminRegister")
	private ResponseEntity<?> adminRegister(@RequestBody @Valid RegisterAndLoginDto helperAdmin) {
		 Response response = authService.adminRegister(helperAdmin);
		 if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;}
}
	
	@PostMapping("/adminLogin")
	public ResponseEntity<?> adminLogin(@RequestBody RegisterAndLoginDto helperAdmin) {
		//System.out.println("hiii");
		 Response response = authService.adminLogin(helperAdmin.getUserName(), helperAdmin.getPassword());
		 if(response != null && response.getStatus() != 0) {
			 return ResponseEntity.status(response.getStatus()).body(response);
		 }else {
				return null;}
		
    } 
    
    @PostMapping("/clinicLogin/{userName}")
    public Response clinicLogin(@PathVariable String userName) {
    Response response = authService.clinicLogin(userName);
   /// System.out.println(response);
    return response;
    }
    
    
    @PostMapping("/requestForNewAccessTokenByRefreshTokenForAdmin")
	public  ResponseEntity<Response> requestForNewAccessTokenByRefreshToken(@RequestBody AccessTokenAndRefreshToken accessTokenAndRefreshToken ){
		return authService.requestForNewJwtTokenByRefreshToken(accessTokenAndRefreshToken.getRefreshToken());
	}
            
}
