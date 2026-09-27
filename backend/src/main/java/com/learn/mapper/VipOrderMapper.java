package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.VipOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP 订单表 Mapper（文档 3.9）
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {
}
