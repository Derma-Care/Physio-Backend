package com.AdminService.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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
@Document(collection =  "Credentials")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterAndLoginEntity implements UserDetails {
@Id
private String id;
private static final long serialVersionUID = 1L;
private String mobileNumber;
private String userName;
private String password;
private List<String> roles;
private String serviceId;
private boolean isRegistred;

@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
if(roles != null) {
return roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();}
else {
	return null;
}
}

@Override
public String getUsername() {	
	return userName ;
}

public boolean getRegistred() {
	return isRegistred;
}
}