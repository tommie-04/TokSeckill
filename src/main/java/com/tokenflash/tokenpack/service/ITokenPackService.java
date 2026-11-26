package com.tokenflash.tokenpack.service;

import com.tokenflash.common.dto.Result;
import com.tokenflash.tokenpack.entity.TokenPack;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ITokenPackService extends IService<TokenPack> {

    Result listByProvider(Long shopId);

    void createFlashSalePack(TokenPack pack);
}
