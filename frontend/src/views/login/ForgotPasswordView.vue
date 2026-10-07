<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { getCaptcha, resetPassword, sendEmailCode } from '@/api/auth'
import { captchaImageSrc } from '@/utils/format'
import type { CaptchaVO } from '@/types'

const router = useRouter()
const message = useMessage()

const form = reactive({
  email: '',
  captchaCode: '',
  mailCode: '',
  newPassword: '',
  confirmPassword: ''
})

const sending = ref(false)
const submitting = ref(false)
const captcha = ref<CaptchaVO | null>(null)
/** 发码冷却倒计时秒数，与后端「同邮箱 60 秒冷却」对齐 */
const cooldown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

function startCooldown() {
  cooldown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 发码需要图形验证码（后端强制校验，且一次性销毁）。
// 拉取本身失败时不能抛到调用方（如 onSendCode 的 finally），否则会盖掉真实错误分支。
async function refreshCaptcha() {
  try {
    captcha.value = await getCaptcha()
  } catch {
    // 拦截器已提示；保持旧图，用户可点击图片重试
  }
  form.captchaCode = ''
}

onMounted(refreshCaptcha)

/**
 * 发送邮箱验证码。
 *
 * 后端顺序：先校验图形验证码 → 再置 60 秒冷却 → 再发信。由此得出两条前端规则：
 * 1. 「图形验证码错误」发生在写冷却之前，冷却未生效，可以立即重试，不进倒计时；
 * 2. 「发送过于频繁」发生在写冷却之后，必须倒计时。
 *
 * 无论成功失败都要换一张图形码：后端 verifyGraphCaptcha 校验后立即删除缓存，
 * 旧图必然失效。成功后不换会让用户盯着废图，冷却结束第一次重发必然失败。
 *
 * 注意：响应拦截器已对非 0 code 统一 message.error。
 * 这里只补拦截器做不到的事：频繁/上限时开始倒计时，不再叠一条 warning。
 */
async function onSendCode() {
  if (!form.email) {
    message.warning('请先填写邮箱')
    return
  }
  if (!form.captchaCode) {
    message.warning('请输入图形验证码')
    return
  }
  sending.value = true
  try {
    await sendEmailCode({
      email: form.email,
      captchaId: captcha.value?.captchaId ?? '',
      captchaCode: form.captchaCode
    })
    message.success('验证码已发送，请查收邮件（10 分钟内有效）')
    startCooldown()
  } catch (e) {
    const msg = (e as { message?: string }).message ?? ''
    if (msg.includes('频繁') || msg.includes('上限')) {
      startCooldown()
    }
  } finally {
    // 图形码一次性销毁，成功也必须换新。换图失败不能挡住按钮复位，用户还可点图片重试。
    try {
      await refreshCaptcha()
    } catch {
      /* 拦截器已提示网络错误 */
    }
    sending.value = false
  }
}

async function onSubmit() {
  if (!form.email) {
    message.warning('请填写注册时使用的邮箱')
    return
  }
  if (!form.mailCode) {
    message.warning('请输入邮箱验证码')
    return
  }
  if (!form.newPassword) {
    message.warning('请输入新密码')
    return
  }
  if (form.newPassword.length < 6 || form.newPassword.length > 32) {
    message.warning('密码长度须为 6-32 位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  submitting.value = true
  try {
    await resetPassword({
      email: form.email,
      mailCode: form.mailCode,
      newPassword: form.newPassword
    })
    message.success('密码重置成功，请用新密码登录')
    router.push('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>找回密码</h1>
      <p class="subtitle">通过注册时绑定的邮箱验证身份后重置密码</p>
      <n-form label-placement="left" :show-label="false" size="large">
        <n-form-item>
          <n-input v-model:value="form.email" placeholder="注册时绑定的邮箱" />
        </n-form-item>
        <n-form-item>
          <div class="captcha-row" style="width: 100%">
            <n-input
              v-model:value="form.captchaCode"
              placeholder="图形验证码"
              :input-props="{ autocomplete: 'off' }"
              style="flex: 1"
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
        <n-form-item>
          <div class="code-row" style="width: 100%">
            <n-input
              v-model:value="form.mailCode"
              placeholder="邮箱验证码"
              :input-props="{ autocomplete: 'off' }"
              style="flex: 1"
              @keyup.enter="onSubmit"
            />
            <n-button
              class="code-btn"
              type="primary"
              secondary
              :loading="sending"
              :disabled="cooldown > 0"
              @click="onSendCode"
            >
              {{ cooldown > 0 ? `${cooldown} 秒后重发` : '获取验证码' }}
            </n-button>
          </div>
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.newPassword" type="password" show-password-on="click" placeholder="新密码（6-32 位）" />
        </n-form-item>
        <n-form-item>
          <n-input
            v-model:value="form.confirmPassword"
            type="password"
            show-password-on="click"
            placeholder="确认新密码"
            @keyup.enter="onSubmit"
          />
        </n-form-item>
        <n-button type="primary" block size="large" :loading="submitting" @click="onSubmit">
          重置密码
        </n-button>
      </n-form>
      <p class="tips">
        想起来了？
        <a class="link" @click="router.push('/login')">返回登录</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.subtitle {
  text-align: center;
  color: var(--ink-400);
  font-size: 13px;
  margin: 6px 0 20px;
}

/* 验证码行：与登录/注册页保持一致的输入框 + 可点击刷新的图片 */
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.captcha-img {
  width: 130px;
  height: 48px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid var(--border-1);
}

.code-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.code-btn {
  flex-shrink: 0;
  min-width: 118px;
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
</style>
