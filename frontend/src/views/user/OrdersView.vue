<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NEmpty, NSpin, NTag } from 'naive-ui'
import { payMock } from '@/api/vip'
import { myOrders } from '@/api/vip'
import { useMessage } from 'naive-ui'
import { formatTime } from '@/utils/format'
import type { VipOrderVO } from '@/types'

const router = useRouter()
const message = useMessage()
const loading = ref(true)
const list = ref<VipOrderVO[]>([])

const STATUS_MAP: Record<number, { label: string; type: 'warning' | 'success' | 'default' }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已关闭', type: 'default' }
}

const PLAN_MAP: Record<string, string> = { month: '月卡', quarter: '季卡', year: '年卡' }

async function load() {
  loading.value = true
  try {
    list.value = (await myOrders()) ?? []
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function payNow(order: VipOrderVO) {
  await payMock(order.orderNo)
  message.success('支付成功')
  await load()
}
</script>

<template>
  <div class="page-container">
    <h2 class="page-title">我的订单</h2>
    <n-spin :show="loading">
      <div v-if="list.length" class="table-card">
        <table class="order-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>套餐</th>
              <th>金额</th>
              <th>支付渠道</th>
              <th>状态</th>
              <th>下单时间</th>
              <th>支付时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in list" :key="order.id">
              <td class="mono">{{ order.orderNo }}</td>
              <td>{{ PLAN_MAP[order.planType] ?? order.planType }}（{{ order.months }} 个月）</td>
              <td>¥{{ order.amount }}</td>
              <td>{{ order.payChannel === 'MOCK' ? '模拟支付' : order.payChannel }}</td>
              <td>
                <n-tag size="small" :bordered="false" :type="STATUS_MAP[order.status]?.type ?? 'default'">
                  {{ STATUS_MAP[order.status]?.label ?? '未知' }}
                </n-tag>
              </td>
              <td>{{ formatTime(order.createTime) }}</td>
              <td>{{ formatTime(order.payTime) }}</td>
              <td>
                <n-button v-if="order.status === 0" size="tiny" type="primary" @click="payNow(order)">模拟支付</n-button>
                <span v-else class="none">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <n-empty v-else-if="!loading" description="暂无订单">
        <template #extra>
          <n-button type="primary" @click="router.push('/vip')">去开通会员</n-button>
        </template>
      </n-empty>
    </n-spin>
  </div>
</template>

<style scoped>
.table-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 20px;
  overflow-x: auto;
}

.order-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.order-table th,
.order-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f1f3;
  text-align: left;
  font-size: 13px;
}

.order-table th {
  color: #888;
  font-weight: 500;
  background: #fafbfc;
}

.mono {
  font-family: 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
}

.none {
  color: #ccc;
}
</style>
