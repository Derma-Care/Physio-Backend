package physiotherapydoctor.serviceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import physiotherapydoctor.dto.AccessTokenAndRefreshToken;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.service.AuthService;
import physiotherapydoctor.util.JwtUtil;
import physiotherapydoctor.util.RolesStore;

@Service
public class AuthServiceImpl implements AuthService {	
	
	@Autowired
	@Lazy
	private AuthenticationManager authManager;
	
	@Autowired
	public CustomDoctorLoginDetailsService customDoctorLoginDetailsService;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	public RolesStore rolesStore;

		
	public ResponseEntity<Response> doctorLogin( DoctorLoginDTO doctorLoginDTO) {
		//System.out.println("hlo");
				Response response = new Response();
				//System.out.println(response);
				customDoctorLoginDetailsService.deviceId = doctorLoginDTO.getDeviceId();
				try {			
					authManager.authenticate(new UsernamePasswordAuthenticationToken(doctorLoginDTO.getUsername(),doctorLoginDTO.getPassword()));
					//System.out.println("invoked after auth");
					List<String> roles = rolesStore.getRoles();					
					 String accessToken = jwtUtil.generateJwtToken(doctorLoginDTO.getUsername(),roles);	
					   String refreshToken = jwtUtil.generateRefreshToken(doctorLoginDTO.getUsername(),roles);
					   response.setMessage("Login successful");
					   response.setStatus(200);
					   AccessTokenAndRefreshToken tokens = new AccessTokenAndRefreshToken();
					   tokens.setAccessToken(accessToken);
		               tokens.setRefreshToken(refreshToken);
		               tokens.setAccessTokenExpireTime(jwtUtil.formattedTimeByZone);
					   response.setData(tokens);
					   	//System.out.println(tokens);
					   response.setSuccess(true);					  
				}catch(Exception e) {
					response.setMessage(e.getMessage());
			        response.setStatus(500);
			        response.setSuccess(false);		        
				}
				return ResponseEntity.status(response.getStatus()).body(response);
			}
	
	
	 public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken){
		   try {
			  Response res = jwtUtil.validateAndGenerateRefreshToken(refreshToken);
			  return ResponseEntity.status(res.getStatus()).body(res);
		   }catch(Exception e) {
			   Response respnse = new Response();
			   respnse.setMessage(e.getMessage());
			   respnse.setStatus(500);
			   return ResponseEntity.status(respnse.getStatus()).body(respnse);
		   }}
	 
}