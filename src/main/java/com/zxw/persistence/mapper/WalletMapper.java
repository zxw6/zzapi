package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.WalletEntity;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 钱包 Mapper。
 */
/**
 * 钱包数据访问接口。
 * 提供余额查询、默认钱包创建和金额扣减操作。
 */
public interface WalletMapper extends BaseMapper<WalletEntity> {

    // 查询指定用户的钱包余额
    BigDecimal selectBalanceByUserId(@Param("userId") Long userId);

    // 查询指定用户的钱包主记录
    WalletEntity selectByUserId(@Param("userId") Long userId);

    // 为用户插入默认钱包
    int insertDefaultWallet(@Param("userId") Long userId);

    // 扣减钱包余额，通常用于套餐购买等消费场景
    int debitBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    default int updateByUserId(Long userId, WalletEntity updateWallet) {
        // 根据用户ID更新钱包记录
        return update(updateWallet, Wrappers.<WalletEntity>lambdaUpdate()
                .eq(WalletEntity::getUserId, userId));
    }

    default void deleteByUserId(Long userId) {
        // 删除指定用户的钱包记录
        delete(Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getUserId, userId));
    }
}
