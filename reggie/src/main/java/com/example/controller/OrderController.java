package com.example.controller;


import com.example.common.R;
import com.example.entity.Orders;
import com.example.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrdersService ordersService;

    /**
     * 用户下单功能
     * @param orders
     * @return
     */
    @RequestMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){

        ordersService.submit(orders);

        return R.success("下单成功！");
    }

}
