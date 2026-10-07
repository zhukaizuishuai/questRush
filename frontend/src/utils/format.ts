/** 题型标签：1 单选 2 多选 3 判断 4 简答 */
export const TYPE_OPTIONS = [
  { label: '单选题', value: 1 },
  { label: '多选题', value: 2 },
  { label: '判断题', value: 3 },
  { label: '简答题', value: 4 }
]

export function typeLabel(type: number): string {
  return TYPE_OPTIONS.find((o) => o.value === type)?.label ?? '未知'
}

export const DIFFICULTY_OPTIONS = [
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 }
]

export function difficultyLabel(d: number): string {
  return DIFFICULTY_OPTIONS.find((o) => o.value === d)?.label ?? '未知'
}

/** naive-ui tag 类型，用于难度着色 */
export function difficultyTagType(d: number): 'default' | 'success' | 'warning' | 'error' {
  if (d === 1) return 'success'
  if (d === 2) return 'warning'
  return 'error'
}

/** 格式化时间 yyyy-MM-dd HH:mm */
export function formatTime(time?: string | null): string {
  if (!time) return '-'
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 格式化日期 yyyy-MM-dd */
export function formatDate(time?: string | null): string {
  if (!time) return '-'
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 验证码图片 src 兼容：后端可能返回完整 data URL 或裸 base64 */
export function captchaImageSrc(image: string): string {
  return image.startsWith('data:') ? image : `data:image/png;base64,${image}`
}

/**
 * 时间戳 → 后端 LocalDateTime 可解析的本地时间串 yyyy-MM-ddTHH:mm:ss。
 * 注意不要用 Date#toISOString()（带 Z 的 UTC 串），后端字段是 LocalDateTime，反序列化会 422。
 */
export function toLocalDateTimeString(time: number | string | null | undefined): string | null {
  if (time === null || time === undefined || time === '') return null
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return null
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
