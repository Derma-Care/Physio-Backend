package physiotherapydoctor.service;

import org.springframework.http.ResponseEntity;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.Response;

public interface AuthService {
	
public ResponseEntity<Response> doctorLogin( DoctorLoginDTO doctorLoginDTO);
public ResponseEntity<Response> requestForNewJwtTokenByRefreshToken(String refreshToken);


}
