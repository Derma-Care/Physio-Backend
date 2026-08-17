package com.chiselon.physiotherapydoctor.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.chiselon.physiotherapydoctor.util.KeyCloakTokenStore;

@Component
public class StartupRunner implements CommandLineRunner {
	
	
	@Autowired
	private KeyCloakTokenStore jwtTokenStrore;
	
	@Override
	public void run(String... args) throws Exception {	
		obtainKeycloakToken();
	}

	
	private void obtainKeycloakToken(){
		      try{						
				 jwtTokenStrore.obtainKeycloakToken();				 
			  }catch(Exception e) {}
	}
	
	
}
