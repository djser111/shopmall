package com.atguigu.gulimall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.common.to.SocialUserTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.MemberFeignService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class Oauth2Controller {
    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession session) throws IOException {
        Map<String, String> map = new HashMap<>();
        String url = "https://gitee.com/oauth/token?grant_type=authorization_code&client_id=075073f0422141a911f0b8fb789245b09cfd79f6d06e0fb5d440a373b6c7dc52&redirect_uri=http://auth.gulimall.com/oauth2.0/gitee/success&client_secret=777a8399cd7b9c44dd11cde2863692a8701b63024ffe677e3d04be8827ae9a2a&code=" + code;
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpPost(url));
        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            System.out.println("token:" + json);
            SocialUserTo socialUserTo = JSON.parseObject(json, SocialUserTo.class);
            String access_token = socialUserTo.getAccess_token();

            //获取用户uid
            String urluser = "https://gitee.com/api/v5/user?access_token=" + access_token;
            HttpClient httpClientUser = HttpClientBuilder.create().build();
            HttpGet httpPostUser = new HttpGet(urluser);           //记得用httpGet请求，否则会405拒绝请求
            HttpResponse responseUser = httpClientUser.execute(httpPostUser);
            String userJson = EntityUtils.toString(responseUser.getEntity());
            System.out.println(userJson);

            JSONObject jsonObject = JSON.parseObject(userJson);
            socialUserTo.setUid(jsonObject.getString("id"));
            socialUserTo.setAvatar_url(jsonObject.getString("avatar_url"));
            socialUserTo.setName(jsonObject.getString("name"));
            System.out.println(socialUserTo.toString());
            R oauthLogin = memberFeignService.oauthLogin(socialUserTo);
            if (oauthLogin.getCode() == 0) {
                MemberRespVo data = oauthLogin.getData(new TypeReference<MemberRespVo>() {
                });
                session.setAttribute("loginUser", data);
                return "redirect:http://gulimall.com";
            } else {
                return "redirect:http://auth.gulimall.com";
            }
        }
        return "redirect:http://auth.gulimall.com";
    }
}
