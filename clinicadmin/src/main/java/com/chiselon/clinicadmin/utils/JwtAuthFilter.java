package com.chiselon.clinicadmin.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chiselon.clinicadmin.dto.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
	
	@Autowired
	private JwtUtil jwtUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
	    String authHeader = request.getHeader("Authorization");
		String token;
		String userName;
		if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			response.setStatus(HttpServletResponse.SC_OK);
			return;}
		if(authHeader != null && authHeader.startsWith("Bearer ")){
		log.info("token is received");
		token = authHeader.substring(7);
		String extractHeader = jwtUtil.decodeTokenWithoutKey(token);
		 String[] parts = extractHeader.split("\\:");
		 String alg = parts[1].substring(1, 6);
		 log.info("alg is extracted from token");
		if(alg.equalsIgnoreCase("HS256")){
		if(jwtUtil.validateToken(token)){
		log.info("token is validated");
		userName = jwtUtil.extractServiceNameFromToken(token);
		 log.info("username is extracted");
		List<String> roles = jwtUtil.extractRoleFromToken(token);
		log.info("roles are extracted");
		if(userName != null && SecurityContextHolder.getContext().getAuthentication() == null ) {				
			List<SimpleGrantedAuthority> rls = roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();
			UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
			new UsernamePasswordAuthenticationToken(userName,null,rls);
			usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);	
			log.info("authentication object is set into security context");
		    }}}else{
		    Map<Object,Object> map = jwtUtil.tokenIntrospection(token);
			if(map.get("active").equals(true)) {
		    log.info("service details are received from tokenIntrospection");
//		    Map<Object,Object> lst = new ObjectMapper().convertValue(map.get("resource_access"),new TypeReference<Map<Object,Object>>(){});	
//		    Object client_id = map.get("client_id");
				Map<String, ResourceAccess> client = new ObjectMapper().convertValue(map.get("resource_access"),new TypeReference<Map<String, ResourceAccess>>() {});
				ResourceAccess resourceAccess = client.get("admin_service");
				List<String> roles = resourceAccess.getRoles();
		    log.info("realm_access roles are extracted");
		  // List<String> roles = new ObjectMapper().convertValue(lst.get(client_id),new TypeReference<List<String>>(){});
		    	  log.info("Introspection response showing token is active");
		    	if(SecurityContextHolder.getContext().getAuthentication() == null ) {		
					//UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
					List<SimpleGrantedAuthority> rls = roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();
					  log.info("realm_access roles are:{}",rls);
					UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
					new UsernamePasswordAuthenticationToken(map.get("username"),null,rls);
			///its used to add information related to request to authenticated object along with userdetails
					usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);						   	
		    }}}}
		filterChain.doFilter(request, response);
		 }catch(Exception e) {   	 
			   Response error = new Response();
		       error.setMessage(e.getMessage());
		       error.setStatus(500);
		       error.setSuccess(false);
		   	   response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		       response.setContentType("application/json");
		       new ObjectMapper().writeValue(response.getOutputStream(), error);
		   }}}