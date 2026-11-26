package com.tokenflash.tokenpack.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tokenflash.common.dto.Result;
import com.tokenflash.tokenpack.entity.FlashSaleStock;
import com.tokenflash.tokenpack.entity.TokenPack;
import com.tokenflash.tokenpack.mapper.TokenPackMapper;
import com.tokenflash.tokenpack.service.IFlashSaleStockService;
import com.tokenflash.tokenpack.service.ITokenPackService;
import com.tokenflash.tokenpack.service.cache.TokenPackCatalogCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class TokenPackServiceImpl extends ServiceImpl<TokenPackMapper, TokenPack> implements ITokenPackService {

    @Resource
    private IFlashSaleStockService stockService;
    @Resource
    private TokenPackCatalogCache catalogCache;

    @Override
    public Result listByProvider(Long shopId) {
        return Result.ok(catalogCache.listByProvider(shopId));
    }

    @Override
    @Transactional
    public void createFlashSalePack(TokenPack pack) {
        // 保存Token 包
        save(pack);
        // 保存秒杀信息
        FlashSaleStock saleStock = new FlashSaleStock();
        saleStock.setVoucherId(pack.getId());
        saleStock.setStock(pack.getStock());
        saleStock.setBeginTime(pack.getBeginTime());
        saleStock.setEndTime(pack.getEndTime());
        stockService.save(saleStock);
    }
}
