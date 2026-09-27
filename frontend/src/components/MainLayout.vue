<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NAvatar, NDropdown, NButton, useMessage } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const navItems = [
  { label: '首页', path: '/' },
  { label: '分类', path: '/category' },
  { label: '题库', path: '/question' },
  { label: '刷题', path: '/practice' },
  { label: '错题本', path: '/wrong' },
  { label: '复习', path: '/review' },
  { label: '统计', path: '/stats' },
  { label: '会员', path: '/vip' }
]

const activePath = computed(() => route.path)

const userOptions = computed(() => {
  const opts = [
    { label: '个人中心', key: 'profile' },
    { label: '我的收藏', key: 'favorites' },
    { label: '我的笔记', key: 'notes' },
    { label: '我的订单', key: 'orders' }
  ]
  if (userStore.isAdmin) {
    opts.push({ label: '管理后台', key: 'admin' })
  }
  opts.push({ label: '退出登录', key: 'logout' })
  return opts
})

function onNav(path: string) {
  router.push(path)
}

function onUserAction(key: string | number) {
  if (key === 'logout') {
    userStore.logout().then(() => {
      message.success('已退出登录')
      router.push('/login')
    })
    return
  }
  router.push(`/${key}`)
}

const avatarText = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '用').slice(0, 1))
</script>

<template>
  <div class="main-layout">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="onNav('/')">
          <span class="logo-mark">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M9.25 1.5L4.5 8h3l-.75 4.5L11.5 6h-3l.75-4.5z" fill="#fff" />
            </svg>
          </span>
          <span class="logo-text">QuestRush</span>
        </div>
        <nav class="nav">
          <a
            v-for="item in navItems"
            :key="item.path"
            class="nav-item"
            :class="{ active: activePath === item.path || (item.path !== '/' && activePath.startsWith(item.path)) }"
            @click="onNav(item.path)"
          >{{ item.label }}</a>
        </nav>
        <div class="right">
          <template v-if="userStore.isLoggedIn">
            <n-dropdown :options="userOptions" @select="onUserAction">
              <div class="user-entry">
                <n-avatar round :size="30" style="background: #10b981">{{ avatarText }}</n-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
                <span v-if="userStore.isVip" class="mini-vip">VIP</span>
              </div>
            </n-dropdown>
          </template>
          <template v-else>
            <n-button quaternary size="small" @click="onNav('/login')">登录</n-button>
            <n-button type="primary" size="small" @click="onNav('/register')">注册</n-button>
          </template>
        </div>
      </div>
    </header>

    <main class="content">
      <router-view />
    </main>

    <footer class="footer">
      QuestRush 在线刷题学习平台
      <template v-if="userStore.isLoggedIn && userStore.vipExpireTime">
        ｜会员有效期至 {{ formatDate(userStore.vipExpireTime) }}
      </template>
    </footer>
  </div>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-1);
}

.topbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: var(--grad-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-brand);
}

.logo-text {
  font-weight: 700;
  font-size: 17px;
  color: var(--ink-900);
  letter-spacing: -0.01em;
}

.nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.nav-item {
  padding: 6px 13px;
  border-radius: 8px;
  font-size: 14px;
  color: var(--ink-600);
  cursor: pointer;
  transition: all 0.15s;
}

.nav-item:hover {
  color: var(--brand-700);
  background: var(--brand-50);
}

.nav-item.active {
  color: var(--brand-700);
  background: var(--brand-50);
  font-weight: 600;
}

.right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}

.user-entry:hover {
  background: var(--bg-soft);
}

.username {
  font-size: 14px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-vip {
  font-size: 11px;
  font-weight: 700;
  color: var(--gold-text);
  background: linear-gradient(135deg, #ffe9b8, #ffcb47);
  border-radius: 4px;
  padding: 1px 5px;
}

.content {
  flex: 1;
  width: 100%;
}

.footer {
  text-align: center;
  color: var(--ink-400);
  font-size: 12px;
  padding: 24px 0;
}
</style>
