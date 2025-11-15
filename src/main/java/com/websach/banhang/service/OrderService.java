package com.websach.banhang.service;

import com.websach.banhang.model.*;
import com.websach.banhang.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.websach.banhang.repository.CartItemRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private ProductService productService;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private CartItemRepository cartItemRepository;

    public java.util.List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }

    public void createOrder(User user, String paymentMethod) {
        // 🛒 Lấy giỏ hàng của user
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống!"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // 🧾 Tạo đơn hàng mới
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        order.setStatus(OrderStatus.CHUA_NHAN);

        double total = 0.0;

        // 💡 Duyệt qua từng CartItem
        for (CartItem cartItem : cart.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtOrderTime(cartItem.getProduct().getFinalPrice());
            total += item.getPriceAtOrderTime() * item.getQuantity();

            // Thêm item vào đơn hàng
            order.addItem(item);

            // Cập nhật tồn kho sản phẩm
            productService.decreaseQuantity(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        order.setTotalAmount(total);

        // 🗃️ Lưu đơn hàng và xóa giỏ hàng
        orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public void updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        order.setStatus(OrderStatus.valueOf(status.toUpperCase()));
        orderRepository.save(order);
    }

    // ⭐ PHƯƠNG THỨC MỚI: Hủy đơn hàng và hoàn lại tồn kho
    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // 1. Xác thực quyền sở hữu
        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền hủy đơn hàng này.");
        }

        // 2. Kiểm tra trạng thái: Chỉ cho hủy nếu CHUA_NHAN
        if (order.getStatus() != OrderStatus.CHUA_NHAN) {
            throw new RuntimeException("Đơn hàng đang ở trạng thái " + order.getStatus().toString() + " và không thể hủy.");
        }

        // 3. Hoàn lại số lượng sản phẩm vào tồn kho
        for (OrderItem item : order.getItems()) {
            // Tăng số lượng sản phẩm lên bằng số lượng trong đơn hàng
            productService.increaseQuantity(item.getProduct().getId(), item.getQuantity());
        }

        // 4. Cập nhật trạng thái đơn hàng thành DA_HUY
        order.setStatus(OrderStatus.DA_HUY);
        orderRepository.save(order);
    }

    // ⭐ PHƯƠNG THỨC MỚI: Xác nhận đã nhận hàng (cho user)
    @Transactional
    public void confirmReceived(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // 1. Xác thực quyền sở hữu
        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền xác nhận đơn hàng này.");
        }

        // 2. Chỉ cho phép xác nhận khi trạng thái là DA_GIAO
        if (order.getStatus() != OrderStatus.DA_GIAO) {
            throw new RuntimeException("Đơn hàng chưa ở trạng thái ĐÃ GIAO để xác nhận.");
        }

        // 3. Cập nhật trạng thái
        order.setStatus(OrderStatus.DA_NHAN);
        orderRepository.save(order);
    }

    /**
     * Tìm kiếm đơn hàng theo ID. Được sử dụng bởi OrderController cho các thao tác GET.
     * @param id ID của Order
     * @return Optional<Order>
     */
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    // ⭐ PHƯƠNG THỨC MỚI: Sửa đơn hàng
    @Transactional
    public void editOrder(
            Long orderId,
            User user,
            List<Long> productIds,
            List<Integer> quantities,
            String newPaymentMethod) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // 1. Xác thực quyền sở hữu và Trạng thái
        if (!order.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền sửa đơn hàng này.");
        }
        if (order.getStatus() != OrderStatus.CHUA_NHAN) {
            throw new RuntimeException("Chỉ có thể sửa đơn hàng ở trạng thái CHỜ XÁC NHẬN.");
        }

        // 2. Hoàn lại tồn kho cho các sản phẩm cũ (Trước khi xóa items)
        for (OrderItem oldItem : order.getItems()) {
            productService.increaseQuantity(oldItem.getProduct().getId(), oldItem.getQuantity());
        }

        // 3. Xóa tất cả OrderItem cũ
        order.getItems().clear(); // Xóa khỏi danh sách trong Order entity
        // ⚠️ Lưu ý: Vì OrderItem có orphanRemoval=true, việc clear list này sẽ xóa các item trong DB khi save Order

        // 4. Cập nhật Phương thức Thanh toán
        order.setPaymentMethod(PaymentMethod.valueOf(newPaymentMethod.toUpperCase()));

        double newTotal = 0.0;

        // 5. Thêm các OrderItem mới
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            int quantity = quantities.get(i);

            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm có ID: " + productId));

            // ⭐ Kiểm tra tồn kho mới (chỉ cần kiểm tra quantity <= tồn kho thực tế, vì ta đã hoàn lại kho ở bước 2)
            if (quantity > product.getQuantity()) {
                // Nếu không đủ, ta ném lỗi. (Quá trình Transaction sẽ rollback toàn bộ)
                throw new RuntimeException("Số lượng mới (" + quantity + ") vượt quá tồn kho hiện có (" + product.getQuantity() + ") cho sản phẩm: " + product.getName());
            }

            // Tạo OrderItem mới
            OrderItem newItem = new OrderItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPriceAtOrderTime(product.getFinalPrice());
            order.addItem(newItem);
            newTotal += newItem.getSubtotal();

            // Trừ tồn kho mới
            productService.decreaseQuantity(productId, quantity);
        }

        // 6. Cập nhật Tổng tiền và Lưu Order
        order.setTotalAmount(newTotal);
        orderRepository.save(order);
    }

    // Lấy tất cả đơn hàng (cho Admin)
    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    // Admin hủy đơn hàng (hoàn lại tồn kho)
    @Transactional
    public void adminCancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng."));

        // Chỉ cho hủy nếu trạng thái chưa là DA_HUY (Hoặc theo quy tắc nghiệp vụ của bạn)
        if (order.getStatus() == OrderStatus.DA_HUY) {
            throw new RuntimeException("Đơn hàng đã bị hủy trước đó.");
        }

        // Hoàn lại số lượng sản phẩm vào tồn kho
        for (OrderItem item : order.getItems()) {
            productService.increaseQuantity(item.getProduct().getId(), item.getQuantity());
        }

        // Cập nhật trạng thái đơn hàng thành DA_HUY
        order.setStatus(OrderStatus.DA_HUY);
        orderRepository.save(order);
    }

    // ⭐ PHƯƠNG THỨC: Tính tổng doanh thu theo ngày/tháng/năm (Lọc DA_NHAN)
    public double getTotalRevenue(LocalDate startDate, LocalDate endDate) {
        return orderRepository.findAll().stream()
                .filter(order ->
                        order.getStatus() == OrderStatus.DA_NHAN && // Lọc theo DA_NHAN
                                order.getOrderDate().toLocalDate().isAfter(startDate.minusDays(1)) &&
                                order.getOrderDate().toLocalDate().isBefore(endDate.plusDays(1)))
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    // ⭐ PHƯƠNG THỨC: Tính doanh thu theo tháng (Lọc DA_NHAN)
    public List<Double> getMonthlyRevenue() {
        LocalDate now = LocalDate.now();
        Map<Integer, Double> monthlyData = new LinkedHashMap<>();

        for (int i = 0; i < 12; i++) {
            monthlyData.put(i, 0.0);
        }

        orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.DA_NHAN) // Lọc theo DA_NHAN
                .forEach(order -> {
                    int monthIndex = order.getOrderDate().getMonthValue() - 1;
                    int year = order.getOrderDate().getYear();

                    if (year == now.getYear()) {
                        monthlyData.merge(monthIndex, order.getTotalAmount(), Double::sum);
                    }
                });

        return new java.util.ArrayList<>(monthlyData.values());
    }

    // ⭐ PHƯƠNG THỨC: Lấy Top 10 Best Sellers (Lọc DA_NHAN)
    public List<Map<String, Object>> getTop10BestSellers() {
        Map<Long, Integer> salesCount = new HashMap<>();

        orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.DA_NHAN) // Lọc theo DA_NHAN
                .flatMap(order -> order.getItems().stream())
                .forEach(item -> salesCount.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum));

        List<Map.Entry<Long, Integer>> topEntries = salesCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        List<Map<String, Object>> topProductsData = topEntries.stream()
                .map(entry -> {
                    Product product = productService.getProductById(entry.getKey()).orElse(null);
                    if (product == null) return null;

                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("product", product);
                    data.put("salesCount", entry.getValue());
                    return data;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return topProductsData;
    }


    // ⭐ PHƯƠNG THỨC MỚI: Tạo đơn hàng từ các CartItem được chọn
    @Transactional
    public void createOrderFromSelectedItems(User user, String paymentMethod, String selectedItemIds) {

        if (selectedItemIds == null || selectedItemIds.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm để thanh toán.");
        }

        // 1. Phân tích chuỗi ID thành List<Long>
        List<Long> selectedIds = Arrays.stream(selectedItemIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 2. Lấy các CartItem dựa trên ID
        List<CartItem> checkedItems = cartItemRepository.findAllById(selectedIds).stream()
                // ⭐ Xác thực quyền sở hữu: Đảm bảo các item này thuộc về user hiện tại
                .filter(item -> item.getCart().getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        if (checkedItems.isEmpty()) {
            throw new RuntimeException("Không tìm thấy sản phẩm hợp lệ trong giỏ hàng để thanh toán.");
        }

        // 3. Tạo Order mới
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        order.setStatus(OrderStatus.CHUA_NHAN);

        double total = 0.0;

        // 4. Duyệt qua TỪNG CartItem ĐƯỢC CHỌN, tạo OrderItem, và giảm tồn kho
        for (CartItem cartItem : checkedItems) {

            // Kiểm tra tồn kho lần cuối
            if (cartItem.getProduct().getQuantity() < cartItem.getQuantity()) {
                // Nếu không đủ tồn kho, rollback toàn bộ transaction và báo lỗi
                throw new RuntimeException("Sản phẩm '" + cartItem.getProduct().getName() + "' không đủ số lượng tồn kho.");
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPriceAtOrderTime(cartItem.getProduct().getFinalPrice());
            total += item.getSubtotal();

            order.addItem(item);

            // Giảm tồn kho
            productService.decreaseQuantity(cartItem.getProduct().getId(), cartItem.getQuantity());
        }

        order.setTotalAmount(total);

        // 5. Lưu đơn hàng
        orderRepository.save(order);

        // 6. ⭐ XÓA CÁC CART ITEM ĐÃ THANH TOÁN (RẤT QUAN TRỌNG)
        cartItemRepository.deleteAll(checkedItems);

        // Lưu ý: Không cần cartRepository.save(cart) nếu chỉ xóa items, vì đã dùng deleteAll(checkedItems)
    }
}
