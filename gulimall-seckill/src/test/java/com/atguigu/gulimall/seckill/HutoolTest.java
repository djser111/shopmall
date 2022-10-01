package com.atguigu.gulimall.seckill;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.junit.Test;

import java.util.Date;

public class HutoolTest {
    @Test
    public void test() {
        DateTime date = DateUtil.date();
        Date beginOfDay = DateUtil.beginOfDay(date);
        System.out.println(beginOfDay);

        //结果：2017-03-03 22:33:23
        Date newDate = DateUtil.offset(date, DateField.DAY_OF_MONTH, 2);
        System.out.println(newDate);
        Date endOfDay = DateUtil.endOfDay(newDate);
        System.out.println(endOfDay);

        //常用偏移，结果：2017-03-04 22:33:23
        DateTime newDate2 = DateUtil.offsetDay(date, 2);
        System.out.println(newDate2);
        //常用偏移，结果：2017-03-01 19:33:23
        DateTime newDate3 = DateUtil.offsetHour(date, -3);
    }
}
