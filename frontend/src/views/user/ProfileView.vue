<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { NButton, NForm, NFormItem, NInput, NUpload, useMessage, type UploadCustomRequestOptions, type UploadFileInfo } from 'naive-ui'
import { updatePassword, updateUser, uploadAvatar } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const message = useMessage()
const userStore = useUserStore()

const profileForm = reactive({
  nickname: '',
  email: ''
})
const profileSaving = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordSaving = ref(false)

onMounted(() => {
  profileForm.nickname = userStore.userInfo?.nickname ?? ''
  profileForm.email = userStore.userInfo?.email ?? ''
})

async function saveProfile() {
  profileSaving.value = true
  try {
    // 邮箱传空串表示解绑（后端会写 NULL）；传 undefined 会被后端当作「不修改」
    await updateUser({ nickname: profileForm.nickname, email: profileForm.email ?? '' })
    await userStore.fetchUserInfo()
    message.success('个人信息已更新')
  } finally {
    profileSaving.value = false
  }
}

async function savePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    message.warning('请填写完整')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    message.success('密码修改成功，请重新登录体验新密码')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally {
    passwordSaving.value = false
  }
}

/** 头像上传：≤2MB，jpg/png/webp */
async function avatarCustomRequest({ file, onFinish, onError }: UploadCustomRequestOptions) {
  const f = file.file
  if (!f) {
    onError()
    return
  }
  if (f.size > 2 * 1024 * 1024) {
    message.error('头像不能超过 2MB')
    onError()
    return
  }
  const formData = new FormData()
  formData.append('file', f)
  try {
    await uploadAvatar(formData)
    await userStore.fetchUserInfo()
    message.success('头像已更新')
    onFinish()
  } catch {
    onError()
  }
}

function beforeUpload({ file }: { file: UploadFileInfo }) {
  const ok = ['image/jpeg', 'image/png', 'image/webp'].includes(file.file?.type ?? '')
  if (!ok) message.error('仅支持 jpg / png / webp 格式')
  return ok
}
</script>

<template>
  <div class="page-container profile-page">
    <div class="profile-grid">
      <!-- 左：头像与 VIP 状态 -->
      <div class="card left">
        <div class="avatar-area">
          <img v-if="userStore.userInfo?.avatar" class="avatar" :src="userStore.userInfo.avatar" alt="头像" />
          <div v-else class="avatar placeholder">{{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '用').slice(0, 1) }}</div>
          <n-upload
            :custom-request="avatarCustomRequest"
            :show-file-list="false"
            accept="image/jpeg,image/png,image/webp"
            :before-upload="beforeUpload"
          >
            <n-button size="small" secondary>更换头像</n-button>
          </n-upload>
        </div>
        <div class="user-meta">
          <div class="username">{{ userStore.userInfo?.username }}</div>
          <div class="role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</div>
          <div class="vip-line">
            <span v-if="userStore.isVip" class="vip-tag">VIP 会员</span>
            <span v-else class="vip-tag grey">未开通会员</span>
          </div>
          <div class="expire">
            {{ userStore.vipExpireTime ? `有效期至 ${formatDate(userStore.vipExpireTime)}` : '开通会员解锁全部题库' }}
          </div>
        </div>
      </div>

      <!-- 右：信息修改与密码修改 -->
      <div class="right-col">
        <div class="card">
          <div class="card-title">个人信息</div>
          <n-form label-placement="left" label-width="80">
            <n-form-item label="用户名">
              <n-input :value="userStore.userInfo?.username" disabled />
            </n-form-item>
            <n-form-item label="昵称">
              <n-input v-model:value="profileForm.nickname" placeholder="昵称" />
            </n-form-item>
            <n-form-item label="邮箱">
              <n-input v-model:value="profileForm.email" placeholder="邮箱（用于找回密码，留空则解绑）" />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="profileSaving" @click="saveProfile">保存修改</n-button>
            </n-form-item>
          </n-form>
        </div>

        <div class="card">
          <div class="card-title">修改密码</div>
          <n-form label-placement="left" label-width="80">
            <n-form-item label="原密码">
              <n-input v-model:value="passwordForm.oldPassword" type="password" show-password-on="click" placeholder="原密码" />
            </n-form-item>
            <n-form-item label="新密码">
              <n-input v-model:value="passwordForm.newPassword" type="password" show-password-on="click" placeholder="新密码" />
            </n-form-item>
            <n-form-item label="确认密码">
              <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" placeholder="再次输入新密码" />
            </n-form-item>
            <n-form-item label=" ">
              <n-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</n-button>
            </n-form-item>
          </n-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  align-items: start;
}

.card {
  background: #fff;
  border: 1px solid #eceef1;
  border-radius: 12px;
  padding: 24px;
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-title {
  font-weight: 700;
  margin-bottom: 18px;
}

.avatar-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-bottom: 18px;
  border-bottom: 1px solid #f0f1f3;
}

.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar.placeholder {
  background: #18a058;
  color: #fff;
  font-size: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-meta {
  text-align: center;
  padding-top: 16px;
}

.username {
  font-weight: 700;
  font-size: 16px;
}

.role {
  color: #888;
  font-size: 13px;
  margin-top: 4px;
}

.vip-line {
  margin-top: 12px;
}

.vip-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #8a5a00;
  background: linear-gradient(135deg, #ffe9b8, #ffd77a);
}

.vip-tag.grey {
  color: #999;
  background: #f0f1f3;
}

.expire {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}
</style>
