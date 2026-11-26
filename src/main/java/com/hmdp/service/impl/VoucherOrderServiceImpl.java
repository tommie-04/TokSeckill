package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.common.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.infrastructure.redis.RedisIdWorker;
import com.hmdp.user.auth.UserHolder;
import com.hmdp.infrastructure.mq.OrderEventPublisher;
import com.hmdp.infrastructure.redis.FlashSaleReservations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;

/**
 * Reserves one pack in Redis before Kafka submission. The consumer persists the order and
 * deducts durable stock in a separate database transaction.
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private FlashSaleReservations reservations;
    @Resource
    private OrderEventPublisher publisher;

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = reservations.reserve(voucherId, userId);
        if (result == null) {
            return Result.fail("暂时无法确认秒杀资格");
        }
        switch (result.intValue()) {
            case 1: return Result.fail("Token 包库存不足");
            case 2: return Result.fail("该 Token 包已申请过");
            case 3: return Result.fail("活动未开放或已结束");
            case 0: break;
            default: throw new IllegalStateException("Unexpected eligibility response: " + result);
        }

        VoucherOrder order = new VoucherOrder().setId(orderId).setUserId(userId).setVoucherId(voucherId);
        if (publisher.publishAndAwaitAck(order)) {
            return Result.ok(Long.toString(orderId));
        }
        // The broker might already have received the event. Keep the reservation for reconciliation.
        return Result.fail("下单提交状态待确认，订单号：" + orderId);
    }

    @Override
    public Result queryOrderStatus(Long orderId) {
        boolean exists = query().eq("id", orderId).eq("user_id", UserHolder.getUser().getId()).count() > 0;
        return Result.ok(Map.of("status", exists ? "CREATED" : "PENDING_OR_UNKNOWN"));
    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder order) {
        if (order.getId() == null || order.getUserId() == null || order.getVoucherId() == null) {
            throw new IllegalArgumentException("Order event is missing an identifier");
        }
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        if (query().eq("user_id", userId).eq("voucher_id", voucherId).count() > 0) {
            return;
        }

        // The unique constraint also covers duplicates that race past the existence check.
        if (!save(new VoucherOrder().setId(order.getId()).setUserId(userId).setVoucherId(voucherId))) {
            throw new IllegalStateException("Could not persist order " + order.getId());
        }
        boolean deducted = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!deducted) {
            throw new IllegalStateException("MySQL stock unavailable for pack " + voucherId);
        }
    }
}
