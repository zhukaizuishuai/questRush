package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.dto.VipOrderDTO;
import com.learn.entity.User;
import com.learn.entity.VipOrder;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserMapper;
import com.learn.mapper.VipOrderMapper;
import com.learn.service.VipService;
import com.learn.vo.VipOrderVO;
import com.learn.vo.VipPlanVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VIP 服务实现（文档 4.5）。
 * 套餐：月/季/年卡对应 1/3/12 个月；模拟支付即置已支付，
 * 权益发放走「剩余时长顺延」SQL（UserMapper.grantVip），不覆盖用户已有时长。
 */
@Slf4j
@Service
public class VipServiceImpl implements VipService {

    private static final Map<String, VipPlanVO> PLANS = Map.of(
            "month", new VipPlanVO("month", "月卡", 1, new BigDecimal("19.90"), "全部 VIP 题目 30 天"),
            "quarter", new VipPlanVO("quarter", "季卡", 3, new BigDecimal("49.90"), "全部 VIP 题目 90 天，立省 40%"),
            "year", new VipPlanVO("year", "年卡", 12, new BigDecimal("149.90"), "全部 VIP 题目 365 天，立省 50%")
    );

    @Resource
    private VipOrderMapper vipOrderMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public List<VipPlanVO> plans() {
        return List.of(PLANS.get("month"), PLANS.get("quarter"), PLANS.get("year"));
    }

    @Override
    public VipOrderVO order(VipOrderDTO dto, Long userId) {
        VipPlanVO plan = PLANS.get(dto.getPlanType());
        if (plan == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "套餐类型不合法");
        }
        VipOrder order = new VipOrder();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setPlanType(plan.getPlanType());
        order.setMonths(plan.getMonths());
        order.setAmount(plan.getPrice());
        order.setPayChannel("MOCK");
        order.setStatus(0);
        vipOrderMapper.insert(order);
        return toVO(order);
    }

    /**
     * 模拟支付：点击即置已支付，走与真实支付完全相同的后置逻辑（文档 3.9/4.5）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(String orderNo, Long userId) {
        VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .eq(VipOrder::getUserId, userId));
        if (order == null) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "订单状态不允许支付");
        }
        // 订单状态流转
        VipOrder update = new VipOrder();
        update.setId(order.getId());
        update.setStatus(1);
        update.setPayTime(LocalDateTime.now());
        vipOrderMapper.updateById(update);
        // 权益发放（事务内）：剩余时长顺延而非覆盖（文档 4.5）
        userMapper.grantVip(userId, order.getMonths());
        log.info("vip granted, userId={}, orderNo={}, months={}", userId, orderNo, order.getMonths());
    }

    @Override
    public IPage<VipOrderVO> myOrders(long pageNum, long pageSize, Long userId) {
        Page<VipOrder> page = new Page<>(pageNum, Math.min(pageSize, 100));
        IPage<VipOrder> result = vipOrderMapper.selectPage(page, new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getUserId, userId)
                .orderByDesc(VipOrder::getId));
        return result.convert(this::toVO);
    }

    private VipOrderVO toVO(VipOrder order) {
        VipOrderVO vo = new VipOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPlanType(order.getPlanType());
        vo.setMonths(order.getMonths());
        vo.setAmount(order.getAmount());
        vo.setPayChannel(order.getPayChannel());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
