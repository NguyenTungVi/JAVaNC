package com.websach.banhang.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Tên tác giả không được để trống")
    private String author;

    // ⚙️ Thể loại: Quan hệ Many-to-One với Entity Category
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false) // Khóa ngoại trỏ đến bảng categories
    private Category category;

    @Column(nullable = false)
    @Positive(message = "Giá phải lớn hơn 0")
    private Double price;

    @Column
    @PositiveOrZero(message = "Giá giảm không được âm")
    private Double discountPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer quantity;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    @Column(length = 20, nullable = false)
    private String status;

    @PrePersist
    @PreUpdate
    public void updateStatus() {
        if (this.quantity == null || this.quantity <= 0) {
            this.status = "Hết hàng";
        } else {
            this.status = "Còn hàng";
        }
    }

//    public Double getFinalPrice() {
//        if (discountPrice != null && discountPrice > 0) {
//            return price - discountPrice; // giá sau khi giảm
//        }
//        return price; // nếu không có giảm thì giữ nguyên
//    }

    public Double getFinalPrice() {
        if (discountPrice != null && discountPrice > 0) {
            // Nếu giá giảm lớn hơn giá gốc, trả về giá gốc
            return Math.max(price - discountPrice, 0.0);
        }
        return price;
    }
}
