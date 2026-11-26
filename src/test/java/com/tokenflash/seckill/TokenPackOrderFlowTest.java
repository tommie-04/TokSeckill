package com.tokenflash.seckill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenflash.infrastructure.redis.UserSessionStore;
import com.tokenflash.tokenpack.entity.TokenPack;
import com.tokenflash.tokenpack.service.ITokenPackService;
import com.tokenflash.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tokenflash.infrastructure.redis.RedisConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TokenPackOrderFlowTest {

    @Resource private MockMvc mvc;
    @Resource private ObjectMapper json;
    @Resource private ITokenPackService packs;
    @Resource private StringRedisTemplate redis;
    @Resource private UserSessionStore sessions;
    @Resource private JdbcTemplate db;

    @Test
    void catalogReservationAndKafkaDeliveryUseTheSamePack() throws Exception {
        long provider = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        TokenPack pack = new TokenPack().setShopId(provider).setTitle("Test pack")
                .setPayValue(990L).setActualValue(1990L).setTokenUnits(1000L)
                .setType(1).setStatus(1).setStock(2)
                .setBeginTime(LocalDateTime.now().minusHours(1)).setEndTime(LocalDateTime.now().plusHours(1));
        packs.createFlashSalePack(pack);
        long id = pack.getId();
        String token = UUID.randomUUID().toString();
        try {
            long now = Instant.now().getEpochSecond();
            redis.opsForValue().set(SECKILL_STOCK_KEY + id, "2");
            redis.opsForHash().putAll(SECKILL_WINDOW_KEY + id,
                    Map.of("begin", Long.toString(now - 3600), "end", Long.toString(now + 3600)));
            UserDTO user = new UserDTO();
            user.setId(provider);
            sessions.saveSession(token, user);

            mvc.perform(get("/token-packs/list/{provider}", provider))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id));
            mvc.perform(post("/token-pack-orders/seckill/{id}", id)).andExpect(status().isUnauthorized());
            String result = mvc.perform(post("/token-pack-orders/seckill/{id}", id).header("authorization", token))
                    .andExpect(jsonPath("$.success").value(true)).andReturn().getResponse().getContentAsString();
            String orderId = json.readTree(result).get("data").asText();
            mvc.perform(post("/token-pack-orders/seckill/{id}", id).header("authorization", token))
                    .andExpect(jsonPath("$.success").value(false));

            long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (System.nanoTime() < deadline && db.queryForObject(
                    "SELECT COUNT(*) FROM tb_voucher_order WHERE id = ?", Integer.class, orderId) == 0) {
                Thread.sleep(100);
            }
            mvc.perform(get("/token-pack-orders/{id}", orderId).header("authorization", token))
                    .andExpect(jsonPath("$.data.status").value("CREATED"));
            assertThat(redis.opsForValue().get(SECKILL_STOCK_KEY + id)).isEqualTo("1");
            assertThat(db.queryForObject("SELECT stock FROM tb_seckill_voucher WHERE voucher_id = ?",
                    Integer.class, id)).isEqualTo(1);
        } finally {
            redis.delete(List.of(SECKILL_STOCK_KEY + id, SECKILL_ORDER_KEY + id, SECKILL_WINDOW_KEY + id,
                    CACHE_TOKEN_PACK_LIST_KEY + provider, LOGIN_USER_KEY + token));
            db.update("DELETE FROM tb_voucher_order WHERE voucher_id = ?", id);
            db.update("DELETE FROM tb_seckill_voucher WHERE voucher_id = ?", id);
            packs.removeById(id);
        }
    }
}
