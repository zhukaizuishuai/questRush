package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.checker.QuestionAccessChecker;
import com.learn.dto.PracticeSessionDTO;
import com.learn.dto.SubmitDTO;
import com.learn.entity.Question;
import com.learn.entity.UserAnswerLog;
import com.learn.entity.UserQuestionRecord;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.UserAnswerLogMapper;
import com.learn.mapper.UserFavoriteMapper;
import com.learn.mapper.UserQuestionRecordMapper;
import com.learn.service.PracticeService;
import com.learn.util.CacheService;
import com.learn.util.PageUtil;
import com.learn.vo.QuestionPracticeVO;
import com.learn.vo.QuestionSubmitVO;
import com.learn.vo.QuestionWrongVO;
import com.learn.vo.StatsVO;
import com.learn.vo.WrongBookPageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 刷题服务实现（文档 4.3）：
 * - 刷题会话：服务端一次性生成有序 id 列表存缓存（TTL 2h），前端按 index 拉取，
 *   替代 ORDER BY RAND()，翻页无重复无漏题
 * - 判分：单选相等；多选按字符排序拼接归一化；判断题按 A/B；简答不判分
 * - 状态表 upsert 原子累加；错题本 / 复习队列 SQL 严格按文档 4.3 两条条件
 */
@Service
public class PracticeServiceImpl implements PracticeService {

    private static final String SESSION_KEY_PREFIX = "practice:session:";

    /**
     * 刷题会话缓存值：绑定 userId，避免会话 id 被他人复用；显式类型避免缓存实现替换后的 ClassCastException。
     */
    private record PracticeSession(Long userId, List<Long> ids) {
    }

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private UserQuestionRecordMapper recordMapper;

    @Resource
    private UserAnswerLogMapper answerLogMapper;

    @Resource
    private UserFavoriteMapper favoriteMapper;

    @Resource
    private QuestionAccessChecker checker;

    @Resource
    private QuestionServiceImpl questionService;

    @Resource
    private CacheService cache;

