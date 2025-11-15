package com.websach.banhang.service;

import com.websach.banhang.model.User;
import com.websach.banhang.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // TÁC DỤNG: Kiểm tra email đã tồn tại trong DB
    public boolean isEmailTaken(String email) {
        return userRepository.findByEmail(email) != null;
    }

    // TÁC DỤNG: Lưu User mới (có mã hóa mật khẩu)
    public User registerUser(User user) {
        // Mã hóa mật khẩu thô
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // Khởi tạo các giá trị mặc định nếu cần (role và enabled đã có default trong Entity constructor)

        return userRepository.save(user);
    }

    // TÁC DỤNG: Xác thực đăng nhập
    public User authenticateUser(String email, String rawPassword) {
        User user = userRepository.findByEmail(email);

        if (user == null) {
            return null;
        }

        // So sánh mật khẩu thô với mật khẩu đã mã hóa (BCrypt)
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return user;
        }

        return null;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // 🌟 PHƯƠNG THỨC MỚI: Lấy User cho API Avatar 🌟
    public User findUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    // 🌟 Lưu hoặc cập nhật thông tin người dùng (profile)
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // ================================
    // ⭐ ADMIN: QUẢN LÝ USER ⭐
    // ================================

    // Lấy tất cả user
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    // Cập nhật vai trò (Role)
    public void updateUserRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (newRole != null && (newRole.equalsIgnoreCase("ADMIN") || newRole.equalsIgnoreCase("USER"))) {
            user.setRole(newRole.toUpperCase());
            userRepository.save(user);
        }
    }

    // Xóa User
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}