package com.websach.banhang.repository;

import com.websach.banhang.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Để tìm kiếm theo tên khi cần (ví dụ: kiểm tra tồn tại khi thêm mới)
    Category findByName(String name);
}

