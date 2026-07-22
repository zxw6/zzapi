package com.zxw.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zxw.persistence.entity.TransactionEntity;

/**
 * 钱包交易流水 Mapper。
 */
/**
 * 交易流水数据访问接口。
 * 负责钱包流水的基础读写和用户级删除。
 */
public interface TransactionMapper extends BaseMapper<TransactionEntity> {

    default void deleteByUserId(Long userId) {
        // 删除指定用户下的全部交易流水
        delete(Wrappers.<TransactionEntity>lambdaQuery()
                .eq(TransactionEntity::getUserId, userId));
    }
}
