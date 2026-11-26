package com.tokenflash.ai.tools;

import com.tokenflash.provider.entity.Shop;
import com.tokenflash.user.entity.User;
import com.tokenflash.tokenpack.entity.TokenPack;
import com.tokenflash.seckill.entity.TokenPackOrder;
import com.tokenflash.provider.service.IShopService;
import com.tokenflash.user.service.IUserService;
import com.tokenflash.seckill.service.ITokenPackOrderService;
import com.tokenflash.tokenpack.service.ITokenPackService;
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
