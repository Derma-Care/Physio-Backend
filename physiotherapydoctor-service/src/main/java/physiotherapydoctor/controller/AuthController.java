package physiotherapydoctor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import physiotherapydoctor.dto.AccessTokenAndRefreshToken;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.AuthService;


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
			

