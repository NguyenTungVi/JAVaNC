package com.websach.banhang.controller;

import com.websach.banhang.model.Product;
import com.websach.banhang.model.Category;
import com.websach.banhang.service.ProductService;
import com.websach.banhang.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    // ================================
    // 📸 LẤY ẢNH SẢN PHẨM
    // ================================
    @GetMapping("/image/{id}")
    @ResponseBody
    public byte[] getProductImage(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(Product::getImage)
                .orElse(null);
    }

    // ================================
    // 🗑️ XOÁ SẢN PHẨM
    // ================================
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        // Giữ nguyên tab "Quản lý sản phẩm" sau khi reload
        return "redirect:/user/profile?tab=admin-products";
    }

    // 💾 THÊM / SỬA SẢN PHẨM
    // (Phần này đã đúng, không cần thay đổi)
    // ================================
    @PostMapping("/save")
    public String saveProduct(@ModelAttribute("product") Product product,
                              @RequestParam("imageFile") MultipartFile imageFile,
                              @RequestParam(value = "discountPrice", required = false) String discountPriceStr) throws IOException {

        // --- 1️⃣ XỬ LÝ ẢNH ---
        if (!imageFile.isEmpty()) {
            product.setImage(imageFile.getBytes());
        } else if (product.getId() != null) {
            // Nếu đang sửa và không có ảnh mới → giữ ảnh cũ
            Product existingProduct = productService.getProductById(product.getId()).orElse(null);
            if (existingProduct != null) {
                // Đảm bảo ảnh cũ được giữ lại
                product.setImage(existingProduct.getImage());
            }
        }
        // ... (phần xử lý giá giảm và lưu sản phẩm giữ nguyên) ...

        // --- 2️⃣ XỬ LÝ GIÁ GIẢM ---
        if (discountPriceStr != null && !discountPriceStr.trim().isEmpty()) {
            try {
                product.setDiscountPrice(Double.parseDouble(discountPriceStr.trim()));
            } catch (NumberFormatException e) {
                product.setDiscountPrice(null);
            }
        } else {
            product.setDiscountPrice(null);
        }

        // --- 3️⃣ LƯU SẢN PHẨM ---
        productService.saveProduct(product);

        // --- 4️⃣ QUAY LẠI ĐÚNG TAB ---
        return "redirect:/user/profile?tab=admin-products";
    }

    // ================================
    // ➕ HIỂN THỊ FORM THÊM SẢN PHẨM (cho Modal AJAX)
    // ================================
    @GetMapping("/add")
    public String addProduct(Model model) {
        // ⭐ Thay đổi: Lấy danh sách Category từ Service (Entity)
        model.addAttribute("categories", categoryService.findAllCategories());

        // Trả về Fragment mới tạo
        return "fragments/add-product :: addProductForm";
    }

    // ================================
    // ✏️ CHỈNH SỬA SẢN PHẨM (cho Modal AJAX)
    // ================================
    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        model.addAttribute("product", product);

        // ⭐ Thay đổi: Lấy danh sách Category từ Service (Entity)
        model.addAttribute("categories", categoryService.findAllCategories());

        // 🚨 THAY ĐỔI: Chỉ trả về fragment để dùng trong Modal
        return "fragments/edit-product :: editProductForm";
    }
}
