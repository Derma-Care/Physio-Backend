package com.AdminService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.AdminService.repository.CredentialsRepository;


@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private CredentialsRepository adminRepository;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		//System.out.println(username);
		return adminRepository.findByUserName(username).orElseThrow(()->new RuntimeException("Object Not Found With Provided Name"));
	}
	
	
}
