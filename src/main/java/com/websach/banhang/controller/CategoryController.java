package com.websach.banhang.controller;

import com.websach.banhang.model.Category;
import com.websach.banhang.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // ================================
    // READ: Lấy danh sách (Dùng trong user.html, nhưng việc load dữ liệu được thực hiện ở Controller chứa user/profile)
    // ================================

    // ================================
    // CREATE: Hiển thị form thêm mới (dùng cho Modal AJAX)
    // ================================
    @GetMapping("/add-form")
    public String showAddForm() {
        // Trả về fragment chỉ chứa form thêm Category
        return "fragments/admin-categories :: addCategoryForm";
    }

    // ================================
    // CREATE: Lưu Category mới
    // ================================
    @PostMapping("/add")
    public String addCategory(@RequestParam("categoryName") String name) {
        try {
            categoryService.addCategory(name);
        } catch (RuntimeException e) {
            // Xử lý lỗi nếu tên đã tồn tại
            // Có thể thêm thông báo lỗi vào Model và redirect, nhưng đơn giản là in ra console
            System.err.println("Lỗi thêm thể loại: " + e.getMessage());
        }
        // Redirect về tab quản lý thể loại sau khi hoàn thành
        return "redirect:/user/profile?tab=admin-categories";
    }

    // ================================
    // DELETE: Xóa Category
    // ================================
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        // Cần thêm logic kiểm tra xem category này có đang được sản phẩm nào sử dụng không
        // Nếu có, cần xử lý (ví dụ: cấm xóa hoặc đặt category sản phẩm thành null)
        try {
            categoryService.deleteCategory(id);
        } catch (Exception e) {
            // Xử lý lỗi (ví dụ: lỗi khóa ngoại)
            System.err.println("Không thể xóa thể loại: " + e.getMessage());
        }
        return "redirect:/user/profile?tab=admin-categories";
    }

    // ================================
    // UPDATE: Hiển thị form sửa (dùng cho Modal AJAX)
    // ================================
    @GetMapping("/edit-form/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Category category = categoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại"));
        model.addAttribute("category", category);
        return "fragments/admin-categories :: editCategoryForm";
    }

    // ================================
    // UPDATE: Lưu thay đổi Category
    // ================================
    @PostMapping("/update")
    public String updateCategory(@ModelAttribute Category category) {
        // Cần đảm bảo tên được chuẩn hóa (Uppercase) trước khi lưu
        category.setName(category.getName().trim().toUpperCase().replace(' ', '_'));
        categoryService.saveCategory(category);
        return "redirect:/user/profile?tab=admin-categories";
    }
}