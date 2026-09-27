package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.VipOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * VIP 订单表 Mapper（文档 3.9）
 */
@Mapper
public interface VipOrderMapper extends BaseMapper<VipOrder> {

    /**
     * 原子置为已支付：仅当订单仍处于「待支付(0)」时才更新。
     * <p>
     * 用于模拟支付/回调，避免并发重复请求（用户连点、前端重放）
     * 各自读到 status=0 后都去发放 VIP 权益，造成重复顺延时长。
     *
     * @return 受影响行数；=1 表示本次真正完成状态流转，可安全发放权益
     */
    @Update("UPDATE vip_order SET status = 1, pay_time = NOW(), update_time = NOW() " +
            "WHERE id = #{id} AND status = 0")
    int markPaid(@Param("id") Long id);
}
