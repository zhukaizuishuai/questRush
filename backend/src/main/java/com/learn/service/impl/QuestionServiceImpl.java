package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.checker.QuestionAccessChecker;
import com.learn.entity.Question;
import com.learn.entity.QuestionCategory;
import com.learn.entity.QuestionLike;
import com.learn.entity.UserFavorite;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.mapper.QuestionLikeMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.QuestionOptionMapper;
import com.learn.mapper.UserFavoriteMapper;
import com.learn.service.QuestionService;
import com.learn.util.MarkdownUtil;
import com.learn.vo.QuestionListVO;
import com.learn.vo.QuestionOptionVO;
import com.learn.vo.QuestionPracticeVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 题目服务实现。
 * 列表 / 搜索在 SQL 层按 is_vip 过滤（文档 4.1 鉴权矩阵第 1、2 行）；
 * 详情逐条走 checker，防 id 遍历（文档 4.1）。
 */
@Service
public class QuestionServiceImpl implements QuestionService {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Resource
    private QuestionOptionMapper optionMapper;

    @Resource
    private UserFavoriteMapper favoriteMapper;

    @Resource
    private QuestionLikeMapper likeMapper;

    @Resource
    private QuestionAccessChecker checker;

    @Override
    public IPage<QuestionListVO> pageList(long pageNum, long pageSize, Long categoryId,
                                          String keyword, Integer difficulty, Long userId) {
        List<Long> categoryIds = resolveCategoryIds(categoryId);
        // 非管理员且 VIP 无效 → SQL 层只查免费题（文档 4.1）
        boolean vipFilter = !checker.canReadVip(userId);
        Page<QuestionListVO> page = new Page<>(pageNum, Math.min(pageSize, 100));
        IPage<QuestionListVO> result = questionMapper.selectPageList(page, categoryIds, keyword, difficulty, vipFilter);
        // 列表题干输出纯文本摘要（文档 4.2）
        result.getRecords().forEach(vo -> vo.setTitle(MarkdownUtil.summary(vo.getTitle(), 120)));
        return result;
    }

    @Override
    public QuestionPracticeVO detail(Long questionId, Long userId) {
        Question question = checker.checkReadable(questionId, userId);
        // selectPracticeById 不 select answer/answerText/analysis（文档 4.2 答案字段分级）
        QuestionPracticeVO vo = questionMapper.selectPracticeById(question.getId());
        if (vo == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        vo.setOptions(optionMapper.selectByQuestionId(question.getId()).stream()
                .map(o -> new QuestionOptionVO(o.getOptionCode(), o.getOptionContent()))
                .toList());
        vo.setFavorited(favoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getQuestionId, questionId)) > 0);
        // 点赞初始状态与计数（LikeButton 需要，文档 4.6）
        vo.setLiked(likeMapper.selectCount(new LambdaQueryWrapper<QuestionLike>()
                .eq(QuestionLike::getUserId, userId)
                .eq(QuestionLike::getQuestionId, questionId)) > 0);
        Question full = questionMapper.selectById(questionId);
        vo.setLikeCount(full == null || full.getLikeCount() == null ? 0 : full.getLikeCount());
        return vo;
    }

    /**
     * 分类 id → 该分类及其子分类 id 列表（题目只挂叶子，查一级分类时需带出子分类）
     */
    public List<Long> resolveCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        List<Long> ids = categoryMapper.selectList(new LambdaQueryWrapper<QuestionCategory>()
                        .eq(QuestionCategory::getParentId, categoryId))
                .stream().map(QuestionCategory::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            ids = Collections.singletonList(categoryId);
        } else {
            ids.add(categoryId);
        }
        return ids;
    }
}
