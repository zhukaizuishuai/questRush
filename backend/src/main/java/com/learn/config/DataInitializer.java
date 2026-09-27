package com.learn.config;

import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.entity.Question;
import com.learn.entity.QuestionCategory;
import com.learn.entity.QuestionOption;
import com.learn.entity.User;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.QuestionOptionMapper;
import com.learn.mapper.UserMapper;
import com.learn.util.Md5Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 种子数据初始化（幂等）：
 * - user 表无 admin 用户时创建 admin / admin123（BCrypt 加密），初始密码打印到启动日志
 * - question_category 表为空时插入种子分类与题目（表为空才插入，重复启动不产生脏数据）
 *
 * 说明：不使用 spring.sql.init 的 data.sql，因为密码必须运行时 BCrypt 加密（文档 3.1）。
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionOptionMapper optionMapper;

    @Override
    public void run(String... args) {
        initAdmin();
        initCategoriesAndQuestions();
    }

    private void initAdmin() {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "admin"));
        if (count > 0) {
            return;
        }
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole("admin");
        admin.setStatus(1);
        admin.setLoginFailCount(0);
        userMapper.insert(admin);
        log.warn("==========================================================");
        log.warn("已创建初始管理员账号：admin / admin123（请登录后尽快修改密码）");
        log.warn("==========================================================");
    }

    private void initCategoriesAndQuestions() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        // 一级分类
        Long java = addCategory("Java", 0L, 100);
        Long python = addCategory("Python", 0L, 90);
        Long frontend = addCategory("前端", 0L, 80);
        Long database = addCategory("数据库", 0L, 70);
        Long network = addCategory("计算机网络", 0L, 60);

        // 二级分类（叶子，题目挂载点）
        Long javaBasic = addCategory("Java基础", java, 10);
        Long javaConcurrent = addCategory("Java并发", java, 9);
        Long pythonBasic = addCategory("Python基础", python, 10);
        Long js = addCategory("JavaScript", frontend, 10);
        Long mysql = addCategory("MySQL", database, 10);
        Long tcp = addCategory("TCP/IP", network, 10);

        seedJavaBasic(javaBasic);
        seedJavaConcurrent(javaConcurrent);
        seedPythonBasic(pythonBasic);
        seedJavaScript(js);
        seedMysql(mysql);
        seedTcpIp(tcp);

        log.info("seed categories & questions initialized");
    }

    private Long addCategory(String name, Long parentId, int sort) {
        QuestionCategory c = new QuestionCategory();
        c.setName(name);
        c.setParentId(parentId);
        c.setSort(sort);
        categoryMapper.insert(c);
        return c.getId();
    }

    /** 通用建题：type 1 单选 2 多选 3 判断 4 简答；options 为 {code, content} 数组 */
    private void addQuestion(Long categoryId, int type, int difficulty, String title, String answer,
                             String answerText, String analysis, int isVip, String[][] options) {
        Question q = new Question();
        q.setCategoryId(categoryId);
        q.setType(type);
        q.setDifficulty(difficulty);
        q.setTitle(title);
        q.setAnswer(type == 4 ? "" : answer);
        q.setAnswerText(answerText);
        q.setAnalysis(analysis);
        q.setIsVip(isVip);
        q.setStatus(1);
        q.setTitleMd5(Md5Util.titleMd5(title));
        questionMapper.insert(q);
        if (options != null) {
            int sort = 0;
            for (String[] o : options) {
                QuestionOption po = new QuestionOption();
                po.setQuestionId(q.getId());
                po.setOptionCode(o[0]);
                po.setOptionContent(o[1]);
                po.setSort(sort++);
                optionMapper.insert(po);
            }
        }
    }

    // ---------- 各分类种子题目（每个子分类 3-4 题，覆盖单选/多选/判断/简答；含 2 道 VIP 题） ----------

    private void seedJavaBasic(Long cat) {
        addQuestion(cat, 1, 1, "Java 中 `String s = new String(\"abc\")` 最多会创建几个对象？", "B", null,
                "字面量 \"abc\" 若常量池不存在会先在常量池创建一个对象，new String 又在堆上创建一个，故最多 2 个。",
                0, new String[][]{{"A", "1 个"}, {"B", "2 个"}, {"C", "3 个"}, {"D", "0 个"}});
        addQuestion(cat, 2, 2, "下列哪些是 Java 中的引用类型？", "BCD", null,
                "String、数组、接口类型都是引用类型；int 是基本类型。",
                0, new String[][]{{"A", "int"}, {"B", "String"}, {"C", "数组"}, {"D", "接口类型"}});
        addQuestion(cat, 3, 1, "Java 中接口中的默认方法（default 方法）必须有方法体。", "A", null,
                "default 方法自带实现，实现类可选择重写或不重写。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 2, "请简述 JVM 的内存区域划分。", null,
                "线程私有：程序计数器、虚拟机栈、本地方法栈；线程共享：堆、方法区（元空间）。",
                "重点说明堆与方法区的作用，以及栈帧内局部变量表、操作数栈的结构。", 0, null);
    }

    private void seedJavaConcurrent(Long cat) {
        addQuestion(cat, 1, 2, "`volatile` 关键字能保证以下哪些特性？（VIP 专属题）", "AC", null,
                "volatile 保证可见性与禁止指令重排，但不保证原子性（i++ 仍不安全）。",
                1, new String[][]{{"A", "可见性"}, {"B", "原子性"}, {"C", "禁止指令重排"}, {"D", "互斥性"}});
        addQuestion(cat, 3, 2, "`synchronized` 修饰实例方法时，锁对象是当前实例 this。", "A", null,
                "修饰静态方法时锁的是类对象 Class 实例。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 3, "简述线程池的核心参数与任务提交后的执行流程。（VIP 专属题）", null,
                "核心参数：corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory、handler。" +
                        "流程：核心线程 → 队列 → 非核心线程 → 拒绝策略。",
                "说明为什么先入队再扩容，以及四种拒绝策略的适用场景。", 1, null);
    }

    private void seedPythonBasic(Long cat) {
        addQuestion(cat, 1, 1, "Python 中下列哪个是不可变类型？", "C", null,
                "tuple 不可变；list、dict、set 均可变。",
                0, new String[][]{{"A", "list"}, {"B", "dict"}, {"C", "tuple"}, {"D", "set"}});
        addQuestion(cat, 2, 2, "下列哪些方法可以创建字典？", "AB", null,
                "dict() 构造与字面量 {} 都可以；dict.fromkeys 也行但选项未列；[] 创建列表。",
                0, new String[][]{{"A", "dict(a=1)"}, {"B", "{\"a\": 1}"}, {"C", "[\"a\"]"}, {"D", "set()"}});
        addQuestion(cat, 3, 1, "Python 中 `is` 比较的是两个对象的值是否相等。", "B", null,
                "is 比较身份（是否同一对象），值比较用 ==。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 2, "简述 Python GIL 的含义与影响。", null,
                "GIL（全局解释器锁）保证同一时刻仅一个线程执行 Python 字节码；CPU 密集任务多线程无法并行，" +
                        "应使用多进程或 C 扩展；IO 密集任务多线程仍然有效。",
                "可对比 multiprocessing 与 threading 的适用场景。", 0, null);
    }

    private void seedJavaScript(Long cat) {
        addQuestion(cat, 1, 1, "`typeof null` 的结果是？", "B", null,
                "历史遗留 bug：typeof null 返回 \"object\"。",
                0, new String[][]{{"A", "\"null\""}, {"B", "\"object\""}, {"C", "\"undefined\""}, {"D", "报错"}});
        addQuestion(cat, 3, 1, "JavaScript 中 `let` 声明的变量存在变量提升，但存在暂时性死区。", "B", null,
                "let 声明会被提升但不会初始化，访问即 ReferenceError，因此说 let 没有变量提升更准确；" +
                        "严格来说存在「提升但未初始化」的行为，此题按「存在变量提升」表述判错。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 2, "简述 JS 事件循环（Event Loop）中宏任务与微任务的执行顺序。", null,
                "每执行完一个宏任务，清空全部微任务队列，再取下一个宏任务。常见宏任务：script、setTimeout、setInterval；" +
                        "微任务：Promise.then、queueMicrotask、MutationObserver。",
                "可举 setTimeout 与 Promise 混合输出的经典例子。", 0, null);
    }

    private void seedMysql(Long cat) {
        addQuestion(cat, 1, 2, "InnoDB 中以下哪个隔离级别可以防止幻读？", "C", null,
                "SERIALIZABLE 通过加锁读防止幻读；REPEATABLE READ 默认靠 MVCC 快照读，当前读需 next-key lock。" +
                        "以「可以防止」为准，串行化最直接。",
                0, new String[][]{{"A", "READ UNCOMMITTED"}, {"B", "READ COMMITTED"}, {"C", "SERIALIZABLE"}, {"D", "以上都不行"}});
        addQuestion(cat, 2, 2, "以下哪些操作会走索引（假设 idx_col 为单列 B+ 树索引）？", "AB", null,
                "对索引列做函数操作或前导模糊匹配会导致索引失效。",
                0, new String[][]{{"A", "WHERE col = 1"}, {"B", "WHERE col LIKE 'abc%'"}, {"C", "WHERE UPPER(col) = 'A'"}, {"D", "WHERE col LIKE '%abc'"}});
        addQuestion(cat, 3, 1, "MySQL 的逻辑删除设计下，唯一键包含 deleted 列可以避免软删数据撞唯一键。", "B", null,
                "唯一键包含 deleted 反而会让 (a,0) 与 (a,1) 并存，约束失效，且第二次软删会撞键（文档 3.0）。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 3, "简述 MySQL 慢查询的排查思路。", null,
                "开启 slow_query_log → 用 explain 分析执行计划（type、key、rows、Extra）→ " +
                        "确认索引命中与回表情况 → 优化 SQL 或加索引 → 验证。",
                "重点解释 explain 输出中 type 的等级与 Extra 的常见标记。", 0, null);
    }

    private void seedTcpIp(Long cat) {
        addQuestion(cat, 1, 1, "TCP 建立连接需要几次握手？", "C", null,
                "三次握手：SYN → SYN+ACK → ACK。",
                0, new String[][]{{"A", "1 次"}, {"B", "2 次"}, {"C", "3 次"}, {"D", "4 次"}});
        addQuestion(cat, 3, 2, "UDP 保证数据包按序到达。", "B", null,
                "UDP 不保证顺序、不保证可靠交付，适合实时音视频等场景。",
                0, new String[][]{{"A", "正确"}, {"B", "错误"}});
        addQuestion(cat, 4, 2, "简述浏览器输入 URL 到页面展示的完整过程。", null,
                "DNS 解析 → TCP 三次握手（HTTPS 再加 TLS 握手）→ 发送 HTTP 请求 → 服务端处理返回响应 → " +
                        "浏览器解析 HTML 构建 DOM/CSSOM → 渲染树绘制。",
                "可按网络层与渲染层两条线展开。", 0, null);
    }
}
