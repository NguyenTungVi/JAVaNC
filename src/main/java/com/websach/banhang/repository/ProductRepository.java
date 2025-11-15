package com.websach.banhang.repository;

import com.websach.banhang.model.Product;
import com.websach.banhang.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);

    // Loại bỏ: List<Product> searchByNameOrAuthor(@Param("keyword") String keyword);

    // ⭐ 1. PHƯƠNG THỨC CHO ADMIN DASHBOARD (NON-PAGING)
    // Đổi tên từ searchAndFilter thành searchAndFilterList để khớp với ProductService
    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:category IS NULL OR p.category = :category)")
    List<Product> searchAndFilterList(@Param("keyword") String keyword, @Param("category") Category category);

    // ⭐ 2. PHƯƠNG THỨC CHO SHOP (PAGING)
    @Query("SELECT p FROM Product p WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND (:category IS NULL OR p.category = :category)")
    Page<Product> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            Pageable pageable);
}