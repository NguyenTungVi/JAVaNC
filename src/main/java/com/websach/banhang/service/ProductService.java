package com.websach.banhang.service;

import com.websach.banhang.model.Product;
import com.websach.banhang.model.Category;
import com.websach.banhang.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Lấy sản phẩm theo ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // Lưu hoặc cập nhật sản phẩm
    public Product saveProduct(Product product) {
        product.updateStatus(); // Cập nhật trạng thái trước khi lưu
        return productRepository.save(product);
    }

    // Xóa sản phẩm
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // Giảm số lượng khi mua
    public void decreaseQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        product.setQuantity(product.getQuantity() - quantity);
        product.updateStatus();
        productRepository.save(product);
    }

    // ⭐ PHƯƠNG THỨC MỚI: TÌM KIẾM VÀ LỌC KẾT HỢP
    // SỬA: Lọc theo thể loại
    public List<Product> getProductsByCategory(String categoryName) {
        // ⭐ Thay đổi: Tìm Category Entity bằng tên
        Category category = categoryService.findByName(categoryName.toUpperCase());
        if (category == null) {
            return List.of(); // Trả về list trống nếu không tìm thấy thể loại
        }
        return productRepository.findByCategory(category);
    }

    // ⭐ PHƯƠNG THỨC 1: HỖ TRỢ SHOP VỚI PHÂN TRANG (TRẢ VỀ PAGE)
    // Signature: (String, String, int, int)
    public Page<Product> searchAndFilterProducts(String keyword, String categoryName, int page, int size) {
        if (keyword == null) {
            keyword = "";
        }

        Pageable pageable = PageRequest.of(page, size);

        Category category = null;
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            category = categoryService.findByName(categoryName.toUpperCase());
        }

        // Gọi Repository với Pageable
        // ⚠️ Đảm bảo Repository có hàm searchAndFilter(keyword, category, pageable)
        return productRepository.searchAndFilter(keyword, category, pageable);
    }

    // ⭐ PHƯƠNG THỨC 2: HỖ TRỢ ADMIN DASHBOARD (TRẢ VỀ LIST KHÔNG PHÂN TRANG)
    // Signature: (String, String)
    public List<Product> searchAndFilterProducts(String keyword, String categoryName) {
        if (keyword == null) {
            keyword = "";
        }

        Category category = null;
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            category = categoryService.findByName(categoryName.toUpperCase());
        }

        if (keyword.isEmpty() && category == null) {
            return productRepository.findAll();
        }

        // ⚠️ Giả định Repository có hàm searchAndFilterList(keyword, category)
        return productRepository.searchAndFilterList(keyword, category);
    }

    // ⭐ PHƯƠNG THỨC MỚI: Tăng số lượng khi hủy đơn hàng
    @Transactional
    public void increaseQuantity(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        product.setQuantity(product.getQuantity() + quantity);
        product.updateStatus();
        productRepository.save(product);
    }
}
