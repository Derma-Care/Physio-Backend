package com.clinicadmin.service.impl;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.clinicadmin.dto.ClinicCredentialsDTO;
import com.clinicadmin.dto.Response;
import com.clinicadmin.feignclient.AdminServiceClient;
import com.clinicadmin.utils.ClinicRelatedInfo;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component
public class CustomClinicAdminLoginDetailsService implements UserDetailsService {

	@Autowired
	private AdminServiceClient adminServiceClient;
	
	@Autowired
	@Lazy
	public ClinicRelatedInfo rolesStore;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		 Response response = adminServiceClient.clinicLogin(username);
		 //System.out.println(response);
		 ClinicCredentialsDTO credentials = new ObjectMapper().convertValue(response.getData(),ClinicCredentialsDTO.class);
		 //System.out.println(credentials);
		 rolesStore.setRoles(credentials.getRoles());
		 rolesStore.setPermissions(credentials.getPermissions());
		 return credentials;}
	}
