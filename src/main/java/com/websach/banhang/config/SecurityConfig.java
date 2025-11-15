package com.websach.banhang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Khai báo Bean PasswordEncoder (giữ nguyên để UserService không bị lỗi)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Định nghĩa chuỗi bộ lọc bảo mật (Security Filter Chain)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF (Thường cần cho các API, nhưng nên cân nhắc)
                .authorizeHttpRequests(authorize -> authorize
                        // Cấu hình cho phép TẤT CẢ các request được truy cập mà không cần xác thực
                        // Nếu muốn cụ thể, bạn có thể dùng: .requestMatchers("/user/**", "/css/**", "/js/**").permitAll()
                        .anyRequest().permitAll()
                )
                // Tắt hoàn toàn cơ chế form login mặc định
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}