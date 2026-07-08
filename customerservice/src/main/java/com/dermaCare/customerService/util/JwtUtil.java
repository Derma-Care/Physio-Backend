package com.dermaCare.customerService.util;


import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;


@Component
public class JwtUtil {
	
	public String formattedTimeByZone;	
	
	@Value("${jwt.secret-key}")
	public String SECRET;
	
	@Value("${refresh-token.secret-key}")
	public String REFRESH_TOEKN_SECRET_KEY;
		
	public String generateJwtToken(String serviceName,List<String> roles) {		
	    Key key = Keys.hmacShaKeyFor(SECRET.getBytes()); 
	   // System.out.println(key);
		ZonedDateTime time = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		ZonedDateTime plusTime = time.plusHours(2);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
	    this.formattedTimeByZone = plusTime.format(formatter);
		Date specificDateTime = Date.from(plusTime.toInstant());	
        Map<String ,Object> claims = new LinkedHashMap<>();
        claims.put("preferred_username", serviceName);
        claims.put("roles", roles);          
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(specificDateTime)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();}
	
	
	public String generateRefreshToken(String serviceName,List<String> roles) {		
	    Key key = Keys.hmacShaKeyFor(REFRESH_TOEKN_SECRET_KEY.getBytes()); 
	   // System.out.println(key);
		ZonedDateTime time = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
		ZonedDateTime plusTime = time.plusDays(30);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
	    this.formattedTimeByZone = plusTime.format(formatter);
		Date specificDateTime = Date.from(plusTime.toInstant());	
        Map<String ,Object> claims = new LinkedHashMap<>();
        claims.put("preferred_username", serviceName);
        claims.put("roles", roles);          
		return Jwts.builder()
				.setClaims(claims)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(specificDateTime)
				.signWith(key, SignatureAlgorithm.HS256)
				.compact();}
	
	
	
