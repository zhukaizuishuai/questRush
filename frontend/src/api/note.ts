import { get, post } from './request'
import type { NoteVO, PageResult } from '@/types'

/** 保存笔记（upsert） */
export function saveNote(data: { questionId: number; content: string }) {
  return post<null>('/note/save', data)
}

/** 获取某题笔记 */
export function noteDetail(questionId: number) {
  return get<NoteVO | null>('/note/detail', { questionId })
}

/** 笔记列表（本人，分页；越权条目 questionTitle 为 null） */
export function noteList(params?: { pageNum?: number; pageSize?: number }) {
  return get<PageResult<NoteVO>>('/note/list', params)
}

export default { saveNote, noteDetail, noteList }
