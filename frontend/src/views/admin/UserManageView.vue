<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NDataTable,
  NDatePicker,
  NInput,
  NModal,
  NSpace,
  useDialog,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import { setUserVip, updateUserStatus, userList } from '@/api/admin'
import { formatDate, formatTime, toLocalDateTimeString } from '@/utils/format'
import type { AdminUserVO } from '@/types'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const list = ref<AdminUserVO[]>([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

// VIP 设置弹窗
const showVipModal = ref(false)
const vipUser = ref<AdminUserVO | null>(null)
const vipExpire = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const page = await userList({
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = page.list
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

function onSearch() {
  query.pageNum = 1
  load()
}

/** 禁用 / 启用（后端会强制登出该用户） */
function toggleStatus(row: AdminUserVO) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 0 ? '禁用' : '启用'
  dialog.warning({
    title: `确认${action}`,
    content: `确定要${action}用户「${row.username}」吗？${next === 0 ? '该用户的登录态将被强制注销。' : ''}`,
    positiveText: '确定',
    negativeText: '取消',
    async onPositiveClick() {
      await updateUserStatus(row.id, next)
      message.success(`已${action}`)
      await load()
    }
  })
}

function openVipModal(row: AdminUserVO) {
  vipUser.value = row
  vipExpire.value = row.vipExpireTime ? new Date(row.vipExpireTime).getTime() : null
  showVipModal.value = true
}

async function saveVip() {
  if (!vipUser.value) return
  // 后端字段是 LocalDateTime，必须发本地时间串（不能用 toISOString() 的 UTC+Z）
  const timeStr = toLocalDateTimeString(vipExpire.value)
  await setUserVip(vipUser.value.id, timeStr)
  message.success(vipExpire.value ? 'VIP 过期时间已更新' : '已取消 VIP')
  showVipModal.value = false
  await load()
}

const columns: DataTableColumns<AdminUserVO> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户名', key: 'username', width: 140 },
  { title: '昵称', key: 'nickname', width: 120, render: (row) => row.nickname || '-' },
  { title: '邮箱', key: 'email', render: (row) => row.email || '-' },
  {
    title: '角色',
    key: 'role',
    width: 90,
    render: (row) => (row.role === 'admin' ? '管理员' : '普通用户')
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => (row.status === 1 ? '正常' : '禁用')
  },
  {
    title: 'VIP 到期时间',
    key: 'vipExpireTime',
    width: 150,
    render: (row) => (row.vipExpireTime ? formatDate(row.vipExpireTime) : '未开通')
  },
  { title: '注册时间', key: 'createTime', width: 150, render: (row) => formatTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render(row) {
      return h(NSpace, { size: 'small' }, () => [
        h(
          NButton,
          { size: 'tiny', onClick: () => openVipModal(row) },
          () => '设置 VIP'
        ),
        h(
          NButton,
          {
            size: 'tiny',
            type: row.status === 1 ? 'error' : 'success',
            secondary: true,
            onClick: () => toggleStatus(row)
          },
          () => (row.status === 1 ? '禁用' : '启用')
        )
      ])
    }
  }
]
</script>

<template>
  <div>
    <h2 class="page-title">用户管理</h2>
    <div class="toolbar">
      <n-input
        v-model:value="query.keyword"
        placeholder="按用户名 / 昵称搜索"
        clearable
        style="width: 220px"
        @keyup.enter="onSearch"
        @clear="onSearch"
      />
      <n-button type="primary" secondary @click="onSearch">搜索</n-button>
    </div>

    <div class="table-card">
      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :bordered="false"
        :pagination="{
          page: query.pageNum,
          pageSize: query.pageSize,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onChange: (p: number) => { query.pageNum = p; load() },
          onUpdatePageSize: (s: number) => { query.pageSize = s; query.pageNum = 1; load() }
        }"
      />
    </div>

    <n-modal v-model:show="showVipModal" preset="card" title="手动设置 VIP 过期时间" style="width: 440px">
      <p class="modal-tip">用户：{{ vipUser?.username }}（直接覆盖 vip_expire_time，设为空表示取消 VIP）</p>
      <n-date-picker v-model:value="vipExpire" type="datetime" clearable placeholder="选择过期时间（留空取消）" style="width: 100%" />
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px">
          <n-button @click="showVipModal = false">取消</n-button>
          <n-button type="primary" @click="saveVip">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.table-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 16px;
}

.modal-tip {
  color: #888;
  font-size: 13px;
  margin: 0 0 12px;
}
</style>
