package com.websach.banhang.controller;

import com.websach.banhang.model.CartItem;
import com.websach.banhang.model.User;
import com.websach.banhang.model.Product;
import com.websach.banhang.repository.CartItemRepository;
import com.websach.banhang.service.CartService;
import com.websach.banhang.service.OrderService;
import com.websach.banhang.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartItemRepository cartItemRepository;

    // ================================
    // 🛒 HIỂN THỊ GIỎ HÀNG
    // Endpoint: /cart -> Chuyển hướng đến tab Giỏ hàng trong trang Profile
    // ================================
    @GetMapping
    public String viewCart(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem giỏ hàng.");
            return "redirect:/user/signin";
        }

        // Thay vì trả về "user/cart" (không khớp với cấu trúc template user.html của bạn),
        // Chúng ta chuyển hướng đến User Profile và kích hoạt tab "cart".
        return "redirect:/user/profile?tab=cart";
    }

    // ================================
    // ➕ THÊM VÀO GIỎ HÀNG (AJAX - Không chuyển hướng)
    // Endpoint: /cart/add/{productId}?qty=...
    // ================================
    @GetMapping("/add/{productId}")
    @ResponseBody // ⬅️ QUAN TRỌNG: Trả về JSON cho AJAX
    public ResponseEntity<Map<String, Object>> addToCart(@PathVariable Long productId,
                                                         @RequestParam(value = "qty", defaultValue = "1") int quantity,
                                                         HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            // Trả về lỗi 401 Unauthorized nếu chưa đăng nhập
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Vui lòng đăng nhập.");
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }

        Optional<Product> productOpt = productService.getProductById(productId);

        if (productOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không tìm thấy sản phẩm.");
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        Product product = productOpt.get();

        try {
            // 1. Thêm/Cập nhật vào Giỏ hàng
            cartService.addToCart(user, product, quantity);

            // 2. Cập nhật biến đếm mới
            int newCartCount = cartService.countCartItems(user);
            session.setAttribute("cartItemCount", newCartCount);

            // 3. Trả về JSON thành công
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Đã thêm " + product.getName() + " vào giỏ hàng!");
            response.put("cartCount", newCartCount);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RuntimeException e) {
            // Bắt lỗi RuntimeException từ CartService (Lỗi tồn kho)
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Lỗi server khi thêm vào giỏ: " + e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ================================
    // ⭐ API MỚI: LẤY SỐ LƯỢNG GIỎ HÀNG (Cho AJAX cập nhật Header) ⭐
    // Endpoint: /cart/api/cart-count
    // ================================
    @GetMapping("/api/cart-count")
    @ResponseBody
    public Map<String, Integer> getCartItemCount(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        int count = 0;
        if (user != null) {
            count = cartService.countCartItems(user);
        }

        Map<String, Integer> response = new HashMap<>();
        response.put("count", count);
        return response; // Trả về JSON: {"count": N}
    }

    // ================================
    // ❌ XÓA SẢN PHẨM KHỎI GIỎ
    // Endpoint: /cart/remove/{itemId}
    // ================================
    @GetMapping("/remove/{itemId}")
    public String removeItem(@PathVariable Long itemId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/user/signin";

        try {
            // ⭐ Lấy tên sản phẩm để hiển thị trong thông báo
            String productName = cartItemRepository.findById(itemId)
                    .map(item -> item.getProduct().getName())
                    .orElse("sản phẩm");

            cartService.removeFromCart(itemId, user);
            session.setAttribute("cartItemCount", cartService.countCartItems(user));
            // ⭐ Thêm flag cho JS để hiển thị thông báo
            redirectAttributes.addFlashAttribute("showSuccessAlert", true);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa " + productName + " khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        // Quay lại tab giỏ hàng
        return "redirect:/user/profile?tab=cart";
    }

    // ================================
    // ⭐ API MỚI: CẬP NHẬT SỐ LƯỢNG (AJAX - Không chuyển hướng)
    // Endpoint: /cart/api/update/{itemId}?quantity=...
    // ================================
        @PostMapping("/api/update/{itemId}")
        @ResponseBody
        public ResponseEntity<Map<String, Object>> updateQuantityAjax(@PathVariable Long itemId,
                                                                      @RequestParam int quantity,
                                                                      HttpSession session) {
            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Vui lòng đăng nhập.");
                return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
            }

            try {
                // 1. Cập nhật số lượng (logic kiểm tra tồn kho nằm trong Service)
                cartService.updateQuantity(itemId, quantity, user);

                // 2. Tính toán lại tổng tiền giỏ hàng (nếu cần cập nhật header)
                session.setAttribute("cartItemCount", cartService.countCartItems(user));

                // 3. Lấy thông tin cần thiết để cập nhật giao diện
                // Lấy lại CartItem đã được cập nhật
                Optional<CartItem> updatedItemOpt = cartItemRepository.findById(itemId);
                if (updatedItemOpt.isEmpty()) {
                    throw new RuntimeException("Cập nhật thành công nhưng không tìm thấy Item.");
                }
                CartItem updatedItem = updatedItemOpt.get();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("itemId", itemId);
                // Dùng subtotal mới để JS cập nhật trên UI
                response.put("newSubtotal", updatedItem.getSubtotal());

                // ⭐ Thêm tổng tiền của TẤT CẢ items (chưa chọn) để cập nhật tổng tiền lớn
                double totalCart = cartService.getTotal(user);
                response.put("totalCart", totalCart);

                return new ResponseEntity<>(response, HttpStatus.OK);

            } catch (SecurityException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Lỗi bảo mật: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            } catch (RuntimeException e) {
                // Bao gồm lỗi tồn kho và không tìm thấy item
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            } catch (Exception e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Lỗi server: " + e.getMessage());
                return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


    // ================================
    // 💳 TẠO ĐƠN HÀNG (CHECKOUT)
    // ================================
        @PostMapping("/checkout")
        public String checkout(
                @RequestParam String paymentMethod,
                @RequestParam String selectedItemIds, // ⭐ THÊM: Nhận chuỗi ID đã chọn
                HttpSession session,
                RedirectAttributes redirectAttributes) {

            User user = (User) session.getAttribute("loggedInUser");
            if (user == null) return "redirect:/user/signin";

            try {
                // ⭐ SỬA: Truyền danh sách ID đã chọn vào Service
                orderService.createOrderFromSelectedItems(user, paymentMethod, selectedItemIds);

                // Cập nhật lại số lượng giỏ hàng trên session
                session.setAttribute("cartItemCount", cartService.countCartItems(user));

                redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Đơn hàng đang chờ xác nhận.");
            } catch (RuntimeException e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi đặt hàng: " + e.getMessage());
            }

            // Chuyển hướng về trang profile và kích hoạt tab 'orders'
            return "redirect:/user/profile?tab=orders";
        }
}