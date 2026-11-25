package com.hmdp.ai.tools;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ShopTool {

    @Resource
    private IShopService shopService;

    @Tool("根据商家名称查询商家信息")
    public Shop findShop(@P("商家名称") String shopName) {
        return shopService.query().eq("name", shopName).one();
    }
}
