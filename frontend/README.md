# QuestRush 前端

习题冲锋、闯关刷题的前端项目（Vue 3 + TypeScript + Vite + Naive UI）。

## 技术栈

- Vue 3.4 + TypeScript + Vite 5
- Vue Router 4（history 模式 + 路由守卫：`meta.requiresAuth` / `meta.requiresAdmin`）
- Pinia（用户状态：token、userInfo、isVip / isAdmin 计算属性）
- Naive UI（浅色主题，中文语言包）
- Axios（统一拦截器：Token 注入 `Authorization` 头，401 清状态跳登录，40301 弹会员引导，422 参数校验提示）
- markdown-it + highlight.js（github 主题）+ DOMPurify（题干 / 解析渲染与 XSS 防护）

## 启动

```bash
npm install
npm run dev        # 开发，/api 代理到 http://localhost:8080
npm run build      # 生产构建
npm run typecheck  # vue-tsc 类型检查
```

## 工程结构

```
src
├── api/          request.ts（Axios 封装）、auth/user/category/question/practice/favorite/note/like/vip/admin
├── router/       路由表 + 全局前置守卫
├── stores/       user.ts（Pinia 用户状态）
├── types/        与后端 VO 一一对应的 TS 类型
├── utils/        format.ts（题型/难度/时间/多选答案归一化）
├── components/   MarkdownRender、QuestionCard、OptionGroup、LikeButton、VipBadge、MainLayout
├── views/
│   ├── login / register        全屏居中卡片，登录失败≥3次动态显示验证码，注册强制验证码
│   ├── home                    分类入口、复习提醒、统计卡片
│   ├── question                分类浏览 / 题目列表搜索 / 题目详情（不含答案）
│   ├── practice                刷题（会话模式）、错题本、复习队列、统计
│   ├── user                    个人中心、VIP 套餐、收藏、笔记、订单
│   └── admin                   左侧菜单布局：仪表盘 / 用户 / 分类 / 题库管理（Excel 导入导出）
└── App.vue / main.ts
```

## 关键约定

- **答案安全**：答题前置接口（列表 / 详情 / 刷题）不含答案；提交后由 `/api/practice/submit` 返回判分结果、正确答案与解析。
- **VIP 引导**：响应拦截器收到 `code=40301` 时弹出「开通会员」引导，点击跳 `/vip`。
- **多选题**：作答归一化为排序后拼接的字母串（`BD` 与 `DB` 等价）。
- **验证码**：`GET /api/auth/captcha` 返回 `captchaId + imageBase64`，点击图片刷新，提交失败后自动刷新。
- **点赞**：乐观更新 + 请求期间置灰防连点，以服务端返回计数为准。
