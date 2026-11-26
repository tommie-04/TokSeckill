package com.hmdp.tokenpack.service.impl;

import com.hmdp.tokenpack.entity.FlashSaleStock;
import com.hmdp.tokenpack.mapper.FlashSaleStockMapper;
import com.hmdp.tokenpack.service.IFlashSaleStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀Token 包表，与Token 包是一对一关系 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2022-01-04
 */
@Service
public class FlashSaleStockServiceImpl extends ServiceImpl<FlashSaleStockMapper, FlashSaleStock> implements IFlashSaleStockService {

}
