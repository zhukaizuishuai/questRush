import { get, post } from './request'
import type { PageResult, VipOrderVO, VipPlanVO } from '@/types'

/** 套餐列表 */
export function getPlans() {
  return get<VipPlanVO[]>('/vip/plans')
}

/** 创建订单 */
export function createOrder(planType: string) {
  return post<VipOrderVO>('/vip/order', { planType })
}

/** 模拟支付（后端为 @RequestParam String orderNo，必须走 query，不能放 body） */
export function payMock(orderNo: string) {
  return post<null>('/vip/pay/mock', null, { params: { orderNo } })
}

/** 我的订单（后端返回 PageResult，需按分页解包） */
export function myOrders(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<VipOrderVO>>('/vip/orders', params)
}
