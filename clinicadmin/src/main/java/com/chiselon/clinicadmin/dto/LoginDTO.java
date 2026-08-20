
package com.chiselon.clinicadmin.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginDTO implements UserDetails {
	
	private static final long serialVersionUID = 1L;
	private String userName;
    private String password;
	private String role;
	private List<String> roles;
	private String deviceId;
	private String staffId;
	private String staffName;
	private String hospitalName;
	private String hospitalId;
	private String branchId;
	private String branchName;
	private Map<String, List<String>> permissions;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
	    if (roles == null) {
	        return Collections.emptyList();
	    }

	    return roles.stream()
	            .map(SimpleGrantedAuthority::new)
	            .toList();
	}
	
	@Override
	public String getUsername() {		
		return userName;
	}
}
