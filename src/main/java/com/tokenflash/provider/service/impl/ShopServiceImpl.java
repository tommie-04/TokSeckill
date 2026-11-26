package com.tokenflash.provider.service.impl;

import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tokenflash.common.dto.Result;
import com.tokenflash.provider.entity.Shop;
import com.tokenflash.provider.mapper.ShopMapper;
import com.tokenflash.provider.service.IShopService;
import com.tokenflash.infrastructure.cache.CacheClient;
import com.tokenflash.common.SystemConstants;
import com.tokenflash.infrastructure.redis.ProviderLocationIndex;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private ProviderLocationIndex locations;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        Shop shop = cacheClient
                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        cacheClient.evict(CACHE_SHOP_KEY, id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x == null || y == null) {
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }

        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        Map<Long, Double> distances = locations.nearby(typeId, x, y, from, end);
        if (distances.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        Set<Long> ids = distances.keySet();
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distances.get(shop.getId()));
        }
        return Result.ok(shops);
    }
}
