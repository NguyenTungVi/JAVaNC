package com.websach.banhang.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {
    @GetMapping("/home")
    public String index() {
        // Đảm bảo rằng trang chủ thực sự là home.html
        return "home"; // hoặc "index" tùy thuộc vào tên file của bạn
    }

}

