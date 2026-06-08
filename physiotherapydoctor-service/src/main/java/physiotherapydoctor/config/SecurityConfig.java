package physiotherapydoctor.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import physiotherapydoctor.serviceImpl.CustomDoctorLoginDetailsService;
import physiotherapydoctor.util.JWTAthenticationEntryPoint;
import physiotherapydoctor.util.JwtAuthFilter;



@Configuration
@EnableWebSecurity
//@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
	
	 @Autowired 
	 private JwtAuthFilter jwtAuthFilter;
	 
	 @Autowired
	 private JWTAthenticationEntryPoint jWTAthenticationEntryPoint;
			

	    @Bean
	    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	        http.csrf(csrf -> csrf.disable())
	            .authorizeHttpRequests(auth -> auth
	                .requestMatchers("/clinic-admin/clinicLogin","/clinic-admin/doctorLogin/**",
	                "/clinic-admin/customers/login","/clinic-admin/newAccessTokenForClinicAdminService")
	                .permitAll()
	                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
	                .anyRequest().authenticated()
	            ).exceptionHandling(ex->ex.authenticationEntryPoint(jWTAthenticationEntryPoint))
	            .sessionManagement(session -> session
	                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	            )
	            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

	        return http.build();
	    }
	    	    
	    @Bean
	    public PasswordEncoder passwordEncoder() {
	        return new BCryptPasswordEncoder();
	    }
	    
	    @Bean
	    public UserDetailsService userDetailsService() {
	    	return new CustomDoctorLoginDetailsService();
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

