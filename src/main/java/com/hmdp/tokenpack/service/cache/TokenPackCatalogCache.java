package com.hmdp.tokenpack.service.cache;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.hmdp.infrastructure.cache.TokenPackCacheProperties;
import com.hmdp.tokenpack.entity.TokenPack;
import com.hmdp.tokenpack.mapper.TokenPackMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.hmdp.infrastructure.redis.RedisConstants.CACHE_TOKEN_PACK_LIST_KEY;

/** Loads token-pack display metadata through the selected cache path. Inventory is checked by Lua. */
@Slf4j
@Service
public class TokenPackCatalogCache {

    @Resource
    private TokenPackMapper tokenPackMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private TokenPackCacheProperties cacheProperties;

    private LoadingCache<Long, List<TokenPack>> providerPacks;

    @PostConstruct
    public void initCache() {
        providerPacks = Caffeine.newBuilder()
                .maximumSize(1000)  // 缓存最大容量值
                .refreshAfterWrite(cacheProperties.getRefreshAfterWrite().toMillis(), TimeUnit.MILLISECONDS)
                .expireAfterWrite(cacheProperties.getExpireAfterWrite().toMillis(), TimeUnit.MILLISECONDS)
                .build(new CacheLoader<Long, List<TokenPack>>() {
                    @Override
                    public List<TokenPack> load(Long shopId) {
                        return loadFromRedisThenDb(shopId);
                    }
                });
    }


    public List<TokenPack> listByProvider(Long shopId) {
        String mode = cacheProperties.getMode();
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "mysql":
                return loadFromDb(shopId);
            case "redis":
                return loadFromRedisThenDb(shopId);
            case "caffeine":
            default:
                return providerPacks.get(shopId);
        }
    }


    private List<TokenPack> loadFromRedisThenDb(Long shopId) {
        String key = CACHE_TOKEN_PACK_LIST_KEY + shopId;

        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toList(json, TokenPack.class);
        }

        List<TokenPack> packs = loadFromDb(shopId);

        if(CollectionUtil.isNotEmpty(packs)) {
            long ttlMillis = cacheProperties.getRedisTtl().toMillis();
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(packs), ttlMillis, TimeUnit.MILLISECONDS);
        }

        return packs;
    }

    private List<TokenPack> loadFromDb(Long shopId) {
        List<TokenPack> packs = tokenPackMapper.listByProvider(shopId);
        return packs == null ? Collections.emptyList() : packs;
    }
}
