package com.websach.banhang.controller;

import com.websach.banhang.model.Order;
import com.websach.banhang.model.OrderStatus;
import com.websach.banhang.model.User;
import com.websach.banhang.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ================================
    // 📜 HIỂN THỊ DANH SÁCH ĐƠN HÀNG
    // Tối ưu hóa: Chuyển hướng người dùng đến tab 'orders' trong trang Profile
    // ================================
    @GetMapping
    public String listOrders(HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem đơn hàng.");
            return "redirect:/user/signin";
        }

        // Chuyển hướng về User Profile và kích hoạt tab "orders"
        return "redirect:/user/profile?tab=orders";
    }

    // ================================
    // 💳 TẠO ĐƠN HÀNG (CHECKOUT)
    // ================================
    @PostMapping("/checkout")
    public String checkout(@RequestParam String paymentMethod, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/user/signin";

        try {
            orderService.createOrder(user, paymentMethod);
            redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Đơn hàng đang chờ xác nhận.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đặt hàng: " + e.getMessage());
        }

        // Chuyển hướng về trang profile và kích hoạt tab 'orders'
        return "redirect:/user/profile?tab=orders";
    }

    // ================================
    // ⚙️ ADMIN CẬP NHẬT TRẠNG THÁI
    // ================================
    @PostMapping("/api/update-status/{id}")
    @ResponseBody // Trả về JSON
    public ResponseEntity<Map<String, Object>> updateStatusAjax(@PathVariable Long id, @RequestParam String status) {

        // ⭐ Bổ sung: Kiểm tra xác thực ADMIN ở đây nếu cần thiết (Tùy thuộc vào Security Config của bạn) ⭐
        // Ví dụ: if (!"ADMIN".equals(session.getAttribute("loggedInUser").getRole())) return ResponseEntity.status(403).build();

        Map<String, Object> response = new HashMap<>();
        try {
            orderService.updateOrderStatus(id, status);

            // Lấy tên trạng thái đã dịch để hiển thị trên UI (ví dụ: Chờ XN -> Chờ xác nhận)
            String displayStatus = status.toString().equals("CHUA_NHAN") ? "Chờ xác nhận" :
                    (status.toString().equals("DANG_GIAO") ? "Đang giao" :
                            (status.toString().equals("DA_GIAO") ? "Đã giao" :
                                    (status.toString().equals("DA_NHAN") ? "Đã nhận" : "Đã hủy")));

            response.put("success", true);
            response.put("newStatus", displayStatus);
            response.put("orderId", id);
            response.put("originalStatus", status);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ================================
    // ❌ HỦY ĐƠN HÀNG (USER)
    // ================================
    @GetMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/user/signin";

        try {
            orderService.cancelOrder(orderId, user);
            redirectAttributes.addFlashAttribute("success", "Đơn hàng #" + orderId + " đã được hủy thành công và tồn kho đã được hoàn lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi hủy đơn hàng: " + e.getMessage());
        }

        return "redirect:/user/profile?tab=orders"; // Quay về tab orders
    }

    // ================================
    // ✅ XÁC NHẬN ĐÃ NHẬN HÀNG (USER)
    // ================================
    @GetMapping("/confirm-received/{orderId}")
    public String confirmReceived(@PathVariable Long orderId, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/user/signin";

        try {
            orderService.confirmReceived(orderId, user);
            redirectAttributes.addFlashAttribute("success", "Đơn hàng #" + orderId + " đã được xác nhận ĐÃ NHẬN.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/user/profile?tab=orders"; // Quay về tab orders
    }


    // ================================
    // ⭐ GET: HIỂN THỊ FORM SỬA ĐƠN HÀNG (AJAX)
    // ================================
    @GetMapping("/edit-form/{orderId}")
    public String showEditOrderForm(@PathVariable Long orderId, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "error/403"; // Trả về lỗi nếu chưa đăng nhập

        Order order = orderService.findById(orderId) // ⭐ Cần thêm findById vào OrderService
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        if (!order.getUser().getId().equals(user.getId()) || order.getStatus() != OrderStatus.CHUA_NHAN) {
            // Ngăn truy cập nếu không phải chủ sở hữu hoặc trạng thái không hợp lệ
            return "error/403";
        }

        model.addAttribute("order", order);
        // Trả về fragment sửa đơn hàng (cần tạo fragments/edit-order-form.html)
        return "fragments/edit-order-form :: editOrderForm";
    }

    // ================================
    // ⭐ POST: LƯU SỬA ĐỔI ĐƠN HÀNG
    // ================================
    @PostMapping("/save-edit/{orderId}")
    public String saveEditOrder(
            @PathVariable Long orderId,
            @RequestParam List<Long> productIds,
            @RequestParam List<Integer> quantities,
            @RequestParam String paymentMethod,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/user/signin";

        try {
            // Lọc và chuẩn hóa dữ liệu (đảm bảo số lượng sản phẩm bằng số lượng quantity)
            if (productIds.size() != quantities.size() || productIds.isEmpty()) {
                throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ.");
            }

            // Xóa các items có quantity <= 0 nếu có (mặc dù form sẽ cố gắng ngăn điều này)
            List<Long> filteredProductIds = productIds.stream()
                    .filter(id -> quantities.get(productIds.indexOf(id)) > 0)
                    .collect(Collectors.toList());

            List<Integer> filteredQuantities = quantities.stream()
                    .filter(qty -> qty > 0)
                    .collect(Collectors.toList());


            orderService.editOrder(orderId, user, filteredProductIds, filteredQuantities, paymentMethod);
            redirectAttributes.addFlashAttribute("success", "Đơn hàng #" + orderId + " đã được cập nhật thành công.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi sửa đơn hàng: " + e.getMessage());
        }

        return "redirect:/user/profile?tab=orders";
    }

    // ================================
    // ❌ ADMIN HỦY ĐƠN HÀNG
    // ================================
    @GetMapping("/admin-cancel/{orderId}")
    public String adminCancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        // ⭐ Lưu ý: Cần thêm logic xác thực ADMIN ở đây
        try {
            orderService.adminCancelOrder(orderId);
            redirectAttributes.addFlashAttribute("success", "Admin đã hủy đơn hàng #" + orderId + ". Tồn kho đã được hoàn lại.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/user/profile?tab=admin-orders";
    }
}
