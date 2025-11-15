package com.websach.banhang.service;

import com.websach.banhang.model.Cart;
import com.websach.banhang.model.CartItem;
import com.websach.banhang.model.Product;
import com.websach.banhang.model.User;
import com.websach.banhang.repository.CartRepository;
import com.websach.banhang.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // ⬅️ Dùng cho các thao tác ghi

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductService productService;

    // 🔹 Lấy giỏ hàng của user (nếu chưa có thì tạo mới)
    public Cart getUserCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    // 🔹 Lấy tất cả sản phẩm trong giỏ
    public List<CartItem> getUserCartItems(User user) {
        return getUserCart(user).getItems();
    }

    // 🔹 Thêm sản phẩm vào giỏ
    @Transactional
    public void addToCart(User user, Product product, int quantity) {
        Cart cart = getUserCart(user);

        // 1. Tìm CartItem hiện tại
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndProduct(cart, product);

        // 2. Tính toán số lượng tổng mới
        int currentQuantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newTotalQuantity = currentQuantityInCart + quantity;

        // ⭐ 3. KIỂM TRA TỒN KHO: Đảm bảo tổng số lượng không vượt quá số lượng tồn kho thực tế
        if (newTotalQuantity > product.getQuantity()) {
            // product.getQuantity() là số lượng tồn kho thực tế
            throw new RuntimeException("Số lượng đặt (" + newTotalQuantity + ") vượt quá số lượng tồn kho hiện có (" + product.getQuantity() + ").");
        }

        // 4. Cập nhật hoặc thêm mới
        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();

            // Cập nhật số lượng tổng mới (existing + new_added)
            existingItem.setQuantity(newTotalQuantity);
            cartItemRepository.save(existingItem); //

        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart); //
            newItem.setProduct(product); //

            // Số lượng mới được thêm vào (vì currentQuantityInCart = 0)
            newItem.setQuantity(quantity);

            // Thêm vào list trong Cart entity và save Cart
            cart.addItem(newItem);
            cartRepository.save(cart);
        }
    }

    // ⭐ SỬA/THÊM: Xóa sản phẩm khỏi giỏ (Cần User để xác thực quyền)
    @Transactional
    public void removeFromCart(Long itemId, User user) {
        // 1. Tìm CartItem theo ID
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng."));

        // 2. Xác thực xem item này có thuộc về giỏ hàng của user hiện tại không
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền xóa sản phẩm này.");
        }

        // 3. Xóa
        cartItemRepository.delete(item);
    }

    // 🔹 Phiên bản cũ (đã loại bỏ vì thiếu xác thực): public void removeFromCart(Long itemId) { ... }

    // ⭐ SỬA/THÊM: Cập nhật số lượng sản phẩm (Cần User để xác thực quyền)
    @Transactional
    public void updateQuantity(Long itemId, int quantity, User user) {
        if (quantity <= 0) {
            // Nếu quantity là 0 hoặc âm, coi như yêu cầu xóa sản phẩm
            removeFromCart(itemId, user);
            return;
        }

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng."));

        // Xác thực quyền sở hữu
        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new SecurityException("Bạn không có quyền cập nhật giỏ hàng này.");
        }

        // ⭐ SỬA: Logic kiểm tra số lượng tồn kho chính xác
        Product product = item.getProduct();
        final int maxAllowableQuantity = product.getQuantity(); // <-- CHỈ LÀ TỒN KHO THỰC TẾ

        if (quantity > maxAllowableQuantity) {
            throw new RuntimeException("Số lượng đặt (" + quantity + ") vượt quá số lượng tồn kho hiện có (" + maxAllowableQuantity + ").");
        }

        // Cập nhật số lượng
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    // 🔹 Phiên bản cũ (đã loại bỏ vì thiếu xác thực): public void updateQuantity(Long itemId, int quantity) { ... }


    // 🔹 Tính tổng tiền giỏ hàng
    public double getTotal(User user) {
        return getUserCart(user).getTotal();
    }

    // ⭐ PHƯƠNG THỨC MỚI: Đếm tổng số lượng CartItems (Cần cho Header)
    public int countCartItems(User user) {
        // Lấy Cart và trả về số lượng items trong list
        return getUserCart(user).getItems().size();
    }


    // ⭐ PHƯƠNG THỨC MỚI: Tính tổng tiền của các CartItem được chọn
    public double getCheckedItemsTotal(List<Long> checkedItemIds, User user) {
        if (checkedItemIds == null || checkedItemIds.isEmpty()) {
            return 0.0;
        }

        // 1. Lấy tất cả CartItem dựa trên danh sách ID
        List<CartItem> checkedItems = cartItemRepository.findAllById(checkedItemIds);

        // 2. Xác thực quyền sở hữu (Đảm bảo các item này thuộc về giỏ hàng của user)
        Long userId = user.getId();
        checkedItems = checkedItems.stream()
                .filter(item -> item.getCart().getUser().getId().equals(userId))
                .collect(Collectors.toList());

        // 3. Tính tổng subtotal
        return checkedItems.stream()
                .mapToDouble(CartItem::getSubtotal)
                .sum();
    }
}