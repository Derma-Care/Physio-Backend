package com.dermaCare.customerService.service;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.dermaCare.customerService.dto.CustomerLoginDTO;
import com.dermaCare.customerService.feignClient.ClinicAdminFeign;
import com.dermaCare.customerService.util.Response;
import com.dermaCare.customerService.util.RolesStore;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CustomCustomerDetailsService implements UserDetailsService{
	
	@Autowired
	public ClinicAdminFeign clinicAdminFeign;
	
	@Autowired
	@Lazy
	public RolesStore rolesStore;
	
	public String deviceId;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		CustomerLoginDTO dto = new CustomerLoginDTO();
		dto.setUserName(username);
		dto.setDeviceId(deviceId);
		 Response res = clinicAdminFeign.login(dto).getBody();
		 if(res.getData()!=null) {
			 CustomerLoginDTO customerLoginDTO = new ObjectMapper().convertValue(res, CustomerLoginDTO.class);
			 rolesStore.setRoles(dto.getRoles()); 
			 return customerLoginDTO;
		 }else {
			 return null;
		 }
	}

}
