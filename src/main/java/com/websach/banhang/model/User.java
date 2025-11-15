package com.websach.banhang.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // Bảng lưu thông tin người dùng
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    // Các trường khớp với form
    @Column(nullable = false, length = 50)
    @NotBlank(message = "Họ không được để trống")
    private String lastName; // Họ

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Tên không được để trống")
    private String firstName; // Tên

    @Column(nullable = false)
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    // Trường Email: KHÔNG CÒN @Email validation.
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email không được để trống")
    private String email;

    // Ảnh đại diện - THAY ĐỔI: Từ String thành byte[] để lưu BLOB
    @Lob  // Đánh dấu là Large Object (BLOB)
    @Column(columnDefinition = "LONGBLOB")  // Cho MySQL, lưu nhị phân
    private byte[] image;  // Dữ liệu nhị phân của ảnh

    // Vai trò và Trạng thái
    @Column(length = 20, nullable = false)
    private String role = "USER";

    @Column(nullable = false)
    private boolean enabled = true; // Trạng thái tài khoản (true = active)

    // Helper
    public String getFullName() {
        return firstName + " " + lastName;
    }


}