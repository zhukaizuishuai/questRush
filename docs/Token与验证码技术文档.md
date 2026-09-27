# Token 与注册登录验证码技术文档

> 目标读者：后续学习/维护本项目的开发者。本文梳理 QuestRush 的登录态（Token）体系与验证码体系（图形验证码 / 邮箱验证码）的完整实现，以及前后端交互细节。

## 1. 技术选型一览

| 能力 | 技术方案 | 说明 |
| --- | --- | --- |
| 登录态 / Token | **Sa-Token**（`cn.dev33:sa-token-spring-boot3-starter`） | UUID 风格 Token，服务端内存存会话 |
| 密码加密 | Spring Security 的 `BCryptPasswordEncoder` | 只用其加密能力，未引入 Spring Security 过滤器链 |
| 图形验证码 | **easy-captcha**（`com.wf.captcha:SpecCaptcha`） | 4 位字母数字，Base64 图片直出 |
| 邮箱验证码 | Spring Boot `spring-boot-starter-mail`（SMTP 465 SSL） | 6 位数字，未配置 SMTP 时走 mock（打日志） |
| 验证码/限流存储 | 自研 `CacheService`（ConcurrentHashMap + TTL 懒清理） | 接口与 Redis 命令一一对应（set/get/incr/setnx），生产可无痛换 Redis |

关键配置文件：`backend/src/main/resources/application.yml`（`sa-token` 段，application.yml:45-52）：

```yaml
sa-token:
  token-name: satoken      # 前端请求头 / Cookie 名
  timeout: 2592000         # Token 有效期 30 天
  active-timeout: -1       # 不启用「无操作过期」
  is-concurrent: true      # 允许多端同时登录
  is-share: true           # 多端登录共享同一个 Token
  token-style: uuid
```

---

## 2. Token 体系（登录态）

### 2.1 后端：签发与校验

**签发**（`AuthServiceImpl.login`，AuthServiceImpl.java:174-181）：

```java
userMapper.resetLoginFail(user.getId());   // 重置失败计数
StpUtil.login(user.getId());               // Sa-Token 创建会话，生成 Token
vo.setToken(StpUtil.getTokenValue());      // Token 放进 LoginVO 返回给前端
vo.setUser(toUserVO(user));                // 同时返回用户信息
```

登录成功后 Sa-Token 会把 Token 写入响应 Cookie（名 `satoken`）；同时业务代码把 Token 显式返回给前端，由前端存 localStorage 并在后续请求头携带（双通道，见 2.3）。

**路由拦截**（`SaTokenConfig`，config/SaTokenConfig.java:33-42）：

```java
SaRouter.match("/api/**").notMatch("/api/auth/**")   // 认证接口放行
        .check(r -> StpUtil.checkLogin());           // 其余接口必须登录，未登录返回 401
SaRouter.match("/api/admin/**")
        .check(r -> StpUtil.checkRole("admin"));     // 管理端还要求 admin 角色，否则 403
```

**账号状态实时校验**（SaTokenConfig.java:45-63，易漏的坑）：Sa-Token 不会因为数据库里 `status` 改成 0 而自动让已签发的 Token 失效。所以项目加了第二个拦截器：每次已登录请求都查库，发现用户不存在 / `status=0`（禁用）/ `deleted=1`（已删除）就 `StpUtil.logout()` 并抛 401。这样管理员封号/删号能立即踢下线。

Sa-Token 从哪读 Token？默认先读请求头 `satoken`，再读同名 Cookie——两个拦截器都依赖这一点。

### 2.2 后端：登出与踢人

- 用户主动登出：`POST /api/auth/logout` → `StpUtil.logout()`（AuthServiceImpl.java:190-193）。
- 管理员封禁/删除用户：`AdminUserServiceImpl` 里调 `StpUtil.logout(userId)` 强制踢下线。

### 2.3 前端：存储、注入、失效处理

核心都在 `frontend/src/api/request.ts`：

1. **存储**：登录成功后 Pinia store（`stores/user.ts` 的 `setAuth`）把 Token 写进 `localStorage`（key = `questrush_token`），用户信息写 `questrush_user`。刷新页面后 store 从 localStorage 恢复。
2. **注入请求头**（request.ts:18-24）：

   ```ts
   instance.interceptors.request.use((config) => {
     const token = localStorage.getItem(TOKEN_KEY)
     if (token) config.headers['satoken'] = token   // 必须与后端 sa-token.token-name 一致
     return config
   })
   ```

   为什么必须带请求头：同源部署时登录写下的 Cookie 能兜底，但跨域部署或禁用 Cookie 时 Cookie 通道就断了，请求头是唯一可靠通道。

3. **401 统一处理**（request.ts:26-48）：响应拦截器收到 `code === 401`（或 HTTP 401）时执行 `clearAuthAndGoLogin()`：
   - 清 localStorage 的 token 和 user；
   - **同步清空 Pinia store**（只清 localStorage 的话 store 里还是旧快照，`isLoggedIn` 仍为 true，路由守卫会把你弹回首页，形成 401 死循环）;
   - 动态 `import('@/router')` 跳转 `/login` 并带 `redirect` 参数（动态引入是为了避免 request ↔ router 循环依赖）。

