package physiotherapydoctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaServer
@EnableDiscoveryClient
@EnableFeignClients
@EnableScheduling
public class PhysiotherapydoctorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhysiotherapydoctorServiceApplication.class, args);
	}

}
