package com.dermaCare.customerService.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.dermaCare.customerService.service.CustomCustomerDetailsService;
import com.dermaCare.customerService.util.JWTAthenticationEntryPoint;
import com.dermaCare.customerService.util.JwtAuthFilter;


@Configuration
@EnableWebSecurity
//@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
	
	 @Autowired 
	 private JwtAuthFilter jwtAuthFilter;
	 
	 @Autowired
	 private JWTAthenticationEntryPoint jWTAthenticationEntryPoint;
		
	 
	  @Bean
	    @Order(1)
	    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
	        http
	            .securityMatcher("/oauth2/authorization/google","/login/**")  
	            .authorizeHttpRequests(auth -> auth
	                .anyRequest().authenticated())
	            .oauth2Login(n->n.defaultSuccessUrl("/customerPublicApis/oauth2tokens"));
	        return http.build();
	    }    
	    	    	

	    @Bean
	    @Order(2)
	    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	        http.csrf(csrf -> csrf.disable()).securityMatcher("/customerpublicapis")
	            .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/VerifyUserCredentialsAndGenerateAndSendOtp","/verifyOtp","/resendOtp",
	                "/newAccessTokenForCustomerService","/oauth2tokens")
	                .permitAll()
	                .anyRequest().authenticated()
	            ).exceptionHandling(ex->ex.authenticationEntryPoint(jWTAthenticationEntryPoint))
	            .sessionManagement(session -> session
	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	            )
	            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

	        return http.build();
	    }	
	    
	    
	    @Bean
		public ClientRegistrationRepository clientRegistrationRepository() {
			return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
		}

		private ClientRegistration googleClientRegistration() {
			return ClientRegistration.withRegistrationId("google")
				.clientId("847747434266-ghg1mghtfe6q54bu41iabsaesi0ordkv.apps.googleusercontent.com")
				.clientSecret("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")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost:8083/api/login/oauth2/code/google")
				.scope("openid", "profile", "email", "address", "phone")
				.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
				.tokenUri("https://www.googleapis.com/oauth2/v4/token")
				.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
				.userNameAttributeName(IdTokenClaimNames.SUB)
				.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
				.issuerUri("https://accounts.google.com")
				.clientName("Google")
				.build();
		}
		
		 @Bean
		    public PasswordEncoder passwordEncoder() {
		        return new BCryptPasswordEncoder();
		    }
		    
		    @Bean
		    public UserDetailsService userDetailsService() {
		    	return new CustomCustomerDetailsService();
		    }
		    
		    
		    @Bean	    
		    public AuthenticationManager customAuthenticationManager(UserDetailsService userDetailsService,
		    		PasswordEncoder passwordEncoder) {
		    	DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		    	authProvider.setUserDetailsService(userDetailsService);
		    	authProvider.setPasswordEncoder(passwordEncoder);
		    	return new ProviderManager(authProvider);
		    }
				
}
