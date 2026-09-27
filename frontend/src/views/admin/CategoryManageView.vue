<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { NButton, NForm, NFormItem, NInput, NInputNumber, NModal, NSelect, NSpin, useDialog, useMessage } from 'naive-ui'
import { adminCategoryList, createCategory, deleteCategory, updateCategory } from '@/api/admin'
import type { CategoryVO } from '@/types'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const tree = ref<CategoryVO[]>([])

// 编辑弹窗
const showModal = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const form = reactive({ id: 0, name: '', parentId: 0, sort: 0 })

interface PNode {
  label: string
  value: number
}

const parentOptions = ref<PNode[]>([])

function buildParentOptions(nodes: CategoryVO[]) {
  const out: PNode[] = [{ label: '一级分类（无父级）', value: 0 }]
  for (const n of nodes) {
    out.push({ label: n.name, value: n.id })
  }
  return out
}

async function load() {
  loading.value = true
  try {
    tree.value = (await adminCategoryList()) ?? []
    parentOptions.value = buildParentOptions(tree.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate(parentId = 0) {
  isEdit.value = false
  form.id = 0
  form.name = ''
  form.parentId = parentId
  form.sort = 0
  showModal.value = true
}

function openEdit(node: CategoryVO) {
  isEdit.value = true
  form.id = node.id
  form.name = node.name
  form.parentId = node.parentId
  form.sort = node.sort
  showModal.value = true
}

async function save() {
  if (!form.name.trim()) {
    message.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateCategory({ id: form.id, name: form.name, parentId: form.parentId, sort: form.sort })
    } else {
      await createCategory({ name: form.name, parentId: form.parentId, sort: form.sort })
    }
    message.success('已保存')
    showModal.value = false
    await load()
  } finally {
    saving.value = false
  }
}

/** 删除前由后端校验子分类与启用题目 */
function remove(node: CategoryVO) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除分类「${node.name}」吗？若其下存在子分类或启用状态的题目，删除将被拒绝。`,
    positiveText: '确定删除',
    negativeText: '取消',
    async onPositiveClick() {
      try {
        await deleteCategory(node.id)
        message.success('已删除')
        await load()
      } catch {
        // 拦截器已提示具体原因
      }
    }
  })
}
</script>

<template>
  <div>
    <div class="head-row">
      <h2 class="page-title">分类管理</h2>
      <n-button type="primary" @click="openCreate(0)">新增一级分类</n-button>
    </div>

    <n-spin :show="loading">
      <div class="cat-table-card">
        <table class="cat-table">
          <thead>
            <tr>
              <th style="width: 60px">ID</th>
              <th>分类名称</th>
              <th style="width: 100px">排序</th>
              <th style="width: 240px">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="root in tree" :key="root.id">
              <tr class="root-row">
                <td>{{ root.id }}</td>
                <td>
                  <b>{{ root.name }}</b>
                  <span class="level-tag">一级</span>
                </td>
                <td>{{ root.sort }}</td>
                <td class="ops">
                  <n-button size="tiny" @click="openEdit(root)">编辑</n-button>
                  <n-button size="tiny" type="primary" secondary @click="openCreate(root.id)">加子分类</n-button>
                  <n-button size="tiny" type="error" secondary @click="remove(root)">删除</n-button>
                </td>
              </tr>
              <tr v-for="child in root.children ?? []" :key="child.id" class="child-row">
                <td>{{ child.id }}</td>
                <td>└─ {{ child.name }}</td>
                <td>{{ child.sort }}</td>
                <td class="ops">
                  <n-button size="tiny" @click="openEdit(child)">编辑</n-button>
                  <n-button size="tiny" type="error" secondary @click="remove(child)">删除</n-button>
                </td>
              </tr>
            </template>
            <tr v-if="!loading && !tree.length">
              <td colspan="4" class="empty">暂无分类</td>
            </tr>
          </tbody>
        </table>
      </div>
    </n-spin>

    <n-modal v-model:show="showModal" preset="card" :title="isEdit ? '编辑分类' : '新增分类'" style="width: 440px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="名称">
          <n-input v-model:value="form.name" placeholder="分类名称" />
        </n-form-item>
        <n-form-item label="父分类">
          <n-select v-model:value="form.parentId" :options="parentOptions" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="排序">
          <n-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <div style="display: flex; justify-content: flex-end; gap: 10px">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cat-table-card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 16px;
  overflow-x: auto;
}

.cat-table {
  width: 100%;
  border-collapse: collapse;
}

.cat-table th,
.cat-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f1f3;
  text-align: left;
  font-size: 14px;
}

.cat-table th {
  color: #888;
  font-weight: 500;
  background: #fafbfc;
}

.child-row td {
  color: #666;
}

.level-tag {
  margin-left: 8px;
  font-size: 11px;
  color: #18a058;
  background: #f0faf4;
  border-radius: 3px;
  padding: 1px 5px;
  font-weight: 400;
}

.ops {
  display: flex;
  gap: 6px;
}

.empty {
  text-align: center;
  color: #999;
}
</style>
