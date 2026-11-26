package com.hmdp.ai.tools;

import com.hmdp.entity.Shop;
import com.hmdp.user.entity.User;
import com.hmdp.tokenpack.entity.TokenPack;
import com.hmdp.seckill.entity.TokenPackOrder;
import com.hmdp.service.IShopService;
import com.hmdp.user.service.IUserService;
import com.hmdp.seckill.service.ITokenPackOrderService;
import com.hmdp.tokenpack.service.ITokenPackService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenPackTool {

    @Resource
    private IShopService shopService;

    @Resource
    private ITokenPackService tokenPackService;

    @Resource
    private IUserService userService;

    @Resource
    private ITokenPackOrderService orderService;

    @Tool("根据商家名称查询商家的优惠券信息")
    public List<TokenPack> findVoucherByShopName(@P("商家名称") String shopName) {
        Shop shop = shopService.query().eq("name", shopName).one();
        if (shop == null) {
            return Collections.emptyList();
        }
        return tokenPackService.query().eq("shop_id", shop.getId()).list();
    }

    @Tool("根据用户手机号查询用户拥有的优惠券信息")
    public List<TokenPack> findVoucherByUserPhone(@P("用户手机号") String userPhone) {
        User user = userService.query().eq("phone", userPhone).one();
        if (user == null) {
            return Collections.emptyList();
        }
        List<TokenPackOrder> orders = orderService.query().eq("user_id", user.getId()).list();
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> voucherIds = orders.stream()
                .map(TokenPackOrder::getVoucherId)
                .distinct()
                .collect(Collectors.toList());
        return tokenPackService.listByIds(voucherIds);
    }
}
