package com.atguigu.gulimall.authserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.to.MemberRegisterTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.authserver.feign.MemberFeignService;
import com.atguigu.gulimall.authserver.feign.ThirdPartyFeignService;
import com.atguigu.common.to.UserLoginTo;
import com.atguigu.gulimall.authserver.vo.UserRegisterVo;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Controller
public class LoginController {
    @Autowired
    private ThirdPartyFeignService thirdPartyFeignService;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    @GetMapping("sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (s != null) {
            long l = Long.parseLong(s.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }

        String code = String.valueOf(RandomUtils.nextInt(100000, 999999)) + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code, 10, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone, code.split("_")[0]);

        return R.ok();
    }

    @PostMapping("/regist")
    public String register(@Valid UserRegisterVo userRegisterVo, BindingResult result, RedirectAttributesModelMap redirectAttributesModelMap) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            redirectAttributesModelMap.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
        String code = userRegisterVo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisterVo.getPhone());
        if (!StringUtils.isEmpty(s)) {
            if (code.equals(s.split("_")[0])) {
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + userRegisterVo.getPhone());
                MemberRegisterTo memberRegisterTo = new MemberRegisterTo();
                BeanUtils.copyProperties(userRegisterVo, memberRegisterTo);
                R r = memberFeignService.register(memberRegisterTo);
                if (r.getCode() == 0) {
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", r.getData("msg", new TypeReference<String>() {
                    }));
                    redirectAttributesModelMap.addFlashAttribute(errors);
                    return "redirect:http://auth.gulimall.com/register.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributesModelMap.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/register.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributesModelMap.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/register.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginTo userLoginTo, RedirectAttributes redirectAttributes, HttpSession session) {
        R login = memberFeignService.login(userLoginTo);
        if (login.getCode() == 0) {
//            login.getData(new TypeReference<>())
            session.setAttribute(AuthServerConstant.LOGIN_USER, login.getData(new TypeReference<MemberRespVo>() {
            }));
            return "redirect:http://gulimall.com";
        }
        Map<String, String> errors = new HashMap<>();
        errors.put("msg", login.getData("msg", new TypeReference<String>() {
        }));
        redirectAttributes.addFlashAttribute("errors", errors);
        return "redirect:http://auth.gulimall.com/login.html";
    }

    @GetMapping({"/", "login.html"})
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "login";
        }
        return "redirect:http://gulimall.com";
    }
}

