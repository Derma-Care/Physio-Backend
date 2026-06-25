package com.clinicadmin.utils;

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

import com.clinicadmin.dto.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RetryableException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
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
		if(authHeader != null && authHeader.startsWith("Bearer ")){
		//System.out.println(authHeader);
		token = authHeader.substring(7);
		String extractHeader = jwtUtil.decodeTokenWithoutKey(token);
		 String[] parts = extractHeader.split("\\:");
		 String alg = parts[1].substring(1, 6);
		//System.out.println(alg);
		if(alg.equalsIgnoreCase("HS256")){
		///System.out.println(extractHeader);
		if(jwtUtil.validateToken(token)){
		userName = jwtUtil.extractServiceNameFromToken(token);
		List<String> roles = jwtUtil.extractRoleFromToken(token);
//		System.out.println(userName);
//		System.out.println(roles);
		if(userName != null && SecurityContextHolder.getContext().getAuthentication() == null ) {		
			//UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
			List<SimpleGrantedAuthority> rls = roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();
			UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
			new UsernamePasswordAuthenticationToken(userName,null,rls);
	////its used to add information related to request to authenticated object along with userdetails
			usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);		
		    }}}else{
		    Map<Object,Object> map = jwtUtil.tokenIntrospection(token);
		    //System.out.println(map);
//		    Map<Object,Object> lst = new ObjectMapper().convertValue(map.get("resource_access"),new TypeReference<Map<Object,Object>>(){});	
//		    Object client_id = map.get("client_id");
		    Map<String,List<String>> client = new ObjectMapper().convertValue(map.get("realm_access"),new TypeReference<Map<String,List<String>>>(){});		   
		   // System.out.println(client);
		    List<String> roles = client.get("roles");
		  // List<String> roles = new ObjectMapper().convertValue(lst.get(client_id),new TypeReference<List<String>>(){}); 			  
		    if(map.get("active").equals(true)) {		    	
		    	if(SecurityContextHolder.getContext().getAuthentication() == null ) {		
					//UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);
					List<SimpleGrantedAuthority> rls = roles.stream().map(n->new SimpleGrantedAuthority(n)).toList();
					///System.out.println(rls);
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
