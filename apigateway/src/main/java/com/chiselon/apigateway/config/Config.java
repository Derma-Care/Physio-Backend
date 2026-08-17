//package com.dermacare.demo.config;
//
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//
//
//@Configuration
//public class Config {
//	
//	@Bean
//	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
//
//	    return http
//	            .cors(Customizer.withDefaults())
//	            .csrf(ServerHttpSecurity.CsrfSpec::disable)
//	            .authorizeExchange(exchanges -> exchanges
//	                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
//	                    .anyExchange().authenticated())
//	            .build();
//	}
//
//}
