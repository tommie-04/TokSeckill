package com.hmdp.seckill.service;

import com.hmdp.common.dto.Result;
import com.hmdp.seckill.entity.TokenPackOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ITokenPackOrderService extends IService<TokenPackOrder> {

    Result reservePack(Long packId);

    Result queryOrderStatus(Long orderId);

    void persistReservedOrder(TokenPackOrder order);
}
