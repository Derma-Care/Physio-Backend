package com.chiselon.clinicadmin.service.impl;

import java.util.Collections;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.chiselon.clinicadmin.dto.DoctorLoginDTO;
import com.chiselon.clinicadmin.repository.DoctorLoginCredentialsRepository;
import com.chiselon.clinicadmin.utils.ClinicRelatedInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CustomOtherRoleUserDetailsService implements UserDetailsService{
	
	@Autowired
	private DoctorLoginCredentialsRepository doctorLoginCredentialsRepository;
	
	@Autowired
	@Lazy
	public ClinicRelatedInfo rolesStore;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {		
		DoctorLoginDTO doctorDto = new ObjectMapper().convertValue(doctorLoginCredentialsRepository.findByUsername(username).get(), DoctorLoginDTO.class);
		doctorDto.setRoles(Collections.singletonList(doctorDto.getRole()));
		rolesStore.setDoctorLoginDTO(doctorDto);
		return doctorDto;		
	}
	
}
