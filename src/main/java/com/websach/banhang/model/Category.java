package com.websach.banhang.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories") // Bảng mới trong DB
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    // Tên thể loại (Ví dụ: ROMANCE, BUSINESS, ...)
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    // Constructor tiện lợi để thêm mới
    public Category(String name) {
        this.name = name;
    }
}