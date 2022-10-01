package com.atguigu.gulimall.order.web;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderWebController {
    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo orderSubmitVo, Model model, RedirectAttributes redirectAttributes) {
        SubmitOrderRespVo respVo = null;
        try {
            respVo = orderService.submitOrder(orderSubmitVo);
            if (respVo.getCode() == 0) {
                model.addAttribute("submitOrderResp", respVo);
                return "pay";
            }
            String msg = "下单失败;";
            switch (respVo.getCode()) {
                case 1:
                    msg += "订单信息过期，请刷新后再次提交";
                    break;
                case 2:
                    msg += "订单商品价格发生变化，请确认后再次提交";
                    break;
            }
            redirectAttributes.addFlashAttribute("msg", msg);
        } catch (Exception e) {
            if (e instanceof NoStockException) {
                String message = ((NoStockException) e).getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.gulimall.com/toTrade";
        }
        return "redirect:http://order.gulimall.com/toTrade";
    }
}
