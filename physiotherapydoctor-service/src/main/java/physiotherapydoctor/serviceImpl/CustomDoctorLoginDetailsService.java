package physiotherapydoctor.serviceImpl;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import physiotherapydoctor.dto.DoctorLoginDTO;
import physiotherapydoctor.dto.Response;
import physiotherapydoctor.feign.ClinicAdminFeign;
import physiotherapydoctor.util.RolesStore;


@Service
public class CustomDoctorLoginDetailsService implements UserDetailsService {
	
	@Autowired
	private ClinicAdminFeign clinicAdminFeign;
	
	@Autowired
	@Lazy
	public RolesStore rolesStore;
	
	public String deviceId;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		DoctorLoginDTO doctorLoginDTO = new DoctorLoginDTO();
		doctorLoginDTO.setUserName(username);
		doctorLoginDTO.setDeviceId(deviceId);
		  Response res = clinicAdminFeign.doctorLogin(doctorLoginDTO).getBody();
		  if(res.getData()!=null) {
			  DoctorLoginDTO dto = new ObjectMapper().convertValue(res,DoctorLoginDTO.class );
			  rolesStore.setRoles(dto.getRoles()); 
			  return dto;
		  }else {
			  return null;
		  }}
	}
