package com.clinicadmin.service;

import org.springframework.http.ResponseEntity;
import com.clinicadmin.dto.ClinicCredentialsDTO;
import com.clinicadmin.dto.CustomerLoginDTO;
import com.clinicadmin.dto.DoctorLoginDTO;
import com.clinicadmin.dto.Response;


public interface AuthService {
	
public ResponseEntity<Response> cliniLogin(ClinicCredentialsDTO clinicCredentialsDTO);

public ResponseEntity<Response> doctorLogin(DoctorLoginDTO loginDTO);

public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);

public Response login(CustomerLoginDTO dto);

}
