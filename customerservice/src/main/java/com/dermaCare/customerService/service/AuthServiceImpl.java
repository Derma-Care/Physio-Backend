package com.dermaCare.customerService.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.dto.AccessTokenAndRefreshToken;
import com.dermaCare.customerService.util.JwtUtil;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.RolesStore;


@Service
public class AuthServiceImpl implements AuthService {	
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	CustomCustomerDetailsService customCustomerDetailsService;
	
	@Autowired
	public RolesStore rolesStore;
	
	
	@Override
	public ResponseEntity<?> customerLogin(String userName, String password,String deviceId) {
		//System.out.println("hlo");
				Response response = new Response();
				//System.out.println(response);
				customCustomerDetailsService.deviceId = deviceId;
				try {			
					authManager.authenticate(new UsernamePasswordAuthenticationToken(userName,password));
					//System.out.println("invoked after auth");
					List<String> roles = new ArrayList<>();
					roles = rolesStore.getRoles();
					 String accessToken = jwtUtil.generateJwtToken(userName,roles);	
					   String refreshToken = jwtUtil.generateRefreshToken(userName,roles);
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