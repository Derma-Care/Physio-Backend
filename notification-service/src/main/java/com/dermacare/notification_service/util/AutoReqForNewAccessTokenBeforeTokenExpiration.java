package com.dermacare.notification_service.util;

import com.dermacare.notification_service.feign.KeyCloakFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import feign.RetryableException;
import jakarta.ws.rs.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;


@Component
@Slf4j
public class AutoReqForNewAccessTokenBeforeTokenExpiration {

	
	@Autowired
	private KeyCloakFeign keyCloakFeign;
	
	@Autowired
	@Lazy
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Retryable(value = {RetryableException.class,NotAuthorizedException.class,ForbiddenException.class,NotFoundException.class,BadRequestException.class,InternalServerErrorException.class}, maxAttempts = 8, backoff = @Backoff(delay = 10000))
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
				  keyCloakTokenStore.setACCESS_TOKEN("Bearer "+token.get("access_token"));
				  keyCloakTokenStore.setEXPIRES_IN(Long.valueOf(token.get("expires_in")));					 
			  }}catch(FeignException e) {
			log.error("exception occured", e.getMessage());;
		}}
	
	
	
	@Recover
	public void message(RetryableException e) {
		log.error(e.getMessage());
	}	
	
}
