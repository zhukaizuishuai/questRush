import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { createDiscreteApi } from 'naive-ui'
import type { Result } from '@/types'

/** token 本地存储 key */
export const TOKEN_KEY = 'questrush_token'
export const USER_KEY = 'questrush_user'

const { message, dialog } = createDiscreteApi(['message', 'dialog'])

const instance: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：注入 sa-token（header 名 Authorization）
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['Authorization'] = token
  }
  return config
})

function clearAuthAndGoLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  message.error('登录已失效，请重新登录')
  // 动态引入避免循环依赖
  import('@/router').then(({ default: router }) => {
    const current = router.currentRoute.value
    if (current.path !== '/login') {
      router.push({ path: '/login', query: { redirect: current.fullPath } })
    }
  })
}

/** 会员引导弹窗（防重复弹出） */
let vipDialogShowing = false
function showVipDialog() {
  if (vipDialogShowing) return
  vipDialogShowing = true
  dialog.warning({
    title: '需要开通会员',
    content: '该题目为 VIP 专属题目，开通会员即可解锁全部题库与解析。',
    positiveText: '立即开通',
    negativeText: '暂不需要',
    onPositiveClick: () => {
      import('@/router').then(({ default: router }) => router.push('/vip'))
    },
    onClose: () => {
      vipDialogShowing = false
    },
    onNegativeClick: () => {
      vipDialogShowing = false
    }
  })
}

// 响应拦截：统一处理 code !== 0
instance.interceptors.response.use(
  (response) => {
    // 文件下载直接放行
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data as Result
    if (res.code === 0) {
      return res.data as never
    }
    switch (res.code) {
      case 401:
        clearAuthAndGoLogin()
        break
      case 40301:
        showVipDialog()
        break
      case 403:
        message.error('无权限执行该操作')
        break
      case 422:
        message.error(res.message || '参数校验失败')
        break
      default:
        message.error(res.message || `请求失败（${res.code}）`)
    }
    // 挂载完整返回体，供调用方读取扩展字段（如登录的 requireCaptcha）
    const err = new Error(res.message || `请求失败（${res.code}）`)
    ;(err as unknown as { result?: Result }).result = res
    return Promise.reject(err)
  },
  (error) => {
    if (error?.response) {
      const { status } = error.response
      if (status === 401) {
        clearAuthAndGoLogin()
      } else if (status === 403) {
        message.error('无权限访问')
      } else {
        message.error(error.message || '网络异常，请稍后重试')
      }
    } else {
      message.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

/** GET 请求，返回已解包的 data */
export function get<T>(url: string, params?: object): Promise<T> {
  return instance.get(url, { params }) as Promise<T>
}

/** POST 请求 */
export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return instance.post(url, data, config) as Promise<T>
}

/** PUT 请求 */
export function put<T>(url: string, data?: unknown): Promise<T> {
  return instance.put(url, data) as Promise<T>
}

/** DELETE 请求 */
export function del<T>(url: string, params?: object): Promise<T> {
  return instance.delete(url, { params }) as Promise<T>
}

/** 文件上传 */
export function upload<T>(url: string, formData: FormData): Promise<T> {
  return instance.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<T>
}

/** 文件下载（携带 token），返回 Blob */
export async function download(url: string, params?: object, filename?: string) {
  const resp = await instance.get(url, { params, responseType: 'blob' })
  const blob = resp.data as Blob
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename || 'download'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

export default instance
