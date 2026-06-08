package com.dermaCare.customerService.dto;

import java.util.Collection;
import java.util.List;

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
public class CustomerLoginDTO implements UserDetails  {
	
	private static final long serialVersionUID = 1L;
	private String userName;
	private String password;
	private String deviceId;
	private List<String> roles;
	
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
		return userName;
	}
	
}
