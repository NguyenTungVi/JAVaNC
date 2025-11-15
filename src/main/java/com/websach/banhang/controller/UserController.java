package com.websach.banhang.controller;

import com.websach.banhang.model.Product;
import com.websach.banhang.model.User;
import com.websach.banhang.model.Cart;
import com.websach.banhang.model.CartItem;
import com.websach.banhang.service.CategoryService;
import com.websach.banhang.service.UserService;
import com.websach.banhang.service.ProductService;
import com.websach.banhang.service.CartService;
import com.websach.banhang.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    // ================================
    // 🧩 TRANG TĨNH
    // ================================

    @GetMapping("/signin")
    public String showSignInPage() {
        return "signin";
    }

    @GetMapping("/signup")
    public String showSignUpPage(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "signup";
    }

    // ================================
    // 👤 ĐĂNG KÝ NGƯỜI DÙNG
    // ================================

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute("user") User user,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               @RequestParam("emailPrefix") String emailPrefix,
                               @RequestParam(value = "role", required = false) String requestedRole,
                               RedirectAttributes redirectAttributes) {

        if (emailPrefix == null || emailPrefix.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("user", user);
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập tên tài khoản Email.");
            return "redirect:/user/signup";
        }

        String fullEmail = emailPrefix.trim() + "@gmail.com";
        user.setEmail(fullEmail);

        // Đảm bảo vai trò được thiết lập, ưu tiên vai trò yêu cầu, sau đó là default trong Entity, cuối cùng là "USER"
        String finalRole = "USER";
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            finalRole = user.getRole();
        }
        if (requestedRole != null && !requestedRole.trim().isEmpty()) {
            finalRole = requestedRole.trim().toUpperCase();
        }
        user.setRole(finalRole);

        if (userService.isEmailTaken(user.getEmail())) {
            redirectAttributes.addFlashAttribute("user", user);
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại! Vui lòng dùng email khác.");
            return "redirect:/user/signup";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                user.setImage(imageFile.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                user.setImage(null);
                redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu trữ file ảnh.");
                return "redirect:/user/signup";
            }
        } else {
            user.setImage(null);
        }

        userService.registerUser(user);
        redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
        return "redirect:/user/signin";
    }

    // ================================
    // 🔐 ĐĂNG NHẬP / ĐĂNG XUẤT
    // ================================

    @PostMapping("/signin")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        User existingUser = userService.authenticateUser(email, password);

        if (existingUser != null) {
            existingUser.setImage(null);
            session.setAttribute("loggedInUser", existingUser);
            return "redirect:/home";
        } else {
            redirectAttributes.addFlashAttribute("error", "Email hoặc mật khẩu không đúng!");
            return "redirect:/user/signin";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Bạn đã đăng xuất thành công!");
        return "redirect:/home";
    }

    // ================================
    // 🏠 TRANG PROFILE / DASHBOARD
    // ================================

    @GetMapping("/profile")
    public String showUserProfile(@RequestParam(value = "tab", required = false) String tab,
                                  @RequestParam(value = "keyword", required = false) String keyword,
                                  @RequestParam(value = "category", required = false) String category,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loggedInUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để truy cập trang này.");
            return "redirect:/user/signin";
        }

        User user = (User) session.getAttribute("loggedInUser");
        User fullUser = userService.findById(user.getId()).orElse(user);

        // Khởi tạo các giá trị lọc/tab
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("currentKeyword", keyword);
        model.addAttribute("currentCategory", category);

        // 🌟 Nếu tab không được chỉ định (lần đầu truy cập), đặt mặc định là 'profile'
        if (tab == null || tab.isEmpty()) {
            tab = "profile";
        }

        // 🌟 Nạp dữ liệu cho các tab
        if ("ADMIN".equals(user.getRole())) {

            LocalDate today = LocalDate.now();

            // ⭐ Dữ liệu chung cho ADMIN (Quản lý User, Thể loại, Orders) ⭐
            model.addAttribute("allUsers", userService.findAllUsers());
            model.addAttribute("allOrders", orderService.findAllOrders());

            // THỐNG KÊ DOANH THU & BEST SELLERS
            model.addAttribute("todayRevenue", orderService.getTotalRevenue(today, today));
            model.addAttribute("monthRevenue", orderService.getTotalRevenue(today.withDayOfMonth(1), today));
            model.addAttribute("yearRevenue", orderService.getTotalRevenue(today.withDayOfYear(1), today));
            model.addAttribute("monthlyRevenue", orderService.getMonthlyRevenue());
            model.addAttribute("bestSellers", orderService.getTop10BestSellers());

            // ⭐ QUẢN LÝ SẢN PHẨM (admin-products) ⭐
            List<Product> filteredProducts;
            if ("admin-products".equals(tab)) {
                filteredProducts = productService.searchAndFilterProducts(keyword, category);
            } else {
                filteredProducts = productService.getAllProducts();
            }

            model.addAttribute("allProducts", productService.getAllProducts());
            model.addAttribute("totalProductsCount", productService.getAllProducts().size());


            if ("admin-orders".equals(tab)) {
                // Dữ liệu đã được nạp ở trên, không cần nạp lại, chỉ cần đặt đúng model attribute
                model.addAttribute("orders", orderService.findAllOrders());
            }

            model.addAttribute("allProducts", filteredProducts);
            model.addAttribute("totalProductsCount", filteredProducts.size());

        } else if ("USER".equals(user.getRole())) {
            // Dữ liệu cho USER
            // ⭐⭐⭐ XỬ LÝ TAB GIỎ HÀNG (cart) ⭐⭐⭐
                try {
                    // Lấy giỏ hàng của người dùng hiện tại
                    Cart userCart = cartService.getUserCart(fullUser);

                    // Lấy Cart Items và tính tổng tiền
                    List<CartItem> cartItems = userCart.getItems();
                    double total = cartService.getTotal(fullUser);

                    model.addAttribute("cartItems", cartItems);
                    model.addAttribute("total", total);

                } catch (Exception e) {
                    System.err.println("Lỗi khi tải giỏ hàng: " + e.getMessage());
                    model.addAttribute("cartItems", Collections.emptyList());
                    model.addAttribute("total", 0.0);
                }

            model.addAttribute("orders", orderService.getUserOrders(fullUser));
            model.addAttribute("userOrders", Collections.emptyList());
            model.addAttribute("boughtBooks", Collections.emptyList());
        }

        // 🌟 Giữ đúng tab khi reload
        model.addAttribute("activeTab", tab);


        return "user";
    }

    // ================================
    // 🖼️ ẢNH ĐẠI DIỆN
    // ================================

    @GetMapping("/avatar/{id}")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Long id) {
        User user = userService.findUserById(id);

        if (user != null && user.getImage() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(user.getImage(), headers, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ================================
    // 🖼️ ẢNH SẢN PHẨM (DÙNG CHO TRANG SHOP)
    // Endpoint: /products/image/{id}
    // ================================
    @GetMapping("/products/image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getProductImageForShop(@PathVariable Long id) {
        byte[] imageBytes = productService.getProductById(id)
                .map(Product::getImage)
                .orElse(null);

        if (imageBytes != null && imageBytes.length > 0) {
            HttpHeaders headers = new HttpHeaders();
            // Đặt MediaType cho ảnh
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } else {
            // Trả về NOT_FOUND (404) nếu không có ảnh
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ================================
    // ✏️ CẬP NHẬT THÔNG TIN NGƯỜI DÙNG
    // ================================

    @PostMapping("/update")
    public String updateUserProfile(@RequestParam(value = "firstName", required = false) String firstName,
                                    @RequestParam(value = "lastName", required = false) String lastName,
                                    @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để tiếp tục.");
            return "redirect:/user/signin";
        }

        try {
            // 🔹 Lấy user thật từ DB
            User userFromDb = userService.findById(loggedUser.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng trong cơ sở dữ liệu"));

            // 🌟 Cập nhật thông tin nếu có dữ liệu mới
            if (firstName != null && !firstName.trim().isEmpty()) {
                userFromDb.setFirstName(firstName.trim());
            }
            if (lastName != null && !lastName.trim().isEmpty()) {
                userFromDb.setLastName(lastName.trim());
            }

            // 🌟 Nếu người dùng chọn ảnh mới → cập nhật ảnh
            if (imageFile != null && !imageFile.isEmpty()) {
                userFromDb.setImage(imageFile.getBytes());
            }

            // 🌟 Lưu vào DB
            userService.saveUser(userFromDb);

            // 🌟 Làm mới session (không lưu blob ảnh)
            User userForSession = new User();
            userForSession.setId(userFromDb.getId());
            userForSession.setEmail(userFromDb.getEmail());
            userForSession.setFirstName(userFromDb.getFirstName());
            userForSession.setLastName(userFromDb.getLastName());
            userForSession.setRole(userFromDb.getRole());
            userForSession.setImage(null);
            session.setAttribute("loggedInUser", userForSession);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật thông tin: " + e.getMessage());
        }


        return "redirect:/user/profile?tab=profile";
    }

    // ================================
    // ⭐ ADMIN: QUẢN LÝ USER ⭐
    // ================================

    // Cập nhật Role cho User
    @PostMapping("/admin/users/role")
    public String updateUserRole(@RequestParam("userId") Long userId,
                                 @RequestParam("role") String newRole,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null || !"ADMIN".equalsIgnoreCase(loggedUser.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập chức năng này.");
            return "redirect:/user/profile";
        }

        try {
            // ⭐ Giữ nguyên Logic chặn Admin tự hạ cấp Role (nếu bạn muốn chặn) ⭐
            if (loggedUser.getId().equals(userId) && !newRole.equalsIgnoreCase("ADMIN")) {
                redirectAttributes.addFlashAttribute("error", "Không thể tự hạ cấp vai trò của bản thân.");
            } else {
                userService.updateUserRole(userId, newRole);
                redirectAttributes.addFlashAttribute("success", "Cập nhật vai trò thành công!");

                // ⭐ QUAN TRỌNG: Nếu Admin thay đổi Role của chính mình thành ADMIN, làm mới session.
                if (loggedUser.getId().equals(userId) && newRole.equalsIgnoreCase("ADMIN")) {
                    // Cập nhật session nếu Admin đang thao tác trên chính tài khoản của mình
                    User updatedUser = userService.findUserById(userId);
                    if (updatedUser != null) {
                        updatedUser.setImage(null);
                        session.setAttribute("loggedInUser", updatedUser);
                    }
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        // Redirect về tab quản lý user
        return "redirect:/user/profile?tab=admin-users";
    }

    // Xóa User
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Long userId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        User loggedUser = (User) session.getAttribute("loggedInUser");
        if (loggedUser == null || !"ADMIN".equalsIgnoreCase(loggedUser.getRole())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập chức năng này.");
            return "redirect:/user/profile";
        }

        try {
            // Không cho admin xóa chính tài khoản của mình
            if (loggedUser.getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Không thể tự xóa tài khoản của bản thân.");
            } else {
                userService.deleteUser(userId);
                redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa người dùng: " + e.getMessage());
        }

        // Redirect về tab quản lý user
        return "redirect:/user/profile?tab=admin-users";
    }

}
