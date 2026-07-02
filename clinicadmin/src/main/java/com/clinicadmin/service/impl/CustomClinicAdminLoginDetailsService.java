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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomClinicAdminLoginDetailsService implements UserDetailsService {

	@Autowired
	private AdminServiceClient adminServiceClient;
	
	@Autowired
	@Lazy
	public ClinicRelatedInfo rolesStore;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

	    log.info("Authentication request received for username: {}", username);

	    try {
	        log.info("Calling Admin Service to fetch clinic credentials for username: {}", username);

	        Response response = adminServiceClient.clinicLogin(username);

	        log.debug("Response received from Admin Service: {}", response);

	        if (response == null || response.getData() == null) {
	            log.error("No clinic credentials found for username: {}", username);
	            throw new UsernameNotFoundException("User not found: " + username);
	        }

	        ClinicCredentialsDTO credentials =
	                new ObjectMapper().convertValue(response.getData(), ClinicCredentialsDTO.class);

	        log.info("Clinic credentials fetched successfully for username: {}", username);
	        log.debug("Roles: {}", credentials.getRoles());
	        log.debug("Permissions: {}", credentials.getPermissions());

	        rolesStore.setRoles(credentials.getRoles());
	        rolesStore.setPermissions(credentials.getPermissions());

	        log.info("Roles and permissions stored successfully for username: {}", username);

	        return credentials;

	    } catch (UsernameNotFoundException ex) {
	        log.error("Authentication failed. Username not found: {}", username, ex);
	        throw ex;

	    } catch (Exception ex) {
	        log.error("Error while loading user details for username: {}", username, ex);
	        throw new UsernameNotFoundException("Unable to authenticate user: " + username, ex);
	    }
	}}
