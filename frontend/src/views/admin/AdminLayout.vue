<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAvatar, NDropdown, useMessage, NMenu, NLayoutSider, NLayoutHeader, NLayoutContent } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const message = useMessage()
const collapsed = ref(false)

const menuOptions: MenuOption[] = [
  { label: '数据统计', key: '/admin/dashboard' },
  { label: '用户管理', key: '/admin/users' },
  { label: '分类管理', key: '/admin/categories' },
  { label: '题库管理', key: '/admin/questions' }
]

const activeKey = computed(() => '/' + route.path.split('/')[2])

const userOptions = [{ label: '返回前台', key: 'home' }, { label: '退出登录', key: 'logout' }]

function onMenuClick(key: string) {
  router.push(key)
}

function onUserAction(key: string | number) {
  if (key === 'logout') {
    userStore.logout().then(() => {
      message.success('已退出登录')
      router.push('/login')
    })
  } else if (key === 'home') {
    router.push('/')
  }
}
</script>

<template>
  <div class="admin-layout">
    <n-layout-sider bordered collapse-mode="width" :collapsed-width="64" :width="200" :collapsed="collapsed" show-trigger @collapse="collapsed = true" @expand="collapsed = false">
      <div class="logo" :class="{ mini: collapsed }">
        <span class="logo-mark">Q</span>
        <span v-if="!collapsed" class="logo-text">QuestRush 后台</span>
      </div>
      <n-menu
        :value="activeKey"
        :options="menuOptions"
        :collapsed="collapsed"
        :collapsed-width="64"
        :indent="20"
        @update:value="onMenuClick"
      />
    </n-layout-sider>

    <div class="admin-main">
      <n-layout-header bordered class="admin-header">
        <span class="page-flag">管理后台</span>
        <n-dropdown :options="userOptions" @select="onUserAction">
          <div class="user-entry">
            <n-avatar round :size="28" style="background: #18a058">
              {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || 'A').slice(0, 1) }}
            </n-avatar>
            <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          </div>
        </n-dropdown>
      </n-layout-header>
      <n-layout-content class="admin-content">
        <router-view />
      </n-layout-content>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  overflow: hidden;
}

.admin-layout :deep(.n-layout-sider) {
  height: 100vh;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  border-bottom: 1px solid #eceef1;
}

.logo.mini {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #18a058;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-weight: 700;
  color: #18a058;
  white-space: nowrap;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
}

.page-flag {
  font-weight: 600;
  color: #666;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
}

.admin-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f5f7fa;
}
</style>
