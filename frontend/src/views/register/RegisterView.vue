<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NButton, NForm, NFormItem, NInput, useMessage } from 'naive-ui'
import { getCaptcha, register } from '@/api/auth'
import { captchaImageSrc } from '@/utils/format'
import type { CaptchaVO } from '@/types'

const router = useRouter()
const message = useMessage()

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  captchaCode: ''
})

const loading = ref(false)
const captcha = ref<CaptchaVO | null>(null)

// 注册强制图形验证码
async function refreshCaptcha() {
  captcha.value = await getCaptcha()
}

onMounted(refreshCaptcha)

async function onSubmit() {
  if (!form.username || !form.password) {
    message.warning('请输入用户名和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    message.warning('两次输入的密码不一致')
    return
  }
  if (!form.captchaCode) {
    message.warning('请输入图形验证码')
    return
  }
  loading.value = true
  try {
    await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      email: form.email || undefined,
      captchaId: captcha.value?.captchaId ?? '',
      captchaCode: form.captchaCode
    })
    message.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // 提交失败后自动刷新验证码
    form.captchaCode = ''
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>注册账号</h1>
      <p class="subtitle">加入 QuestRush，开启高效刷题之旅</p>
      <n-form label-placement="left" :show-label="false" size="large">
        <n-form-item>
          <n-input v-model:value="form.username" placeholder="用户名" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.password" type="password" show-password-on="click" placeholder="密码" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.confirmPassword" type="password" show-password-on="click" placeholder="确认密码" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.nickname" placeholder="昵称（选填）" />
        </n-form-item>
        <n-form-item>
          <n-input v-model:value="form.email" placeholder="邮箱（选填，用于找回密码）" />
        </n-form-item>
        <n-form-item>
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
          注册
        </n-button>
      </n-form>
      <p class="tips">已有账号？<a class="link" @click="router.push('/login')">去登录</a></p>
    </div>
  </div>
</template>

<style scoped>
.tips {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin-top: 16px;
}

.link {
  color: #18a058;
  cursor: pointer;
}
</style>
