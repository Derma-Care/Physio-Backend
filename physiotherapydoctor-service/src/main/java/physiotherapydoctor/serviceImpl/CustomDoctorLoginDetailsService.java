package physiotherapydoctor.serviceImpl;

import java.util.LinkedHashMap;
import java.util.Map;

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
import physiotherapydoctor.util.FeignImpl;
import physiotherapydoctor.util.RolesStore;


@Service
public class CustomDoctorLoginDetailsService implements UserDetailsService {
	
	@Autowired
	private FeignImpl clinicAdminFeign;
	
	@Autowired
	@Lazy
	public RolesStore rolesStore;
	
	public String deviceId;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if(deviceId == null) {
			deviceId = " ";}
		 Map<String,String> credentials = new LinkedHashMap<>();
		 credentials.put("username",username);
		 credentials.put("deviceId",deviceId);
		 //System.out.println(credentials);
		  Response res = clinicAdminFeign.doctorLogin(credentials).getBody();
		  //System.out.println(res);
		  if(res.getData()!=null) {
			  DoctorLoginDTO dto = new ObjectMapper().convertValue(res.getData(),DoctorLoginDTO.class );
			  rolesStore.setRoles(dto.getRoles()); 
			  	//System.out.println(dto);
			  return dto;
		  }else {
			  return null;
		  }}
	}
