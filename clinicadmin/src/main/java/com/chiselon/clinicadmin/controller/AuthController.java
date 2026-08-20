package com.chiselon.clinicadmin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chiselon.clinicadmin.dto.AccessTokenAndRefreshToken;
import com.chiselon.clinicadmin.dto.ClinicCredentialsDTO;
import com.chiselon.clinicadmin.dto.LoginDTO;
import com.chiselon.clinicadmin.dto.Response;
import com.chiselon.clinicadmin.service.AuthService;


@RestController
@RequestMapping("/clinic-admin")
public class AuthController {
	
	@Autowired
	private AuthService authService;
	
	@PostMapping("/clinicLogin")
	public ResponseEntity<Response> clinicLogin(@RequestBody ClinicCredentialsDTO clinicCredentialsDTO ){
		//System.out.println(clinicCredentialsDTO);
		return authService.cliniLogin(clinicCredentialsDTO);
	}
	
	
	@PostMapping("/doctorLogin")
	public ResponseEntity<Response> doctorLogin(@RequestBody Map<String,String> dto) {
		return authService.doctorLogin(dto);
		
	}
	
	 @PostMapping("/newAccessTokenForClinicAdminService")
		public  ResponseEntity<Response> requestForNewAccessTokenByRefreshToken(@RequestBody AccessTokenAndRefreshToken accessTokenAndRefreshToken ){
			return authService.requestForNewJwtTokenByRefreshToken(accessTokenAndRefreshToken.getRefreshToken());
		}
	 
	 @PostMapping("/customers/login")
	    public ResponseEntity<Response> login(@RequestBody Map<String,String> dto) {
	        Response response = authService.login(dto);
	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
	 
		@PostMapping("/loginUsingRoles")
		public ResponseEntity<Response> loginUsingRoles(@RequestBody LoginDTO dto){
			  Response response = authService.loginUsingRoles(dto);
			    return ResponseEntity.status(response.getStatus()).body(response);
			
		}
		
			
}
