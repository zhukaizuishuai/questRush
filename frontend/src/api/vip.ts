import { get, post } from './request'
import type { VipOrderVO, VipPlanVO } from '@/types'

/** 套餐列表 */
export function getPlans() {
  return get<VipPlanVO[]>('/vip/plans')
}

/** 创建订单 */
export function createOrder(planType: string) {
  return post<VipOrderVO>('/vip/order', { planType })
}

/** 模拟支付 */
export function payMock(orderNo: string) {
  return post<null>('/vip/pay/mock', { orderNo })
}

/** 我的订单 */
export function myOrders() {
  return get<VipOrderVO[]>('/vip/orders')
}

export default { getPlans, createOrder, payMock, myOrders }
