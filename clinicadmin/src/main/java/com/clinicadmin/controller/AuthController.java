package com.clinicadmin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.clinicadmin.dto.AccessTokenAndRefreshToken;
import com.clinicadmin.dto.ClinicCredentialsDTO;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.DoctorLoginDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.service.AuthService;


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
	public ResponseEntity<Response> doctorLogin(@RequestBody DoctorLoginDTO loginDTO) {
		return authService.doctorLogin(loginDTO);
		
	}
	
	 @PostMapping("/newAccessTokenForClinicAdminService")
		public  ResponseEntity<Response> requestForNewAccessTokenByRefreshToken(@RequestBody AccessTokenAndRefreshToken accessTokenAndRefreshToken ){
			return authService.requestForNewJwtTokenByRefreshToken(accessTokenAndRefreshToken.getRefreshToken());
		}
	 
	 @PostMapping("/customers/login")
	    public ResponseEntity<Response> login(@RequestBody CustomerLoginDTO dto) {
	        Response response = authService.login(dto);
	        return ResponseEntity.status(response.getStatus()).body(response);
	    }
			
}
