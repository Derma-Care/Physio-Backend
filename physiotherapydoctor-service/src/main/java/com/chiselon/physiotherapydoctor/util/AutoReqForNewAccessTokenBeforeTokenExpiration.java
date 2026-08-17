package com.chiselon.physiotherapydoctor.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AutoReqForNewAccessTokenBeforeTokenExpiration {
	
	@Autowired
	private KeyCloakFeignImpl keyCloakFeign;
	
	@Autowired
	@Lazy
	private KeyCloakTokenStore keyCloakTokenStore;
	
	public void reqforNewServiceToken() { // TO GET JWKS FROM AUTH SERVICE
		  log.info("reqforNewServiceToken method is invoked");
		try {
			 MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			 form.add("grant_type", "client_credentials");
			 form.add("client_id", "notification_service");
			 form.add("client_secret", "MsHiOtmJfzc86dz9n2fbKgDa6ZOFnNIX");
			  Map<String, Object> data = keyCloakFeign.getToken(form);			
			  if(data != null) {
				  Map<String,String> token  =  new ObjectMapper().convertValue(data,new TypeReference<Map<String,String>>() {});
				  keyCloakTokenStore.setAccess_token("Bearer "+token.get("access_token"));
				  keyCloakTokenStore.setExpires_in(Long.valueOf(token.get("expires_in")));
				 }}catch(FeignException e) {
			log.error("exception occured", e.getMessage());;
		}}
}
