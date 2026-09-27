<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { NButton, NSpin, NTag, useMessage } from 'naive-ui'
import { createOrder, getPlans, payMock } from '@/api/vip'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'
import type { VipOrderVO, VipPlanVO } from '@/types'

const message = useMessage()
const userStore = useUserStore()

const loading = ref(true)
const plans = ref<VipPlanVO[]>([])
const orderLoading = ref('')
const currentOrder = ref<VipOrderVO | null>(null)

const planIcon: Record<string, string> = { month: '🌙', quarter: '⭐', year: '👑' }

onMounted(async () => {
  try {
    plans.value = (await getPlans()) ?? []
  } finally {
    loading.value = false
  }
})

async function orderAndPay(plan: VipPlanVO) {
  if (!userStore.isLoggedIn) {
    message.warning('请先登录')
    return
  }
  orderLoading.value = plan.planType
  try {
    // 下单
    currentOrder.value = await createOrder(plan.planType)
    // 模拟支付（MOCK 渠道点击即支付，走与真实支付相同的后置权益发放）
    await payMock(currentOrder.value.orderNo)
    await userStore.fetchUserInfo()
    message.success(`🎉 支付成功，${plan.name}已开通！`)
    currentOrder.value = null
  } finally {
    orderLoading.value = ''
  }
}
</script>

<template>
  <div class="page-container">
    <div class="vip-hero">
      <h2>开通 VIP 会员</h2>
      <p>解锁全部 VIP 专属题库与解析，支持顺序 / 随机刷题全量题源</p>
      <div v-if="userStore.isVip" class="current-vip">
        <n-tag type="warning" :bordered="false">当前为 VIP 会员，有效期至 {{ formatDate(userStore.vipExpireTime) }}（续费按时长顺延）</n-tag>
      </div>
    </div>

    <n-spin :show="loading">
      <div class="plan-grid">
        <div v-for="plan in plans" :key="plan.planType" class="plan-card" :class="{ hot: plan.planType === 'year' }">
          <div class="plan-icon">{{ planIcon[plan.planType] ?? '🎫' }}</div>
          <div class="plan-name">{{ plan.name }}</div>
          <div class="plan-price">
            <span class="price">¥{{ plan.price }}</span>
          </div>
          <div class="plan-months">共 {{ plan.months }} 个月</div>
          <div v-if="plan.description" class="plan-desc">{{ plan.description }}</div>
          <n-button
            type="primary"
            block
            :loading="orderLoading === plan.planType"
            @click="orderAndPay(plan)"
          >
            立即开通
          </n-button>
        </div>
      </div>
      <div v-if="!loading && !plans.length" class="empty-tip">套餐加载中或暂未配置</div>
    </n-spin>

    <p class="mock-tip">※ 本平台为演示项目，使用模拟支付：下单后点击支付即完成订单与权益发放，与真实支付流程一致。</p>
  </div>
</template>

<style scoped>
.vip-hero {
  text-align: center;
  padding: 30px 0 24px;
}

.vip-hero h2 {
  margin: 0 0 8px;
}

.vip-hero p {
  color: #888;
  margin: 0;
}

.current-vip {
  margin-top: 14px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.plan-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 14px;
  padding: 28px 24px;
  text-align: center;
  transition: all 0.15s;
}

.plan-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.plan-card.hot {
  border-color: #ffd77a;
}

.plan-icon {
  font-size: 36px;
}

.plan-name {
  font-weight: 700;
  font-size: 16px;
  margin: 10px 0;
}

.plan-price {
  margin-bottom: 6px;
}

.price {
  font-size: 30px;
  font-weight: 700;
  color: #e05a5a;
}

.original {
  color: #bbb;
  text-decoration: line-through;
  font-size: 14px;
  margin-left: 6px;
}

.plan-months {
  color: #888;
  font-size: 13px;
  margin-bottom: 8px;
}

.plan-desc {
  color: #999;
  font-size: 12px;
  margin-bottom: 16px;
  min-height: 18px;
}

.mock-tip {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin-top: 28px;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 60px 0;
}
</style>
