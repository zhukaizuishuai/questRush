import { get } from './request'
import type { CategoryVO } from '@/types'

/** 分类树 */
export function getCategoryTree() {
  return get<CategoryVO[]>('/category/list')
}
