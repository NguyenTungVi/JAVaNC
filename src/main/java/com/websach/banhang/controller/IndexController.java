package com.websach.banhang.controller;

import com.websach.banhang.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/")
    public String home(Model model) {
        // Lấy Top 10 cho trang home
        List<Map<String, Object>> bestSellers = orderService.getTop10BestSellers();

        // Nếu muốn chỉ hiển thị 6 sản phẩm ở home, giới hạn lại
        if (bestSellers.size() > 4) {
            bestSellers = bestSellers.subList(0, 4);
        }
        // Sử dụng cùng logic như trang best-sellers
        model.addAttribute("bestSellers", orderService.getTop10BestSellers());
        return "home";
    }

    @GetMapping("/signin")
    public String signin() {
        return "signin";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}