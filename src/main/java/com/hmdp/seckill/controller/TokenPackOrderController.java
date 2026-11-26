package com.hmdp.seckill.controller;


import com.hmdp.common.dto.Result;
import com.hmdp.seckill.service.ITokenPackOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Accepts authenticated token-pack reservations and exposes owner-scoped order status.
 *
 * @author 虎哥
 */
@RestController
@RequestMapping({"/token-pack-orders", "/voucher-order"})
public class TokenPackOrderController {

    @Resource
    private ITokenPackOrderService orderService;

    @GetMapping("/{id}")
    public Result queryOrderStatus(@PathVariable("id") Long orderId) {
        return orderService.queryOrderStatus(orderId);
    }

    @PostMapping("seckill/{id}")
    public Result reservePack(@PathVariable("id") Long packId) {
        return orderService.reservePack(packId);
    }
}
