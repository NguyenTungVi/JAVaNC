package com.websach.banhang.config;

import com.websach.banhang.model.User;
import com.websach.banhang.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.file.Files;

@Configuration
public class admin {

    @Bean
    CommandLineRunner init(UserRepository userRepository) {
        return args -> {
            String adminEmail = "admin@gmail.com";

            // Kiểm tra nếu admin chưa tồn tại
            User existingAdmin = userRepository.findByEmail(adminEmail);
            if (existingAdmin == null) {  // <--- sửa isEmpty() thành null check
                ClassPathResource imgFile = new ClassPathResource("static/images/default-avatar.png");
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("System");
                admin.setImage(Files.readAllBytes(imgFile.getFile().toPath()));
                admin.setPassword(new BCryptPasswordEncoder().encode("admin123"));
                admin.setEmail(adminEmail);
                admin.setRole("ADMIN");
                admin.setEnabled(true);

                userRepository.save(admin);
                System.out.println("Admin account created!");
            } else {
                System.out.println("Admin account already exists.");
            }
        };
    }
}
