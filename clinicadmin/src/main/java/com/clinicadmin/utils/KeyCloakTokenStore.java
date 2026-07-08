package com.clinicadmin.utils;

import com.clinicadmin.feignclient.KeyCloakFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RetryableException;
import jakarta.ws.rs.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;


@Component
@Data
@Slf4j
public class KeyCloakTokenStore {
	
	@Autowired
	private KeyCloakFeign keyCloakFeign;
	
	
	@Autowired
	private AutoReqForNewAccessTokenBeforeTokenExpiration autoReqForNewAccessToken;
	
	private String access_token;
	private Long expires_in;
	 @Retryable(value = {RetryableException.class,NotAuthorizedException.class,ForbiddenException.class,NotFoundException.class,BadRequestException.class,InternalServerErrorException.class}, maxAttempts = 4, backoff = @Backoff(delay = 4000))
	 public void obtainKeycloakToken(){ // CHECK TOKEN FOR SERVICE PRESENT OT NOT
		 log.info("obtainKeycloakToken method is invoked");
		 try {
			 if(access_token == null) {
				 MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
				 form.add("grant_type", "client_credentials");
				 form.add("client_id", "admin-service");
				 form.add("client_secret", "QR5OBooIYbefMFXbf5TMR2EJ2SS9xMRI");
				  Map<String, Object> data = keyCloakFeign.getToken(form);
				  if(data != null) {
				 Map<String,String> token  =  new ObjectMapper().convertValue(data,new TypeReference<Map<String,String>>() {});
				 access_token = "Bearer "+token.get("access_token");
				 expires_in = Long.valueOf(token.get("expires_in"));
				    }}}catch(Exception e) { log.error(e.getMessage());}
	            }
	 	
	 	 
		@Recover
		public void backOffMessage(RetryableException e) {
			log.error(e.getMessage());
		}
				
				
		  @Scheduled(initialDelay = 1000 * 60 * 1, fixedRate = 1000 * 60 * 8) 
			 public void reqForKeycloakTokenBeforeExpireWithScheduler(){				
			  try {
			  autoReqForNewAccessToken.reqforNewServiceToken();
			  }catch(Exception e) {log.error(e.getMessage());}
		  }

}
