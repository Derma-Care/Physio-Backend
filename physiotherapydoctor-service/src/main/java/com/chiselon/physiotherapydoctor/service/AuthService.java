package com.chiselon.physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;
import com.chiselon.physiotherapydoctor.dto.DoctorLoginDTO;
import com.chiselon.physiotherapydoctor.dto.Response;

public interface AuthService {
	
public ResponseEntity<Response> doctorLogin( DoctorLoginDTO doctorLoginDTO);
public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);


}