    public Response validateAndGenerateRefreshToken(String RefToken) {
    	Response res = new Response();
    	try {
    		if(!isRefreshTokenExprired(RefToken)){
    			res.setData(Collections.singletonMap("jwtToken", generateJwtToken(extractNameFromRefreshToken(RefToken),extractRoleFromRefreshToken(RefToken))));
    			res.setStatus(200);
    			res.setSuccess(true);
    			res.setMessage("new jwt token");
    		}else {
    			res.setMessage("RefreshToken Has Expired Please Login To Get New RefreshToken");
    			res.setStatus(401);
    			res.setSuccess(false);
    		}}catch(Exception e) {
    			res.setStatus(500);
    			res.setMessage(e.getMessage());
    			res.setSuccess(false);
    	}
    	return res;
    }
    	
   
    public String extractServiceIdFromToken(String token) {
    	String sub = new String();
		     try {
				Key currentSecretKeyBytes = Keys.hmacShaKeyFor(SECRET.getBytes());				
				Claims claims = Jwts.parserBuilder()
						.setSigningKey(currentSecretKeyBytes)
						.build()
						.parseClaimsJws(token)
						.getBody();	
					   // System.out.println(claims);
				    sub = claims.getSubject();
				    return sub;
				     }catch(Exception e) {
				    	 System.out.println(e.getMessage());
							return null;
						}}
				
					
	public List<String> extractRoleFromToken(String token) {
		List<String> roles = new ArrayList<>();
		try {			
			Key currentSecretKeyBytes = Keys.hmacShaKeyFor(SECRET.getBytes());				
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(currentSecretKeyBytes)
					.build()
					.parseClaimsJws(token)
					.getBody();	
	Object obj = claims.get("roles",ArrayList.class);
	roles = new ObjectMapper().convertValue(obj, new TypeReference<ArrayList<String>>(){});
	}catch(Exception e) { roles = null;}
		return roles;
}
		
	
	public String extractServiceNameFromToken(String token) {
		String name = new String();
		    try {			
				Key currentSecretKeyBytes = Keys.hmacShaKeyFor(SECRET.getBytes());				
				Claims claims = Jwts.parserBuilder()
						.setSigningKey(currentSecretKeyBytes)
						.build()
						.parseClaimsJws(token)
						.getBody();	
				name = claims.get("preferred_username", String.class);
		}catch(Exception e) {
		System.out.println(e.getMessage());
		name = null;
	}
		return name;
}

	
	public String extractNameFromRefreshToken(String token) {
		String name = new String();
		    try {			
				Key currentSecretKeyBytes = Keys.hmacShaKeyFor(REFRESH_TOEKN_SECRET_KEY.getBytes());				
				Claims claims = Jwts.parserBuilder()
						.setSigningKey(currentSecretKeyBytes)
						.build()
						.parseClaimsJws(token)
						.getBody();	
				name = claims.get("preferred_username", String.class);
		}catch(Exception e) {
		System.out.println(e.getMessage());
		name = null;
	}
		return name;
}
	
	
	public List<String> extractRoleFromRefreshToken(String token) {
		List<String> roles = new ArrayList<>();
		try {			
			Key currentSecretKeyBytes = Keys.hmacShaKeyFor(REFRESH_TOEKN_SECRET_KEY.getBytes());				
			Claims claims = Jwts.parserBuilder()
					.setSigningKey(currentSecretKeyBytes)
					.build()
					.parseClaimsJws(token)
					.getBody();	
	Object obj = claims.get("roles",ArrayList.class);
	roles = new ObjectMapper().convertValue(obj, new TypeReference<ArrayList<String>>(){});
	}catch(Exception e) { roles = null;}
		return roles;
}
		
	
	public boolean isTokenExprired(String token) {
		boolean exp = false;
		try {
				Key currentSecretKeyBytes = Keys.hmacShaKeyFor(SECRET.getBytes());				
				Claims claims = Jwts.parserBuilder()
						.setSigningKey(currentSecretKeyBytes)
						.build()
						.parseClaimsJws(token)
						.getBody();	
				exp = claims.getExpiration().before(new Date());
				//System.out.println(exp);
				}catch(Exception e) {
			//System.out.println(e.getMessage());
			exp = true;}
		return exp; }
	
	
	public boolean isRefreshTokenExprired(String token) {
		boolean exp = false;
		try {
				Key currentSecretKeyBytes = Keys.hmacShaKeyFor(REFRESH_TOEKN_SECRET_KEY.getBytes());				
				System.out.println(REFRESH_TOEKN_SECRET_KEY);
				Claims claims = Jwts.parserBuilder()
						.setSigningKey(currentSecretKeyBytes)
						.build()
						.parseClaimsJws(token)
						.getBody();	
				exp = claims.getExpiration().before(new Date());
				//System.out.println(exp);
				}catch(Exception e) {
			//System.out.println(e.getMessage());
			exp = true;}
		//System.out.println(exp);
		return exp; }
	
	
	public boolean validateToken(String token) {
		try {
		if(!isTokenExprired(token)) {
		return true;}
		else {
	    return false;}
        }catch(Exception e) {
	    return false;}}
			
			
		public String decodeTokenWithoutKey(String token) {
	        String[] parts = token.split("\\.");
	        if (parts.length == 3) {
	            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
	         return payloadJson;
	        } else {
	            return null;
	        }
	    }
		
		
		@RateLimiter(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
		@Retry(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
		@CircuitBreaker(name = "keyclaok", fallbackMethod = "tokenIntrospectionFallback")
		public Map<Object,Object> tokenIntrospection(String token) {
			  Map<Object,Object> map = null;
			 if(token != null) {
				  RestTemplate restTemplate = new RestTemplate();

				   				 String introspectionUrl =
				                "http://localhost:9094/realms/physiocare/protocol/openid-connect/token/introspect";

				        HttpHeaders headers = new HttpHeaders();
				        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

				        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
				        form.add("token", token);
				        form.add("client_id", "admin-service");
				        form.add("client_secret", "QR5OBooIYbefMFXbf5TMR2EJ2SS9xMRI");

				        HttpEntity<MultiValueMap<String, String>> request =
				                new HttpEntity<>(form, headers);

				        ResponseEntity<Object> response = restTemplate.exchange(
				                introspectionUrl,
				                HttpMethod.POST,
				                request,
				                Object.class
				        );

				        map = new ObjectMapper().convertValue(response.getBody(),new TypeReference<Map<Object,Object>>() {
						});
				    }
			 return map;
   }
		
		
		
		public Map<Object,Object> tokenIntrospectionFallback(String token){
					
			return null;
		}
			
		}
	

