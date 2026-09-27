import { defineStore } from 'pinia'
import type { UserInfo } from '@/types'
import { TOKEN_KEY, USER_KEY } from '@/api/request'
import * as authApi from '@/api/auth'

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readStoredUser()
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    /** VIP 是否有效：vipExpireTime > now（管理员不受限，由后端判定） */
    isVip: (state): boolean => {
      const t = state.userInfo?.vipExpireTime
      if (!t) return false
      return new Date(t).getTime() > Date.now()
    },
    isAdmin: (state): boolean => state.userInfo?.role === 'admin',
    vipExpireTime: (state) => state.userInfo?.vipExpireTime ?? null
  },

  actions: {
    setAuth(token: string, userInfo: UserInfo) {
      this.token = token
      this.userInfo = userInfo
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },

    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
    },

    /** 登录（失败 ≥3 次时后端要求验证码，由调用方捕获处理） */
    async login(username: string, password: string, captchaId?: string, captchaCode?: string) {
      const data = await authApi.login({ username, password, captchaId, captchaCode })
      this.setAuth(data.token, data.user)
      return data
    },

    /** 拉取最新用户信息（登录后 / 刷新 VIP 状态后调用） */
    async fetchUserInfo() {
      if (!this.token) return
      try {
        const info = await authApi.getUserInfo()
        this.setUserInfo(info)
      } catch {
        // 401 等已在拦截器统一处理
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 忽略登出接口异常
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
