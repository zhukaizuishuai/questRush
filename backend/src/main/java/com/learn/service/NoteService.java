package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.vo.NoteVO;

/**
 * 笔记服务（文档 3.8）：upsert 保存，仅本人可见
 */
public interface NoteService {

    /** 保存笔记（upsert：ON DUPLICATE KEY UPDATE content=VALUES(content), deleted=0） */
    void save(Long questionId, String content, Long userId);

    /** 获取某题笔记（本人），无笔记返回空对象 */
    NoteVO detail(Long questionId, Long userId);

    /** 笔记列表（本人，按更新时间倒序；越权条目题干脱敏） */
    IPage<NoteVO> list(long pageNum, long pageSize, Long userId);
}
