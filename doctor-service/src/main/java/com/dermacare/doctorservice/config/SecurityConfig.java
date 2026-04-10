package com.dermacare.doctorservice.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF for Postman testing
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()   // Allow all requests without authentication
            )
            .httpBasic(Customizer.withDefaults()); // Optional for basic auth if needed

        return http.build();
    }
}

