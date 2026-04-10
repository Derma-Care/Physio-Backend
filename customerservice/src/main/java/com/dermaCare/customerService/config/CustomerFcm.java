package com.dermaCare.customerService.config;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.core.io.ClassPathResource;
import jakarta.annotation.PostConstruct;

@Configuration
public class CustomerFcm {
	
	@Value("${app.firebase-configuration-file}")
	private String firebaseConfigPath;

	@PostConstruct
	public void init() throws IOException {
		ClassPathResource resource = new ClassPathResource(firebaseConfigPath);
		InputStream is = resource.getInputStream();
		FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(is)).build();
		if (FirebaseApp.getApps().isEmpty()) {
			FirebaseApp.initializeApp(options);
		}
	}
}