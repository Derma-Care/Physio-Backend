package com.clinicadmin.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import com.clinicadmin.dto.ClinicCredentialsDTO;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.DoctorLoginDTO;
import com.clinicadmin.dto.Response;


public interface AuthService {
	
public ResponseEntity<Response> cliniLogin(ClinicCredentialsDTO clinicCredentialsDTO);

public ResponseEntity<Response> doctorLogin(Map<String,String> dto);

public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);

public Response login(Map<String,String>  dto);

public Response loginUsingRoles(DoctorLoginDTO dto);

}
