package com.rk.bloodlab;

import com.rk.bloodlab.auth.AuthenticationService;
import com.rk.bloodlab.auth.RegisterRequest;
import com.rk.bloodlab.config.WhatsAppConfig;
import com.rk.bloodlab.user.Role;
import lombok.var;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(WhatsAppConfig.class)
public class BloodlabApplication {

	public static void main(String[] args) {
		SpringApplication.run(BloodlabApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(
			AuthenticationService service
	) {
		return args -> {
			RegisterRequest admin = RegisterRequest.builder()
					.firstname("Admin")
					.lastname("Admin")
					.email("admin@mail.com")
					.password("password")
					.role(Role.ADMIN)
					.build();
			System.out.println("Admin token: " + service.register(admin).getAccessToken());

		};
	}
}
