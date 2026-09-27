import { get, post, put } from './request'
import type { CaptchaVO, LoginVO, UserInfo } from '@/types'

/** 获取图形验证码 */
export function getCaptcha() {
  return get<CaptchaVO>('/auth/captcha')
}

export interface LoginParams {
  username: string
  password: string
  captchaId?: string
  captchaCode?: string
}

export interface LoginResult extends LoginVO {
  /** 登录失败 ≥3 次后为 true，前端需展示验证码输入框 */
  requireCaptcha?: boolean
}

/** 登录 */
export function login(data: LoginParams) {
  return post<LoginResult>('/auth/login', data)
}

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  email?: string
  captchaId: string
  captchaCode: string
}

/** 注册（强制图形验证码） */
export function register(data: RegisterParams) {
  return post<null>('/auth/register', data)
}

/** 退出登录 */
export function logout() {
  return post<null>('/auth/logout')
}

/** 发送邮箱验证码（找回密码 / 绑定邮箱）——后端强制图形验证码，必须带上 captchaId+captchaCode */
export function sendEmailCode(data: { email: string; captchaId: string; captchaCode: string }) {
  return post<null>('/auth/email-code', data)
}

/** 邮箱验证码重置密码（字段名 mailCode 与后端 ResetPasswordDTO 对齐） */
export function resetPassword(data: { email: string; mailCode: string; newPassword: string }) {
  return post<null>('/auth/reset-password', data)
}

/** 个人信息（含 VIP 状态与到期时间） */
export function getUserInfo() {
  return get<UserInfo>('/user/info')
}

/** 修改昵称、邮箱 */
export function updateUser(data: { nickname: string; email?: string }) {
  return put<null>('/user/update', data)
}

/** 修改密码（需校验原密码） */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return put<null>('/user/password', data)
}

/** 上传头像（≤2MB，jpg/png/webp），返回头像地址 */
export function uploadAvatar(formData: FormData) {
  return post<string>('/user/avatar', formData)
}

export { getCaptcha as captcha }
export default { getCaptcha, login, register, logout, getUserInfo, updateUser, updatePassword, uploadAvatar }