4. **登录态探测**：路由守卫调 `GET /api/auth/me`（AuthController.java:72-75），已登录返回 userId，否则返回 `data: null`——这是 `/api/auth/**` 白名单里的接口，所以不要求登录。

---

## 3. 图形验证码

### 3.1 后端实现（AuthServiceImpl）

**生成** `GET /api/auth/captcha`（AuthServiceImpl.java:53-68）：

```
1. IP 限流：同一 IP 每分钟最多 20 次（key: captcha:ip:{ip}，CacheService.increment）
2. SpecCaptcha(130, 48, 4) 生成 4 位字母数字，code 统一转小写
3. 生成 32 位 captchaId（UUID 去横线）
4. 存储：key = captcha:graph:{captchaId}，value = code，TTL 5 分钟
5. 返回 CaptchaVO { captchaId, image: Base64 PNG }
```

**校验** `verifyGraphCaptcha`（AuthServiceImpl.java:70-82）：

```
1. 先删 key，再比对 —— 无论成功失败都是一次性使用（防重放）
2. 忽略大小写比对；key 不存在（过期/已用/伪造）或比对失败 → 抛「验证码错误或已过期」
```

一次性删除这一点很关键：验证码图虽然还在页面上，但后端已删，重放请求必然失败。

### 3.2 前端交互

以登录页 `frontend/src/views/login/LoginView.vue` 为例：

1. `onMounted` 调 `getCaptcha()` → `GET /api/auth/captcha`，拿到 `{ captchaId, image }`；
2. `captchaImageSrc()`（utils/format.ts:54-56）把 Base64 拼成 `data:image/png;base64,...` 直接 `<img :src>` 展示，无静态文件；
3. 点击图片或提交失败后调 `refreshCaptcha()` 重新拉取（提交失败必刷新，避免拿已失效的图反复提交）；
4. 提交时带上 `captchaId + captchaCode`。

注册页同理，且**注册强制验证码**（不填过不去）；找回密码发送邮箱验证码时也强制图形验证码。

---

## 4. 登录防爆破策略（验证码 + 限流联动）

登录接口是验证码策略最复杂的地方（产品调整 2026-09-27：**登录页始终显示验证码**）。规则（AuthServiceImpl.java:110-182）：

### 4.1 双维度限流

| 维度 | 规则 | 缓存 key |
| --- | --- | --- |
| 用户名 | 15 分钟内失败 ≥ 3 次 → 强制验证码；≥ 5 次 → 锁 15 分钟 | `login:user:fail:{username}` / `login:user:lock:{username}` |
| IP | 15 分钟内失败 ≥ 20 次 → 锁 IP 30 分钟 | `login:ip:fail:{ip}` / `login:ip:lock:{ip}` |

### 4.2 验证码「带了就校验」的渐进语义

```
captchaRequired = 用户名失败计数 >= 3
captchaProvided = 请求里带了 captchaId + captchaCode
  ├─ captchaRequired && !captchaProvided → 抛「用户名或密码错误」+ data.requireCaptcha=true
  └─ captchaProvided → 先校验验证码，再校验密码
```

因为登录页始终显示验证码，所以正常用户每次都带；`requireCaptcha` 标志是为「登录页改造前发出的请求」和异常路径兜底。

### 4.3 防账号枚举的细节（安全设计重点）

- **按用户名计数而不是按数据库 `login_fail_count`**：DB 计数只有真实存在的账号才有行，不存在的用户名永远不会触发锁定，攻击者可据此枚举已注册账号。改成缓存按用户名计数后，不存在/存在的账号行为完全一致。
- **账号不存在与密码错误返回完全相同的文案**「用户名或密码错误」（AuthServiceImpl.java:148-166）。
- **DB 层锁定（`login_lock_time`）与缓存锁定的文案也保持一致**，避免差异暴露账号是否存在。
- 唯一例外：密码已校验通过后的「账号已被禁用」提示——此时无枚举风险，且该信息对正常用户是必要的。

### 4.4 requireCaptcha 如何传回前端

后端用 `BizException(code, message, data)` 携带扩展数据（exception/BizException.java），全局异常处理器把它塞进 `Result.data`。前端响应拦截器（request.ts:100-103）在 `code !== 0` 时把**完整 Result 挂到 Error 对象**（`err.result = res`），调用方可以从 `e.result.data.requireCaptcha` 读取——这是「错误响应也要携带业务数据」的通用模式。

---

## 5. 邮箱验证码（找回密码）

### 5.1 发送 `POST /api/auth/email-code`（AuthServiceImpl.java:196-213）

```
1. 强制图形验证码（captchaId + captchaCode，先 verifyGraphCaptcha）
2. 同一邮箱 60 秒冷却（setIfAbsent，SETNX 语义）
3. 同一 IP 每日最多 10 次（increment，TTL 1 天）
4. 生成 6 位数字码，存 captcha:mail:{email}，TTL 10 分钟
5. MailService.sendCaptcha 发邮件；未配置 SMTP host 时走 mock（验证码打印到日志，本地联调友好）
```

