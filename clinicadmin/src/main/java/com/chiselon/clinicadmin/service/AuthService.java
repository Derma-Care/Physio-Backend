package com.chiselon.clinicadmin.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import com.chiselon.clinicadmin.dto.ClinicCredentialsDTO;
import com.chiselon.clinicadmin.dto.LoginDTO;
import com.chiselon.clinicadmin.dto.Response;


public interface AuthService {
	
public ResponseEntity<Response> cliniLogin(ClinicCredentialsDTO clinicCredentialsDTO);

public ResponseEntity<Response> doctorLogin(Map<String,String> dto);

public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);

public Response login(Map<String,String>  dto);

public Response loginUsingRoles(LoginDTO dto);

}
