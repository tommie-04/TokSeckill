package com.hmdp.tokenpack.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmdp.tokenpack.entity.TokenPack;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface TokenPackMapper extends BaseMapper<TokenPack> {

    // 查询秒杀Token 包,where type = 1
    List<TokenPack> listByProvider(@Param("shopId") Long shopId);
}
