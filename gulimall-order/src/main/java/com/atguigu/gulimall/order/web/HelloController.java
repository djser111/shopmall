package com.atguigu.gulimall.order.web;

import com.atguigu.common.constant.AuthServerConstant;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;

@Controller
public class HelloController {
    @GetMapping("/confirm.html")
    public String confirm() {
        return "confirm";
    }

    @GetMapping("/detail.html")
    public String detail(HttpSession session) {
        System.out.println(session.getAttribute(AuthServerConstant.LOGIN_USER));
        return "detail";
    }

    @GetMapping("/list.html")
    public String list() {
        return "list";
    }

    @GetMapping("/pay.html")
    public String pay() {
        return "pay";
    }
}
