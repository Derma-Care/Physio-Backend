package com.AdminService.config;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.AdminService.util.KeyCloakTokenStore;

@Component
public class StartupRunner implements CommandLineRunner {	
	
	@Autowired
	private KeyCloakTokenStore jwtTokenStrore;
	
	
	@Override
	public void run(String... args) throws Exception {	
		obtainKeycloakToken();
		secretKeyGenerator();
	}

	
	private void obtainKeycloakToken(){
		   try{						
				 jwtTokenStrore.obtainKeycloakToken();
			///System.out.println("autoCheckJwtToken");
			}catch(Exception e) {}
	}
	
	private void secretKeyGenerator(){
		try {
		 KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
	      SecretKey secretKey = keyGen.generateKey();
	      String encoded = Base64.getEncoder().encodeToString(secretKey.getEncoded());
       /// jwtUtil.SECRET = encoded;
	      System.out.println(encoded);
		}catch(NoSuchAlgorithmException e) {
			System.out.println(e.getMessage());
		}
	} 
	
}
