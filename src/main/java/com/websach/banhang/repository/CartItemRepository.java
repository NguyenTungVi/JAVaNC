package com.websach.banhang.repository;

import com.websach.banhang.model.CartItem;
import com.websach.banhang.model.Cart;
import com.websach.banhang.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteByCart(Cart cart);
}
