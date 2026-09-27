<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAvatar, NDropdown, useMessage, NMenu, NLayoutSider, NLayoutHeader, NLayoutContent } from 'naive-ui'
import type { MenuOption } from 'naive-ui'
// MenuThemeOverrides 未从 naive-ui 根导出，只能走子路径（naive-ui 未声明 exports 字段，deep import 可用）
import type { MenuThemeOverrides } from 'naive-ui/es/menu/styles'
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

/**
 * 当前高亮的菜单 key。
 * 路由为 /admin/{segment}[/...]，菜单 key 为 /admin/{segment}，需拼回完整两段路径，
 * 否则算出来是 /categories 而菜单 key 是 /admin/categories，永远匹配不上（选中态不生效）。
 * 「题目编辑」页 /admin/question-edit/:id 归入「题库管理」高亮。
 */
const activeKey = computed(() => {
  const segment = route.path.split('/')[2] ?? ''
  if (segment === 'question-edit') return '/admin/questions'
  return segment ? `/admin/${segment}` : '/admin/dashboard'
})

const userOptions = [{ label: '返回前台', key: 'home' }, { label: '退出登录', key: 'logout' }]

/**
 * 侧边栏菜单配色。
 *
 * naive-ui 把这些主题变量以「行内样式」写在 .n-menu 元素上，
 * 普通 CSS 选择器（含 :deep）都覆盖不了，必须通过 theme-overrides 传入。
 *
 * 关键点：`itemColorActiveHover` 故意与 `itemColorActive` 取同一个值 ——
 * 否则鼠标停在已选中项上时，背景会从选中色变成悬停色（默认实现是
 * 主色 10% 透明，与本项目品牌色深浅不同），出现「深绿→浅绿」的跳变。
 */
const menuThemeOverrides: MenuThemeOverrides = {
  itemColorHover: 'var(--bg-soft)',
  itemColorActive: 'var(--brand-50)',
  itemColorActiveHover: 'var(--brand-50)',
  itemColorActiveCollapsed: 'var(--brand-50)',
  itemTextColorActive: 'var(--brand-700)',
  itemTextColorActiveHover: 'var(--brand-700)',
  itemIconColorActive: 'var(--brand-500)',
  itemIconColorActiveHover: 'var(--brand-500)'
}

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
        :theme-overrides="menuThemeOverrides"
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
            <n-avatar round :size="28" style="background: var(--brand-500)">
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

/* ---------- 侧边栏菜单：选中态 ---------- */
/*
  配色统一由 JS 侧的 menuThemeOverrides 注入（原因见其注释），
  这里只负责 naive-ui 没有、需要额外补的两件事：
*/
/* 1) 选中项文字加粗 */
.admin-layout :deep(.n-menu-item-content--selected .n-menu-item-content-header) {
  font-weight: 600;
}

/* 2) 选中项左侧强调条：naive-ui 的菜单未占用 ::after，可安全使用。
      注意不要用 ::before —— 那是它绘制「选中背景胶囊」的载体。 */
.admin-layout :deep(.n-menu-item-content--selected::after) {
  content: '';
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background-color: var(--brand-500);
  pointer-events: none;
}

/* 折叠态下菜单只剩图标，强调条会挤在图标左侧，隐藏 */
.admin-layout :deep(.n-menu--collapsed .n-menu-item-content--selected::after) {
  display: none;
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
  background: var(--grad-brand);
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.logo-text {
  font-weight: 700;
  color: var(--brand-700);
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
