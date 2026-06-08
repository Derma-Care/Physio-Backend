package com.clinicadmin.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClinicCredentialsDTO implements UserDetails {

	private static final long serialVersionUID = 1L;
	private String userName;
	private String password;
	private List<String> roles;
	private String clinicAdminWebFcmToken;
	private String hospitalId;
	private String branchId;
	private String hospitalName;
	private Map<String, Map<String, List<String>>> permissions;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		if(roles != null) {
	return roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();
		}else{
			return null;
		}
}
	
	@Override
	public String getUsername() {
		return userName ;
	}
}
