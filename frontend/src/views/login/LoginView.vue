<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { getCaptcha } from '@/api/auth'
import { captchaImageSrc } from '@/utils/format'
import type { CaptchaVO } from '@/types'

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  captchaCode: ''
})

const loading = ref(false)
/** 登录页始终显示验证码（产品调整 2026-09-27），后端「带了就校验」 */
const showCaptcha = ref(true)
const captcha = ref<CaptchaVO | null>(null)

onMounted(refreshCaptcha)

async function refreshCaptcha() {
  captcha.value = await getCaptcha()
}

async function onSubmit() {
  if (!form.username || !form.password) {
    message.warning('请输入用户名和密码')
    return
  }
  if (showCaptcha.value && !form.captchaCode) {
    message.warning('请输入图形验证码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password, showCaptcha.value ? captcha.value?.captchaId : undefined, showCaptcha.value ? form.captchaCode : undefined)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (e) {
    // 提交失败后自动刷新验证码，避免拿已失效的图反复提交
    form.captchaCode = ''
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page split">
    <!-- 左侧品牌面板（窄屏自动隐藏） -->
    <div class="brand-panel">
      <div class="brand-logo">
        <span class="brand-logo-mark">
          <svg width="18" height="18" viewBox="0 0 16 16" fill="none">
            <path d="M9.25 1.5L4.5 8h3l-.75 4.5L11.5 6h-3l.75-4.5z" fill="#fff" />
          </svg>
        </span>
        <span class="brand-logo-text">QuestRush</span>
      </div>
      <div class="brand-body">
        <h1 class="brand-slogan">把每一道错题，<br />变成面试的底气</h1>
        <p class="brand-sub">顺序刷题 · 随机练习 · 遗忘曲线复习，一站式备战技术面试</p>
        <div class="brand-feature">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <circle cx="9" cy="9" r="8" fill="#fff" fill-opacity="0.2" />
            <path d="M5.5 9.2L7.8 11.5L12.5 6.8" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>四大题型自动判分，作答即刻出解析</span>
        </div>
        <div class="brand-feature">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <circle cx="9" cy="9" r="8" fill="#fff" fill-opacity="0.2" />
            <path d="M5.5 9.2L7.8 11.5L12.5 6.8" stroke="#fff" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <span>错题自动归集，按遗忘曲线科学安排复习</span>
        </div>
      </div>
      <div class="brand-footer">QuestRush · 在线刷题学习平台</div>
    </div>

    <!-- 右侧表单 -->
    <div class="form-panel">
      <div class="form-inner">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-subtitle">登录 QuestRush，继续你的刷题计划</p>
        <n-form label-placement="left" :show-label="false" size="large">
          <n-form-item>
            <n-input v-model:value="form.username" placeholder="用户名" @keyup.enter="onSubmit" />
          </n-form-item>
          <n-form-item>
            <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="密码" @keyup.enter="onSubmit" />
          </n-form-item>
          <n-form-item v-if="showCaptcha">
            <div class="captcha-row" style="width: 100%">
              <n-input
                v-model:value="form.captchaCode"
                placeholder="图形验证码"
                :input-props="{ autocomplete: 'off' }"
                style="flex: 1"
                @keyup.enter="onSubmit"
              />
              <img
                v-if="captcha"
                class="captcha-img"
                :src="captchaImageSrc(captcha.image)"
                title="点击刷新验证码"
                @click="refreshCaptcha"
              />
            </div>
          </n-form-item>
          <n-button type="primary" block size="large" :loading="loading" @click="onSubmit">
            登录
          </n-button>
        </n-form>
        <p class="tips">还没有账号？<a class="link" @click="goRegister">立即注册</a></p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 左侧品牌面板 */
.brand-panel {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  width: 44%;
  min-width: 420px;
  min-height: 100vh;
  padding: 56px 64px;
  color: #fff;
  background:
    radial-gradient(400px 400px at 86% 12%, rgba(120, 255, 200, 0.22) 0%, rgba(120, 255, 200, 0) 100%),
    radial-gradient(360px 360px at 6% 92%, rgba(110, 190, 255, 0.2) 0%, rgba(110, 190, 255, 0) 100%),
    linear-gradient(150deg, #0a8f68 0%, #0b7a71 100%);
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.brand-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  max-width: 520px;
}

.brand-slogan {
  font-size: 34px;
  line-height: 1.45;
  margin: 0 0 4px;
  letter-spacing: 0.01em;
}

.brand-sub {
  font-size: 15px;
  opacity: 0.92;
  margin: 0 0 12px;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.brand-footer {
  font-size: 12px;
  opacity: 0.75;
}

/* 右侧表单 */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.form-inner {
  width: 360px;
  padding: 24px 0;
}

.form-title {
  font-size: 26px;
  margin: 0 0 6px;
  color: var(--ink-900);
}

.form-subtitle {
  font-size: 14px;
  color: var(--ink-600);
  margin: 0 0 28px;
}

.tips {
  text-align: center;
  color: var(--ink-400);
  font-size: 13px;
  margin-top: 16px;
}

.link {
  color: var(--brand-700);
  cursor: pointer;
  font-weight: 500;
}

/* 窄屏隐藏品牌面板，退化为居中卡片 */
@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }

  .form-panel {
    background: var(--bg-page);
  }

  .form-inner {
    width: 420px;
    max-width: calc(100vw - 40px);
    background: #fff;
    border: 1px solid var(--border-1);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-card);
    padding: 36px;
  }
}
</style>
