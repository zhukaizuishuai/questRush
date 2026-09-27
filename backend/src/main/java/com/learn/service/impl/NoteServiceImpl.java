package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.checker.QuestionAccessChecker;
import com.learn.entity.Question;
import com.learn.entity.User;
import com.learn.entity.UserNote;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.UserMapper;
import com.learn.mapper.UserNoteMapper;
import com.learn.service.NoteService;
import com.learn.vo.NoteVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 笔记服务实现（文档 3.8 / 4.1）。
 * 保存必须走 upsert：唯一键不含 deleted，「先删后插」会撞唯一键；
 * upsert 会把已软删的笔记 deleted 置回 0，复用同一行。
 */
@Service
public class NoteServiceImpl implements NoteService {

    @Resource
    private UserNoteMapper noteMapper;

    @Resource
    private QuestionAccessChecker checker;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public void save(Long questionId, String content, Long userId) {
        // 防给 VIP 题写笔记（文档 4.1 鉴权矩阵）
        checker.checkReadable(questionId, userId);
        noteMapper.upsertNote(userId, questionId, content);
    }

    @Override
    public NoteVO detail(Long questionId, Long userId) {
        // 防通过笔记读取 VIP 题内容（文档 4.1）
        checker.checkReadable(questionId, userId);
        // MP 逻辑删除：自动过滤 deleted=0
        UserNote note = noteMapper.selectOne(new LambdaQueryWrapper<UserNote>()
                .eq(UserNote::getUserId, userId)
                .eq(UserNote::getQuestionId, questionId));
        NoteVO vo = new NoteVO();
        vo.setQuestionId(questionId);
        vo.setContent(note == null ? "" : note.getContent());
        vo.setUpdateTime(note == null ? null : note.getUpdateTime());
        return vo;
    }

    @Override
    public IPage<NoteVO> list(long pageNum, long pageSize, Long userId) {
        Page<NoteVO> page = new Page<>(pageNum, Math.min(pageSize, 100));
        IPage<NoteVO> result = noteMapper.selectNotePage(page, userId);
        // 越权条目脱敏（文档 4.1）：VIP 题在无权限时题干置空，只保留 id 与内容不可见
        User user = userMapper.selectById(userId);
        boolean admin = user != null && "admin".equals(user.getRole());
        if (!admin) {
            boolean vipValid = checker.isVipValid(user);
            for (NoteVO vo : result.getRecords()) {
                if (vo.getQuestionTitle() != null) {
                    Question q = questionMapper.selectById(vo.getQuestionId());
                    if (q != null && q.getIsVip() == 1 && !vipValid) {
                        vo.setQuestionTitle(null);
                    }
                }
            }
        }
        return result;
    }
}
