package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.dto.VipOrderDTO;
import com.learn.vo.VipOrderVO;
import com.learn.vo.VipPlanVO;

import java.util.List;

/**
 * VIP 服务（文档 4.5）
 */
public interface VipService {

    /** 套餐列表 */
    List<VipPlanVO> plans();

    /** 创建订单（待支付） */
    VipOrderVO order(VipOrderDTO dto, Long userId);

    /** 模拟支付：事务内订单流转 + 权益按剩余时长顺延发放 */
    void mockPay(String orderNo, Long userId);

    /** 我的订单 */
    IPage<VipOrderVO> myOrders(long pageNum, long pageSize, Long userId);
}
