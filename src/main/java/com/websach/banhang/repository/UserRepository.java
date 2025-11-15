package com.websach.banhang.repository;

import com.websach.banhang.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Phương thức đã định nghĩa trong file gốc, rất hữu ích để kiểm tra tồn tại và đăng nhập
    User findByEmail(String email);

}