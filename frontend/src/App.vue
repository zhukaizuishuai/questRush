<script setup lang="ts">
import { zhCN, dateZhCN, NConfigProvider, NMessageProvider, NDialogProvider } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'

/** 品牌主题覆盖：Emerald 绿主色 + 柔和辅助色（对应设计稿视觉体系） */
const themeOverrides: GlobalThemeOverrides = {
  common: {
    fontFamily:
      "'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
    primaryColor: '#10B981',
    primaryColorHover: '#0EA271',
    primaryColorPressed: '#059669',
    primaryColorSuppl: '#0EA271',
    infoColor: '#6B8AFF',
    infoColorHover: '#5A7BF5',
    infoColorPressed: '#4E6BE8',
    infoColorSuppl: '#5A7BF5',
    successColor: '#10B981',
    successColorHover: '#0EA271',
    successColorPressed: '#059669',
    successColorSuppl: '#0EA271',
    warningColor: '#F0A020',
    errorColor: '#F43F5E',
    errorColorHover: '#E2304F',
    errorColorPressed: '#C92846',
    borderRadius: '10px',
    borderRadiusSmall: '8px'
  },
  Button: {
    // 主要操作按钮使用中圆角，避免过方
    borderRadiusMedium: '12px',
    borderRadiusLarge: '14px',
    borderRadiusSmall: '10px'
  },
  Card: {
    borderRadius: '16px'
  }
}
</script>

<template>
  <n-config-provider :locale="zhCN" :date-locale="dateZhCN" :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <router-view />
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
