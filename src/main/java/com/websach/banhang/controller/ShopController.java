package com.websach.banhang.controller;

import com.websach.banhang.model.Category;
import com.websach.banhang.model.Product;
import com.websach.banhang.service.OrderService;
import com.websach.banhang.service.ProductService;
import com.websach.banhang.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // ⬅️ IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ShopController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    /**
     * Hiển thị trang Shop cho khách hàng (có phân trang).
     */
    @GetMapping("/shop")
    public String showShopPage(
            @RequestParam(value = "category", required = false) String categoryName,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page, // ⬅️ Lấy số trang
            Model model) {

        final int pageSize = 12; // ⬅️ 12 sản phẩm mỗi trang

        // 1. Lấy tất cả Categories
        List<Category> allCategories = categoryService.findAllCategories();
        model.addAttribute("allCategories", allCategories);

        // 2. Gọi phương thức searchAndFilterProducts (PHÂN TRANG)
        Page<Product> productsPage = productService.searchAndFilterProducts(keyword, categoryName, page, pageSize);

        // 3. Truyền Page object vào model (tên biến là "products")
        model.addAttribute("products", productsPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryName", categoryName);

        // Trả về template shop.html
        return "shop";
    }


    @GetMapping("/products/detail/{id}")
    public String showProductDetailPage(@PathVariable Long id, Model model) {
        // 1. Lấy Product Entity từ Service
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // 2. Đưa Product vào Model
        model.addAttribute("product", product);

        // 3. Trả về tên file view mới
        return "fragments/product-detail"; // ⭐ Đảm bảo tên file này khớp với tên file mới ⭐
    }

    @Autowired
    private OrderService orderService; // Đã sửa đổi để tính DA_NHAN

    // ⭐ Phương thức mới để hiển thị Top 10 Bán chạy nhất ⭐
    @GetMapping("/best-sellers")
    public String showBestSellers(Model model) {
        // Gọi phương thức từ OrderService để lấy danh sách Best Sellers
        // Giả định orderService.getTop10BestSellers() trả về List<Map<String, Object>>
        model.addAttribute("bestSellers", orderService.getTop10BestSellers());
        return "best-sell"; // Trả về template best-sell.html
    }
}