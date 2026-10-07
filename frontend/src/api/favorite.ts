import { get, post } from './request'
import type { FavoriteVO, PageResult } from '@/types'

/** 收藏 / 取消收藏 */
export function toggleFavorite(questionId: number) {
  return post<{ favorited: boolean }>('/favorite/toggle', { questionId })
}

/** 收藏列表（越权条目脱敏：title 为 null） */
export function favoriteList(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<FavoriteVO>>('/favorite/list', params)
}