### 5.2 重置 `POST /api/auth/reset-password`（AuthServiceImpl.java:215-232）

```
1. 从缓存取 captcha:mail:{email} 比对（注意：此处没有「先删后比」，校验通过才删除）
2. 通过后按 email 查用户，重置密码为 BCrypt(newPassword)
```

---

## 6. 前后端交互时序

### 6.1 登录（含验证码）

```
浏览器                          后端
  │ GET /api/auth/captcha        │
  │─────────────────────────────▶│ 生成 captchaId + 图，存缓存(TTL 5min)
  │ ◀──── {captchaId, image} ────│
  │ 展示图片，用户填表单           │
  │ POST /api/auth/login         │
  │  {username,password,         │
  │   captchaId,captchaCode}     │
  │─────────────────────────────▶│ ① IP 锁? 用户名锁? → 拒
  │                              │ ② 失败≥3 且未带验证码 → 拒(requireCaptcha)
  │                              │ ③ 带了验证码 → 先删缓存再比对
  │                              │ ④ BCrypt 比对密码
  │                              │ ⑤ 失败: 计数+1, 可能触发锁定 → 拒
  │                              │ ⑥ 成功: 重置计数, StpUtil.login
  │ ◀── {code:0, data:{token,    │   （同时 Set-Cookie: satoken=...）
  │       user:{...}}} ──────────│
  │ localStorage 存 token        │
  │ 跳转 redirect 页             │
```

### 6.2 登录后的普通请求

```
浏览器                          后端
  │ GET /api/xxx                 │
  │  headers: satoken: {token}   │
  │─────────────────────────────▶│ ① SaInterceptor: checkLogin
  │                              │ ② admin 接口: checkRole("admin")
  │                              │ ③ 状态拦截器: 查库校验 status/deleted
  │                              │ ④ 业务处理
  │ ◀──── {code:0, data:...} ────│
  │（code:401 → 清 token → 跳 /login?redirect=当前页）
```

### 6.3 注册

```
  │ GET /api/auth/captcha ──────▶│ （同上，拿 captchaId + 图）
  │ POST /api/auth/register      │
  │  {username,password,nickname,│
  │   email,captchaId,captchaCode}│
  │─────────────────────────────▶│ ① 强制校验图形验证码（一次性）
  │                              │ ② username/email 查重
  │                              │ ③ BCrypt 加密入库，role=user
```

---

## 7. 关键文件索引

| 模块 | 文件 |
| --- | --- |
| 认证接口 | `backend/src/main/java/com/learn/controller/AuthController.java` |
| 认证逻辑（验证码/限流/登录全部规则） | `backend/src/main/java/com/learn/service/impl/AuthServiceImpl.java` |
| Sa-Token 拦截与账号状态校验 | `backend/src/main/java/com/learn/config/SaTokenConfig.java` |
| 缓存抽象（验证码/限流存储） | `backend/src/main/java/com/learn/util/CacheService.java`（实现 `InMemoryCacheService`） |
| 业务异常（含 requireCaptcha 扩展数据） | `backend/src/main/java/com/learn/exception/BizException.java` |
| sa-token 配置 | `backend/src/main/resources/application.yml:45-52` |
| 邮件发送（含 mock 兜底） | `backend/src/main/java/com/learn/service/MailService.java` |
| axios 实例 / token 注入 / 401 处理 | `frontend/src/api/request.ts` |
| 认证 API 封装 | `frontend/src/api/auth.ts` |
| 用户 store（token 持久化） | `frontend/src/stores/user.ts` |
| 登录页（验证码展示/刷新） | `frontend/src/views/login/LoginView.vue` |
| 注册页 | `frontend/src/views/register/RegisterView.vue` |
| Base64 转 img src | `frontend/src/utils/format.ts:54-56` |

## 8. 学习时值得注意的设计点

1. **验证码一次性消费**：先删后比对，天然防重放。
2. **无 Redis 依赖**：`CacheService` 抽象出 Redis 语义（set/get/incr/setnx），单机内存实现，换 Redis 时业务零改动——但注意单机内存实现的局限（重启丢会话/验证码，多实例不共享），生产多实例部署必须换 Redis。
3. **防账号枚举**：登录失败文案统一 + 按用户名计数（而非按 DB 行计数），这是安全设计里容易被忽略的点。
4. **Sa-Token 不感知数据库状态变化**：需要额外拦截器查库校验 status/deleted 才能做到「封号即时生效」。
5. **前后端契约细节**：请求头名必须与 `sa-token.token-name` 一致；错误响应也可携带业务数据（`Result.data`），前端通过 `e.result` 取——`requireCaptcha` 就是这样流转的。
6. **双通道 Token**（请求头 + Cookie）：同源下靠 Cookie 兜底，跨域/无 Cookie 环境靠请求头，前端代码里对这一点有明确注释。
