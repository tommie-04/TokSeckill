package com.hmdp.tokenpack.controller;


import com.hmdp.common.dto.Result;
import com.hmdp.tokenpack.entity.TokenPack;
import com.hmdp.tokenpack.service.ITokenPackService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Serves the token-pack catalog and retains the historical voucher endpoints.
 *
 * @author 虎哥
 */
@RestController
@RequestMapping({"/token-packs", "/voucher"})
public class TokenPackController {

    @Resource
    private ITokenPackService tokenPackService;

    /**
     * 新增秒杀包
     * @param pack Token 包信息，包含秒杀信息
     * @return Token 包id
     */
    @PostMapping("seckill")
    public Result createFlashSalePack(@RequestBody TokenPack pack) {
        tokenPackService.createFlashSalePack(pack);
        return Result.ok(pack.getId());
    }

    /**
     * 新增普通券
     * @param pack Token 包信息
     * @return Token 包id
     */
    @PostMapping
    public Result createPack(@RequestBody TokenPack pack) {
        tokenPackService.save(pack);
        return Result.ok(pack.getId());
    }


    /**
     * 查询店铺的【秒杀】Token 包列表
     * @param shopId 店铺id
     * @return Token 包列表
     */
    @GetMapping("/list/{shopId}")
    public Result listByProvider(@PathVariable("shopId") Long shopId) {
       return tokenPackService.listByProvider(shopId);
    }
}
