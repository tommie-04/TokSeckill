package com.hmdp.controller;


import com.hmdp.common.dto.Result;
import com.hmdp.service.IVoucherOrderService;
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
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @GetMapping("/{id}")
    public Result queryOrderStatus(@PathVariable("id") Long orderId) {
        return voucherOrderService.queryOrderStatus(orderId);
    }

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