    @Override
    public Map<String, Object> createSession(PracticeSessionDTO dto, Long userId) {
        String source = dto.getSource() == null ? "all" : dto.getSource();
        boolean vipFilter = !checker.canReadVip(userId);
        List<Long> ids;
        if ("review".equals(source)) {
            // 复习模式：从遗忘曲线队列选题，SQL 已过滤下架/删除/无权限题目，越早到期越先复习
            ids = recordMapper.selectReviewIds(userId, vipFilter);
            if (ids == null || ids.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "当前没有到期待复习的题目");
            }
        } else if ("wrong".equals(source)) {
            // 错题重做模式：从错题本选题
            ids = recordMapper.selectWrongIds(userId, vipFilter);
            if (ids == null || ids.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "错题本是空的，没有需要重做的题目");
            }
        } else {
            // 常规刷题：权限过滤已在 SQL 完成，无 VIP 权限的用户选不到 VIP 题
            List<Long> categoryIds = questionService.resolveCategoryIds(dto.getCategoryId());
            ids = questionMapper.selectReadableIds(categoryIds, dto.getDifficulty(), vipFilter);
            if (ids == null || ids.isEmpty()) {
                throw new BizException(ResultCode.PARAM_ERROR, "该条件下暂无可刷题目");
            }
        }
        if ("random".equals(dto.getMode())) {
            Collections.shuffle(ids);
        }
        // 复习 / 错题模式由用户显式发起，默认一次做完整个队列（count 不传即不截断）；
        // 常规刷题保持默认 20 题，避免一次拉取过多。
        int count = dto.getCount() != null ? dto.getCount() : ("all".equals(source) ? 20 : Integer.MAX_VALUE);
        if (ids.size() > count) {
            ids = new ArrayList<>(ids.subList(0, count));
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        // TTL 2 小时（文档 4.3）；会话绑定用户，防止会话 id 外泄后被他人复用
        cache.set(SESSION_KEY_PREFIX + sessionId, new PracticeSession(userId, ids), Duration.ofHours(2));
        return Map.of("sessionId", sessionId, "total", ids.size());
    }

    @Override
    public QuestionPracticeVO next(String sessionId, int index, Long userId) {
        Object cached = cache.get(SESSION_KEY_PREFIX + sessionId);
        if (!(cached instanceof PracticeSession session)) {
            throw new BizException(ResultCode.PARAM_ERROR, "会话已过期，请重新开始练习");
        }
        if (!userId.equals(session.userId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权访问该刷题会话");
        }
        List<Long> ids = session.ids();
        if (index < 0 || index >= ids.size()) {
            throw new BizException(ResultCode.NOT_FOUND, "题目序号超出会话范围");
        }
        // 详情逐条走 checker（文档 4.1：会话内题目也可能在 2 小时内被下架）
        return questionService.detail(ids.get(index), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionSubmitVO submit(SubmitDTO dto, Long userId) {
        // 防普通用户对 VIP 题作答（文档 4.1 鉴权矩阵）；不通过则不落库
        Question question = checker.checkReadable(dto.getQuestionId(), userId);

        Integer isCorrect = grade(question, dto.getAnswer());

        // 状态表 upsert（原子累加，文档 4.3 / 4.6 并发安全）
        String answer = dto.getAnswer().trim();
        if (isCorrect == null) {
            recordMapper.upsertEssay(userId, question.getId(), answer);
        } else if (isCorrect == 1) {
            recordMapper.upsertCorrect(userId, question.getId(), answer);
        } else {
            recordMapper.upsertWrong(userId, question.getId(), answer);
        }

        // 流水表追加（正确率统计口径：正确条数 / 总作答条数）
        UserAnswerLog log = new UserAnswerLog();
        log.setUserId(userId);
        log.setQuestionId(question.getId());
        log.setUserAnswer(answer);
        log.setIsCorrect(isCorrect);
        answerLogMapper.insert(log);

        // 组装提交后 VO（此阶段才下发答案与解析，文档 4.2）
        QuestionSubmitVO vo = new QuestionSubmitVO();
        vo.setQuestionId(question.getId());
        vo.setIsCorrect(isCorrect);
        vo.setAnswer(question.getAnswer());
        vo.setAnswerText(question.getAnswerText());
        vo.setAnalysis(question.getAnalysis());
        UserQuestionRecord record = recordMapper.selectOne(new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getUserId, userId)
                .eq(UserQuestionRecord::getQuestionId, question.getId()));
        vo.setMastered(record == null ? 0 : record.getMastered());
        vo.setNextReviewTime(record == null ? null : record.getNextReviewTime());
        return vo;
    }

    /**
     * 判分规则（文档 4.3）：
     * 单选答案完全相等；多选「按字符排序拼接」归一化后比较（BD 与 DB 等价）；
     * 判断题按单选项（标准答案存 A/B）；简答不判分返回 NULL。
     */
    private Integer grade(Question question, String userAnswer) {
        Integer type = question.getType();
        if (type == 4) {
            return null;
        }
        String standard = question.getAnswer() == null ? "" : question.getAnswer().trim();
        String answer = userAnswer == null ? "" : userAnswer.trim();
        if (type == 2) {
            return normalizeMulti(standard).equals(normalizeMulti(answer)) ? 1 : 0;
        }
        // 单选(1)与判断(3)统一按字符串相等
        return standard.equalsIgnoreCase(answer) ? 1 : 0;
    }

    /** 多选归一化：剔除非字母、转大写、按字符排序后拼接 */
    private String normalizeMulti(String s) {
        return s.replaceAll("[^A-Za-z]", "").toUpperCase().chars()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public WrongBookPageVO wrongBook(long pageNum, long pageSize, Long userId) {
        Page<QuestionWrongVO> page = new Page<>(PageUtil.num(pageNum), PageUtil.size(pageSize));
        IPage<QuestionWrongVO> result = recordMapper.selectWrongBook(page, userId);
        maskUnauthorized(result.getRecords(), userId);
        // 复用选题 SQL 统计全队列可作答数：与「开始复习」实际能取到的题数严格同口径
        boolean vipFilter = !checker.canReadVip(userId);
        long answerable = recordMapper.selectWrongIds(userId, vipFilter).size();
        return buildWrongBookPage(result, answerable);
    }

    @Override
    public WrongBookPageVO reviewList(long pageNum, long pageSize, Long userId) {
        Page<QuestionWrongVO> page = new Page<>(PageUtil.num(pageNum), PageUtil.size(pageSize));
        IPage<QuestionWrongVO> result = recordMapper.selectReviewQueue(page, userId);
        maskUnauthorized(result.getRecords(), userId);
        // 同上：与复习会话的选题 SQL 完全一致，避免前端按分页估算
        boolean vipFilter = !checker.canReadVip(userId);
        long answerable = recordMapper.selectReviewIds(userId, vipFilter).size();
        return buildWrongBookPage(result, answerable);
    }

    private WrongBookPageVO buildWrongBookPage(IPage<QuestionWrongVO> page, long answerableCount) {
        WrongBookPageVO vo = new WrongBookPageVO();
        vo.setList(page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setAnswerableCount(answerableCount);
        return vo;
    }

    /**
     * 越权脱敏（文档 4.1）：VIP 过期后只返回题目 id 与分类名，题干置空，不暴露任何题目内容。
     * 题目被下架 / 删除时同样脱敏而非 404，避免整页失败。
     */
    private void maskUnauthorized(List<QuestionWrongVO> records, Long userId) {
        if (records == null) {
            return;
        }
        for (QuestionWrongVO vo : records) {
            try {
                checker.checkReadable(vo.getQuestionId(), userId);
            } catch (BizException e) {
                vo.setTitle(null);
            }
        }
    }

    @Override
    public StatsVO stats(Long userId) {
        StatsVO vo = new StatsVO();
        long total = answerLogMapper.countTotal(userId);
        long correct = answerLogMapper.countCorrect(userId);
        vo.setTotalCount(total);
        vo.setCorrectCount(correct);
        vo.setAccuracy(total == 0 ? 0.0 : Math.round(correct * 1000.0 / total) / 10.0);
        vo.setCategoryStats(answerLogMapper.selectCategoryStats(userId));
        vo.setContinuousCheckInDays(calcContinuousDays(userId));
        // 错题本 / 复习队列条数（与文档 4.3 两个列表查询条件同口径，供首页提醒）
        vo.setWrongBookCount(recordMapper.selectCount(new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getUserId, userId)
                .eq(UserQuestionRecord::getMastered, 0)
                .eq(UserQuestionRecord::getIsCorrect, 0)));
        LocalDateTime now = LocalDateTime.now();
        vo.setReviewCount(recordMapper.selectCount(new LambdaQueryWrapper<UserQuestionRecord>()
                .eq(UserQuestionRecord::getUserId, userId)
                .eq(UserQuestionRecord::getMastered, 0)
                .isNotNull(UserQuestionRecord::getNextReviewTime)
                .le(UserQuestionRecord::getNextReviewTime, now)));
        return vo;
    }

    /** 连续打卡天数：按流水表 distinct 日期从今天（或昨天）向前连续计数 */
    private int calcContinuousDays(Long userId) {
        List<LocalDate> dates = answerLogMapper.selectRecentDates(userId);
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        // distinct 已倒序，用 LinkedHashSet 去重保险
        Set<LocalDate> distinct = new LinkedHashSet<>(dates);
        List<LocalDate> ordered = new ArrayList<>(distinct);
        int streak = 0;
        LocalDate expected = LocalDate.now();
        for (LocalDate d : ordered) {
            if (d.equals(expected)) {
                streak++;
                expected = d.minusDays(1);
            } else if (streak == 0 && d.equals(expected.minusDays(1))) {
                // 今天还没答题，从昨天起算
                streak++;
                expected = d.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }
}
