package com.chiselon.customerservice.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.chiselon.customerservice.dto.CustomerLoginDTO;
import com.chiselon.customerservice.util.FeignImpl;
import com.chiselon.customerservice.util.Response;
import com.chiselon.customerservice.util.RolesStore;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CustomCustomerDetailsService implements UserDetailsService{
	
	@Autowired
	public FeignImpl clinicAdminFeign;
	
	@Autowired
	@Lazy
	public RolesStore rolesStore;
	
	public String deviceId;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		 Map<String,String> credentials = new LinkedHashMap<>();
		 credentials.put("username",username);
		 credentials.put("deviceId",deviceId);
		 Response res = clinicAdminFeign.login(credentials).getBody();
		/// System.out.println(res);
		 if(res.getData()!=null) {
			 CustomerLoginDTO customerLoginDTO = new ObjectMapper().convertValue(res.getData(), CustomerLoginDTO.class);
			 rolesStore.setRoles(customerLoginDTO.getRoles()); 
			 ///System.out.println(customerLoginDTO);
			 return customerLoginDTO;
		 }else {
			 return null;
		 }
	}

}
