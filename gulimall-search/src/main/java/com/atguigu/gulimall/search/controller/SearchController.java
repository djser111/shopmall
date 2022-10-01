package com.atguigu.gulimall.search.controller;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParamVo;
import com.atguigu.gulimall.search.vo.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
    @Autowired
    private MallSearchService mallSearchService;

    @GetMapping({"/", "/list.html"})
    public String listPage(SearchParamVo searchParamVo, Model model, HttpServletRequest httpServletRequest) {
        searchParamVo.setQueryString(httpServletRequest.getQueryString());
        SearchResponseVo searchResponseVo = mallSearchService.search(searchParamVo);
        model.addAttribute("result", searchResponseVo);
        return "list";
    }

}
