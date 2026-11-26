package com.tokenflash.infrastructure.redis;

import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;

@Component
public class CommunityIndex {
    @Resource
    private StringRedisTemplate redis;

    public boolean hasLiked(Long blogId, Long userId) {
        return redis.opsForZSet().score(BLOG_LIKED_KEY + blogId, userId.toString()) != null;
    }

    public void like(Long blogId, Long userId) {
        redis.opsForZSet().add(BLOG_LIKED_KEY + blogId, userId.toString(), System.currentTimeMillis());
    }

    public void unlike(Long blogId, Long userId) {
        redis.opsForZSet().remove(BLOG_LIKED_KEY + blogId, userId.toString());
    }

    public Set<String> firstFiveLikes(Long blogId) {
        return redis.opsForZSet().range(BLOG_LIKED_KEY + blogId, 0, 4);
    }

    public void deliver(Long userId, Long blogId) {
        redis.opsForZSet().add(FEED_KEY + userId, blogId.toString(), System.currentTimeMillis());
    }

    public Set<ZSetOperations.TypedTuple<String>> feedPage(Long userId, long max, int offset) {
        return redis.opsForZSet().reverseRangeByScoreWithScores(FEED_KEY + userId, 0, max, offset, 2);
    }

    public void follow(Long userId, Long followedId) {
        redis.opsForSet().add(FOLLOW_KEY + userId, followedId.toString());
    }

    public void unfollow(Long userId, Long followedId) {
        redis.opsForSet().remove(FOLLOW_KEY + userId, followedId.toString());
    }

    public Set<String> commonFollows(Long firstId, Long secondId) {
        return redis.opsForSet().intersect(FOLLOW_KEY + firstId, FOLLOW_KEY + secondId);
    }
}
