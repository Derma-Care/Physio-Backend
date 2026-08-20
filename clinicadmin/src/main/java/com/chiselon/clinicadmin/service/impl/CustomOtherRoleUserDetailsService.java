package com.chiselon.clinicadmin.service.impl;

import java.util.Collections;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.chiselon.clinicadmin.dto.LoginDTO;
import com.chiselon.clinicadmin.repository.LoginRepository;
import com.chiselon.clinicadmin.utils.ClinicRelatedInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CustomOtherRoleUserDetailsService implements UserDetailsService{
	
	@Autowired
	private LoginRepository doctorLoginCredentialsRepository;
	
	@Autowired
	@Lazy
	public ClinicRelatedInfo rolesStore;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {		
		LoginDTO doctorDto = new ObjectMapper().convertValue(doctorLoginCredentialsRepository.findByUsername(username).get(), LoginDTO.class);
		doctorDto.setRoles(Collections.singletonList(doctorDto.getRole()));
		rolesStore.setDoctorLoginDTO(doctorDto);
		return doctorDto;		
	}
	
}
