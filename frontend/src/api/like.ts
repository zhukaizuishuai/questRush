import { post } from './request'

/** 点赞 / 取消点赞（toggle），返回最新计数与状态 */
export function toggleLike(questionId: number) {
  return post<{ liked: boolean; likeCount: number }>('/like/toggle', { questionId })
}

export default { toggleLike }
