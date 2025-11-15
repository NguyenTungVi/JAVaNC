package com.websach.banhang.service;

import com.websach.banhang.model.Category;
import com.websach.banhang.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    public Category findByName(String name) {
        return categoryRepository.findByName(name);
    }

    // 🌟 Hàm Admin dùng để thêm Category mới 🌟
    public Category addCategory(String categoryName) {
        // Chuẩn hóa tên (nên dùng chữ IN HOA)
        String standardizedName = categoryName.trim().toUpperCase().replace(' ', '_');

        // Kiểm tra xem thể loại đã tồn tại chưa
        if (categoryRepository.findByName(standardizedName) != null) {
            throw new RuntimeException("Thể loại \"" + standardizedName + "\" đã tồn tại.");
        }

        // Tạo và lưu Category mới
        Category newCategory = new Category(standardizedName);
        return categoryRepository.save(newCategory);
    }

    // ⭐ CHỨC NĂNG 1: Lưu hoặc cập nhật Category (để dùng cho UPDATE) ⭐
    // Lệnh này fix lỗi "Cannot resolve method 'saveCategory' in 'CategoryService'"
    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    // ⭐ CHỨC NĂNG 2: Xóa Category (để dùng cho DELETE) ⭐
    // Lệnh này fix lỗi "Cannot resolve method 'deleteCategory' in 'CategoryService'"
    public void deleteCategory(Long id) {
        // Lưu ý: Nếu có lỗi khóa ngoại, bạn cần xử lý ở đây hoặc trong Controller
        categoryRepository.deleteById(id);
    }

    // ⭐ CHỨC NĂNG 3: Tìm kiếm theo ID (Cần cho việc SỬA/EDIT) ⭐
    // Phương thức này cũng cần thiết cho hàm showEditForm trong Controller
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }
}