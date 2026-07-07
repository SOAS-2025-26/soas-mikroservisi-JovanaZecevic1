package com.soas.users_service;

import com.soas.users_service.repository.UserRepository;
import com.soas.users_service.service.UsersServiceImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import serviceLibrary.dto.usersService.UserDto;

@SpringBootApplication
@ComponentScan(basePackages = {"com.soas.users_service", "util"})
@EnableFeignClients(basePackages = "serviceLibrary.proxies")
public class UsersServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(UsersServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(UsersServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner seedDefaultTestUsers(UserRepository userRepository, UsersServiceImplementation usersService) {
		return args -> {
			seedIfMissing(userRepository, usersService, "owner@gmail.com", "owner", "OWNER");
			seedIfMissing(userRepository, usersService, "admin@gmail.com", "admin", "ADMIN");
			seedIfMissing(userRepository, usersService, "user@gmail.com", "user", "USER");
			seedIfMissing(userRepository, usersService, "user1@gmail.com", "user1", "USER");
			seedIfMissing(userRepository, usersService, "user2@gmail.com", "user2", "USER");
		};
	}

	private void seedIfMissing(UserRepository userRepository, UsersServiceImplementation usersService,
								String email, String password, String role) {
		if (userRepository.existsByEmail(email)) {
			return;
		}
		try {
			usersService.createUser("OWNER", new UserDto(email, password, role));
			log.info("Seeded default test user: {}", email);
		} catch (Exception e) {
			log.warn("Could not seed default test user {}: {}", email, e.getMessage());
		}
	}

}
