package com.AdminService.util;

import com.AdminService.feign.KeyCloakFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RetryableException;
import jakarta.ws.rs.*;
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
public class AutoReqForNewAccessTokenBeforeTokenExpiration {

	
	@Autowired
	private KeyCloakFeign keyCloakFeign;
	
	@Autowired
	@Lazy
	private KeyCloakTokenStore keyCloakTokenStore;
	
	@Retryable(value = {RetryableException.class,NotAuthorizedException.class,ForbiddenException.class,NotFoundException.class,BadRequestException.class,InternalServerErrorException.class}, maxAttempts = 8, backoff = @Backoff(delay = 10000))
	public void reqforNewServiceToken() { // TO GET JWKS FROM AUTH SERVICE
		// System.out.println("autoCheckForJwks method invoked");
		//try {	if(utilityForStoreJwtTokenAndExpiryTime.getAccess_token() == null && utilityForStoreJwtTokenAndExpiryTime.getExpires_in() == null) {
			 MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			 form.add("grant_type", "client_credentials");
			 form.add("client_id", "clinicadminservice@8080");
			 form.add("client_secret", "OBBIAnghPvfF7kVkI4pTj2jv2ndmoa0g");
			  Map<String, Object> data = keyCloakFeign.getToken(form);
			 // System.out.println(data);
			  if(data != null) {
				  Map<String,String> token  =  new ObjectMapper().convertValue(data,new TypeReference<Map<String,String>>() {});
				  keyCloakTokenStore.access_token = "Bearer "+token.get("access_token");
				  keyCloakTokenStore.expires_in = Long.valueOf(token.get("expires_in"));
//					  System.out.println(keyCloakTokenStore.access_token);
//					  System.out.println(keyCloakTokenStore.expires_in);
			  //System.out.println(utilityForStoreJwtTokenAndExpiryTime);
	          }}
//		}catch(FeignException e) {
//			System.out.println( e.getClass());
//		}
	
	
	
	@Recover
	public void message(RetryableException e) {
		System.out.println(e.getMessage());
	}	
	
}
