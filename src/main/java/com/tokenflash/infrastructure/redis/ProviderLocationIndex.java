package com.tokenflash.infrastructure.redis;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

@Component
public class ProviderLocationIndex {
    @Resource
    private StringRedisTemplate redis;

    public Map<Long, Double> nearby(Integer typeId, double x, double y, int from, int end) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                SHOP_GEO_KEY + typeId, GeoReference.fromCoordinate(x, y), new Distance(5000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end));
        Map<Long, Double> distances = new LinkedHashMap<>();
        if (results != null) {
            results.getContent().stream().skip(from).forEach(result ->
                    distances.put(Long.valueOf(result.getContent().getName()), result.getDistance().getValue()));
        }
        return distances;
    }
}
