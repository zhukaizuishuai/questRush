package com.learn.controller;

import jakarta.annotation.Resource;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.VipOrderDTO;
import com.learn.service.VipService;
import com.learn.vo.VipOrderVO;
import com.learn.vo.VipPlanVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * VIP 接口（文档 6.3）
 */
@RestController
@RequestMapping("/api/vip")
public class VipController {

    @Resource
    private VipService vipService;

    /** 套餐列表 */
    @GetMapping("/plans")
    public Result<List<VipPlanVO>> plans() {
        return Result.ok(vipService.plans());
    }

    /** 创建订单（待支付） */
    @PostMapping("/order")
    public Result<VipOrderVO> order(@Valid @RequestBody VipOrderDTO dto) {
        return Result.ok(vipService.order(dto, StpUtil.getLoginIdAsLong()));
    }

    /** 模拟支付（权益按剩余时长顺延发放，文档 4.5） */
    @PostMapping("/pay/mock")
    public Result<Void> mockPay(@RequestParam String orderNo) {
        vipService.mockPay(orderNo, StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 我的订单 */
    @GetMapping("/orders")
    public Result<PageResult<VipOrderVO>> orders(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        IPage<VipOrderVO> page = vipService.myOrders(pageNum, pageSize, StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }
}
