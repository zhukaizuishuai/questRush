# QuestRush Docker 化部署指导手册

> 前置条件：已阅读《部署指导手册.md》（裸机版），服务器上已装 Docker 引擎。
> 本文档已脱敏，不含真实 IP、密码等敏感信息，可提交到 Git。
> 目标：把「裸机三件套（Nginx + jar + MySQL）」改造成「三个容器统一编排」。

---

## 第一章 Docker 核心概念速成

用你已经熟悉的前端概念来类比：

| Docker 概念 | 是什么 | 前端类比 |
| --- | --- | --- |
| **镜像 (image)** | 只读模板，包含运行环境和文件系统 | `node_modules` 里那份打包好的依赖 + 运行时 |
| **容器 (container)** | 镜像跑起来的运行实例 | 一个正在跑的 `node` 进程，但完全隔离 |
| **Dockerfile** | 描述「怎么把我的应用变成镜像」的脚本 | 打包配置（类似 `vite.config.ts` 定义怎么 build） |
| **docker compose** | 用一个 yaml 同时编排多个容器 | `package.json` 的 scripts + 一键 `npm run dev` 起全套 |
| **数据卷 (volume)** | 容器删了数据还在的"外挂硬盘" | localStorage / 数据库文件落在容器外 |
| **端口映射** | 把容器端口映射到宿主机 | Vite 的 `server.proxy` 反向思路：外部 → 容器 |

名称记法：这一章的词**都不是缩写，就是英文原词**——`docker` 本义"码头搬运工"（把应用连同环境打包搬着走），`image` 图像/镜像、`container` 容器、`volume` 体积/卷、`compose` 组合/编排、`network` 网络。`Dockerfile` = Docker + file（构建说明书）。

**最重要的一条心智模型**：容器之间互相隔离，`localhost` 各是各的。容器 A 想访问容器 B，不能用 `localhost`，要用 **compose 里的服务名**（比如 `http://backend:8080`）——这是裸机迁移到 Docker 时最容易翻车的点。

## 第二章 Docker 常用命令速查

> Linux 基础命令（`ssh` `scp` `ls` `cp` `tar` `ps` `ss` `kill` `systemctl` `grep` 等）的英文全称已在《部署指导手册.md》第一章逐条标注，本文档只补 Docker 相关的部分。

### 2.1 镜像与容器基础

```bash
docker ps                      # ps = Process Status（进程状态）：看正在运行的容器（加 -a = all，连停止的也看）
docker images                  # images = 镜像复数：本地有哪些镜像
docker pull nginx:1.27-alpine  # pull = 拉取：下载镜像（tag 决定版本，alpine 是最小的精简版发行版）
docker run -d --name xx nginx  # run = 运行：从镜像启动容器（-d = detach 脱离终端跑后台，--name 指定容器名）
docker stop xx / start xx      # stop 停止 / start 启动已有容器
docker rm xx                   # rm = ReMove（移除）：删除容器（先 stop）
docker rmi 镜像名              # rmi = Remove Image（删镜像），比 rm 多个 i
docker logs -f xx              # logs = 日志：跟踪容器日志（-f = follow 跟随，等价 tail -f）
docker exec -it xx bash        # exec = EXECute（执行）：进容器内部开个终端（像 ssh 进小电脑）
                               # -i = interactive 交互模式，-t = tty 分配伪终端，两个常合写成 -it
docker inspect xx              # inspect = 检视：查看容器全部细节（IP、挂载、环境变量）
```

### 2.2 docker compose（本项目主用）

```bash
docker compose up -d           # up = 起（与 down 相对）：按 yaml 一键启动全套（-d = detach 后台）
docker compose ps              # 看这套服务的状态
docker compose logs -f backend # 只跟某个服务的日志
docker compose restart backend # 重启某个服务
docker compose down            # down = 拆掉：全部停止并删除容器（数据卷默认保留）
docker compose up -d --force-recreate backend   # recreate = 重建：配置改了想让容器按新配置重来
```

对比裸机：裸机更新要「kill 旧进程 → nohup 拉新进程」手动两步；Docker 是「换文件 → `docker compose restart`」一步，且服务器重启后所有容器自动恢复（`restart: always`），不再需要 nohup（NO Hang-UP，不挂断）。

### 2.3 数据卷与拷贝

```bash
docker volume ls                          # volume = 卷；ls = LiSt 列出数据卷
docker volume inspect compose名_mysql-data # 查看卷实际存在宿主机哪里
docker cp 容器名:/app/a.log ./             # cp = CoPy：容器和宿主机之间拷文件
                                          # 写法同裸机 cp，只是来源写成 "容器名:容器内路径"
```

---

## 第三章 目标架构

```text
浏览器
   │  http://<服务器IP>
   ▼
┌─ Docker 网络 (questrush) ────────────────────┐
│                                              │
│  nginx 容器 :80 ◄── 端口映射 ── 宿主机 80     │
│    ├── /        → 挂载的前端 dist 静态文件    │
│    └── /api/*   → http://backend:8080        │
│                      │                       │
│                backend 容器 (java 17 + jar)  │
│                      │                       │
│                mysql 容器 :3306（不映射公网）  │
│                      └── mysql-data 数据卷    │
└──────────────────────────────────────────────┘
```

对比裸机版的变化：

| 项 | 裸机版 | Docker 版 |
| --- | --- | --- |
| 进程管理 | nohup + systemd 手动组合 | compose 一键托管，开机自启 |
| 端口暴露 | 22、80 | 22、80（不变） |
| MySQL 访问 | 宿主机本机 | 容器网络内（连公网都摸不到） |
| 更新后端 | scp + kill + nohup | scp + `compose restart backend` |
| 迁移到新服务器 | 重装系统重配全部 | 拷走目录 + 数据卷即可 |

**目录规划**（全部放在 `/opt/questrush-docker/`）：

```text
/opt/questrush-docker/
├── docker-compose.yml      # 编排文件
├── .env                    # 密码等敏感变量（勿提交 Git）
├── backend/
│   └── app.jar             # 后端产物（挂载进容器）
├── frontend/
│   └── dist/               # 前端静态文件（挂载进容器）
├── nginx/
│   └── questrush.conf      # Nginx 站点配置（挂载进容器）
└── mysql/
    └── init/               # 首次启动自动导入的 SQL
        └── 01-dump.sql
```

> 挂载方式 vs 打进镜像方式：本手册用「**宿主机挂载**」——jar 和 dist 放宿主机、容器运行时读挂载目录。好处是 2G 小服务器上不用在容器里跑编译，更新只换文件 + restart。把应用**打包进镜像**的进阶玩法见附录 A。

---

## 第四章 部署步骤

> 约定：`[本机]` = 在自己 Mac 的终端执行；`[服务器]` = ssh 登录后执行。
> `<尖括号>` 内容替换为真实值，密码类记入本机 LOCAL_SETUP.md。

### 阶段 1：本地打包 `[本机]`

和裸机版完全一样（Docker 不改变产物）：

```bash
cd <项目路径>/backend
mvn package -DskipTests          # 产物 target/quest-rush-backend-1.0.0.jar

cd ../frontend
npm run build                    # 产物 dist/
```

### 阶段 2：服务器建目录 + 上传 `[本机]`

> 命令全称见《部署指导手册》第一章：`ssh` = **S**ecure **Sh**ell、`scp` = **S**ecure **C**opy、`mkdir` = **M**ake **DIR**ectory（`-p` 父目录一并创建）、`tar czf` = **T**ape **AR**chive + **c**reate/**z**ip/**f**ile。
> `mkdir -p /opt/questrush-docker/{backend,frontend,nginx,mysql/init}` 里的花括号是 shell 的**批量展开**，一条命令建出 4 个目录。

```bash
ssh root@<服务器IP> "mkdir -p /opt/questrush-docker/{backend,frontend,nginx,mysql/init}"

scp backend/target/quest-rush-backend-1.0.0.jar \
  root@<服务器IP>:/opt/questrush-docker/backend/app.jar

cd frontend && tar czf /tmp/dist.tar.gz dist
scp /tmp/dist.tar.gz root@<服务器IP>:/tmp/

cd <项目路径>
scp docker-compose.yml root@<服务器IP>:/opt/questrush-docker/     # 阶段 3 写好后传
scp .env root@<服务器IP>:/opt/questrush-docker/                   # 含密码，走 scp 不走 Git
scp nginx/questrush.conf root@<服务器IP>:/opt/questrush-docker/nginx/
```

回服务器解压前端 `[服务器]`：

```bash
tar xzf /tmp/dist.tar.gz -C /opt/questrush-docker/frontend --strip-components=1
```

### 阶段 3：编写三个配置文件

**① `docker-compose.yml`**（本机项目根目录新建，内容如下）：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: questrush-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: quest_rush
      MYSQL_USER: quest_rush
      MYSQL_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Shanghai
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d:ro   # 首次启动自动执行
    networks: [questrush]

  backend:
    image: eclipse-temurin:17-jre
    container_name: questrush-backend
    restart: always
    working_dir: /app
    command: java -jar /app/app.jar
    volumes:
      - ./backend/app.jar:/app/app.jar:ro
      - backend-uploads:/app/uploads                  # 头像上传目录持久化
    environment:
      DB_HOST: mysql            # 关键：写服务名，不是 localhost！
      DB_PORT: 3306
      DB_NAME: quest_rush
      DB_USERNAME: quest_rush
      DB_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Shanghai
    depends_on:
      - mysql
    networks: [questrush]

  nginx:
    image: nginx:1.27-alpine
    container_name: questrush-nginx
    restart: always
    ports:
      - "80:80"                 # 全套服务只有这一个对宿主机/公网开放
    volumes:
      - ./nginx/questrush.conf:/etc/nginx/conf.d/default.conf:ro
      - ./frontend/dist:/usr/share/nginx/html:ro
    depends_on:
      - backend
    networks: [questrush]

volumes:
  mysql-data:
  backend-uploads:

networks:
  questrush:
```

> yaml 里各字段与缩写的英文来源：
> - `container_name` 容器名 / `restart: always` 重启策略（总是自动拉起）/ `environment` 环境变量 / `volumes` 挂载 / `depends_on` 依赖（先起 mysql 再起 backend）/ `networks` 网络 / `ports` 端口，全部是英文原词。
> - 挂载写法 `宿主机路径:容器路径:ro`，末尾 **`ro` = Read Only（只读）**，容器改不了宿主机上的这个文件。
> - `${DB_PASSWORD}` 表示"从同目录 `.env` 取值"，`.env` = **env**ironment（环境变量文件）。
> - `TZ: Asia/Shanghai` 里 **TZ = Time Zone（时区）**，不设的话容器按 UTC 走，日志时间会差 8 小时。
> - `mysql/init` 对应容器里的 `/docker-entrypoint-initdb.d`：**entrypoint** = 入口点、**init** = **init**ialize（初始化）、末尾 `d` 是 daemon（守护进程）惯例——只有数据卷首次初始化时才会执行这里的 SQL。
> - `eclipse-temurin:17-jre` 里 **JRE = Java Runtime Environment**（只负责"跑"的运行时），比 **JDK = Java Development Kit**（能编译的开发套件）小一半，容器里只跑 jar 用 JRE 就够。

> `application.yml` 里本来就支持 `DB_HOST`/`DB_PORT` 环境变量，所以后端代码**一行不用改**。

**② `.env`**（与 compose 同目录，存放密码）：

```bash
DB_PASSWORD=<数据库密码>
MYSQL_ROOT_PASSWORD=<容器MySQL的root密码，自己定一个新的>
```

> ⚠️ `.env` 含明文密码：已在 `.gitignore` 中忽略，只通过 scp 传服务器，永远不进 Git。

**③ `nginx/questrush.conf`**（注意和裸机版的唯一区别：`proxy_pass` 指向服务名）：

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location /api {
        proxy_pass http://backend:8080;      # 容器间用服务名互相访问
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

> Nginx 指令也都是英文原词：`listen 80` 监听 80 端口；`server_name _` 站点域名（`_` 表示任意域名都匹配）；`root` 站点根目录、`index` 首页文件；`proxy_pass` 把请求"转交"给后端（这就是反向代理）；`proxy_set_header` 转发时补上请求头，`Host` 是原始域名、`X-Real-IP` 是真实客户端 IP；`try_files` 依次尝试这几个路径，都找不到就回 `/index.html`（前端 history 路由刷新不 404 靠它）。

### 阶段 4：数据迁移（把宿主机 MySQL 的数据搬进容器）`[服务器]`

裸机 MySQL 里已经有你的数据（admin 账号、题目、做题记录），导出后交给容器 MySQL 首次启动自动导入：

```bash
# 从宿主机 MySQL 导出（会要 root 密码）
# mysqldump = mysql + dump（倒出来）；-u = user 账号，-p = password 密码；--databases 指定要导的库
mysqldump -uroot -p --databases quest_rush \
  > /opt/questrush-docker/mysql/init/01-dump.sql

ls -lh /opt/questrush-docker/mysql/init/   # ls = LiSt 列出；-l 详细信息，-h 人类可读大小
# 应看到 01-dump.sql（几十 KB 到几 MB）

# 关键约束：mysql 容器只在数据卷【首次初始化】时执行 init 目录里的 SQL。
# 所以如果之前试跑过 compose（数据卷已存在），必须先清掉重来：
docker volume ls | grep mysql-data    # grep = Global Regular Expression Print，从列表里筛关键字
                                      # 有输出则先 down + 删卷（见排障表）
```

### 阶段 5：切换 —— 停裸机服务，启动容器 `[服务器]`

宿主机的 nginx（占 80）和后端 java 进程必须先让路：

```bash
# 1. 停裸机 nginx（systemctl = SYSTEM CONTrol，系统服务控制）
systemctl stop nginx

# 2. 停裸机后端（2G 内存也装不下两份 java）
ps aux | grep java          # ps = Process Status；输出第二列就是 PID（Process ID，进程号）
kill <PID>                  # kill 本质是给进程发信号，默认 SIGTERM（终止）
ss -tlnp | grep -E ':(80|8080)\b'   # ss = Socket Statistics 套接字统计；-E = Extended regex（扩展正则）
                                    # 无输出 = 两个端口都释放了

# 3. 启动全套容器
cd /opt/questrush-docker    # cd = Change Directory
docker compose up -d
docker compose ps           # 三个容器都应是 Up / running
```

首次启动 mysql 容器会初始化并自动导入 dump.sql，**等待约 30 秒**，然后验证：

```bash
docker compose logs backend | tail -20      # tail = 尾巴，只看最后 20 行；看到 Started LearnApplication 即成功
docker exec questrush-mysql mysql -uquest_rush -p'<DB密码>' \
  -e "USE quest_rush; SHOW TABLES;"         # -e = execute，在终端层直接跑一条 SQL（不进 mysql> 交互层）
                                            # 表都在 = 数据迁移成功
curl -I http://127.0.0.1                    # curl = Client for URLs；-I 只要响应头；127.0.0.1 = 本机回环地址
```

最后浏览器打开 `http://<服务器IP>` → admin 登录 → 刷题正常 = 切换完成。

### 阶段 6：收尾 `[服务器]`

```bash
# 裸机 MySQL 已无用途（数据已在容器卷里），停掉并取消自启，省 500MB 内存：
# mysqld 末尾 d = daemon（守护进程，真正在后台跑的那个 MySQL 服务）
# stop = 只停这一次；disable = 取消开机自启（永久），两个要一起做才彻底
systemctl stop mysqld
systemctl disable mysqld

# JDK 也可以留着（宿主机不再需要跑 java）：
systemctl disable docker 不建议 —— Docker 现在是核心了，必须开机自启

# 防火墙不用动：还是只开放 22（SSH 默认端口）和 80（HTTP 默认端口），容器 MySQL 连宿主机端口都不占
```

> 裸机 MySQL 的数据在 `/var/lib/mysql`，建议保留 1~2 周作为回滚保险，确认容器版稳定后再删。

---

## 第五章 日常运维（Docker 版）

### 更新后端

```bash
# [本机]
mvn package -DskipTests
scp backend/target/quest-rush-backend-1.0.0.jar root@<服务器IP>:/opt/questrush-docker/backend/app.jar

# [服务器]
cd /opt/questrush-docker && docker compose restart backend
docker compose logs -f backend      # 看到 Started 即成功
```

### 更新前端

```bash
# [本机]
npm run build && tar czf /tmp/dist.tar.gz dist
scp /tmp/dist.tar.gz root@<服务器IP>:/tmp/
# [服务器]
tar xzf /tmp/dist.tar.gz -C /opt/questrush-docker/frontend --strip-components=1
# 前端是挂载的静态文件，换完即生效，无需重启任何容器
```

### 数据备份（定期做！）

```bash
# [服务器] 一行导出全库快照；$(...) 是命令替换，date +%Y%m%d 生成 20261004 这样的日期串
docker exec questrush-mysql mysqldump -uroot -p'<MYSQL_ROOT_PASSWORD>' quest_rush \
  > /opt/backup/quest_rush_$(date +%Y%m%d).sql
```

### 排障三板斧

```bash
docker compose ps                        # 1. 容器是不是都在跑
docker compose logs --tail 100 backend   # 2. 谁不正常看谁的日志（--tail 100 = 只看最后 100 行）
docker compose restart <服务名>           # 3. 大部分小毛病重启就好
```

---

## 第六章 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| backend 一直重启 / 报 Communications link failure | `DB_HOST` 写了 localhost | 容器间要用服务名 `mysql` |
| `port is already allocated` | 宿主机 80 被裸机 nginx 占用 | `systemctl stop nginx` 后再 up |
| MySQL 没有导入数据 | 数据卷已存在，init 脚本只在首次执行 | `docker compose down`，`docker volume rm` 删掉 mysql-data 卷，重新 up |
| `Access denied for 'quest_rush'` | .env 密码和建容器时不一致 | 改 .env 后 `down` + 删卷重建（会清数据，先备份） |
| 页面 502 | backend 还没起来或挂了 | `docker compose ps` + `logs backend` |
| 服务器重启后服务没了 | compose 配置缺 `restart: always` | 检查 yaml |
| 头像上传失败 | uploads 卷权限问题 | `docker exec questrush-backend ls -la /app/uploads` 检查 |
| 内存吃紧 | 2G 机器上容器 + 系统双 MySQL 并存 | 完成切换后务必停掉宿主机 mysqld |

---

## 第七章 与裸机版共存与回滚

两个方案可以随时互切，这是先学裸机再学 Docker 的额外好处：

**切回裸机**：

```bash
docker compose down                      # 停容器（数据卷保留，数据不丢）
systemctl enable --now nginx mysqld      # enable = 设开机自启，--now = 立刻就启动；裸机数据还在 /var/lib/mysql
su - admin && cd /opt/questrush          # su = Switch User 切用户；&& 表示前一条成功才执行后一条
nohup java -jar app.jar > app.log 2>&1 & # nohup = NO Hang-UP（不挂断），& 丢后台；见裸机手册阶段 5
```

**再切回 Docker**：反向操作一遍（停裸机 → `docker compose up -d`），因为 mysql-data 卷一直保留，数据无缝衔接。

---

## 附录 A：进阶 —— 把应用打包进镜像（自建镜像版）

本手册用挂载方式部署。想体验完整 Docker 工作流（CI/CD 的基础），可以改成构建镜像：

**`backend/Dockerfile`**：

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/quest-rush-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`frontend/Dockerfile`**：

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY ../nginx/questrush.conf /etc/nginx/conf.d/default.conf
```

Dockerfile 指令全称（全是大写英文原词，不是缩写）：

| 指令 | 英文直译 | 作用 |
| --- | --- | --- |
| `FROM` | from（"从……来"） | 声明基础镜像，一切从它起步 |
| `WORKDIR` | **WORK** **DIR**ectory（工作目录） | 之后的命令都在这个目录里执行 |
| `COPY` | copy（复制） | 把宿主机文件拷进镜像 |
| `RUN` | run（运行） | **构建镜像时**执行，结果固化进镜像层（区别于容器启动时才跑的 `ENTRYPOINT`） |
| `EXPOSE` | expose（暴露） | 声明容器监听哪个端口，主要给使用者看的说明 |
| `ENTRYPOINT` | **ENTRY POINT**（入口点） | 容器启动时固定执行的命令 |
| `AS build` | as（"起别名叫 build"） | 给多阶段构建的这一阶段命名，后面才能用 `--from=build` 引用它 |
| `npm ci` | **C**ontinuous **I**ntegration（持续集成） | 严格按 `package-lock.json` 安装，构建环境比 `npm install` 更稳定 |
| `--build` | build（构建） | `docker compose up -d --build` = 启动前先本地构建镜像 |

对应地，compose 里 `image: eclipse-temurin:17-jre` + 挂载改为：

```yaml
  backend:
    build: ./backend          # 用 build 代替 image + volumes
```

然后 `docker compose up -d --build` 即可自动构建。多阶段构建（multi-stage build）里 `node:20` 只是编译用的临时层，最终镜像只含编译产物，这就是 Docker 生态里前端项目的标准玩法。

**附录 B：本次切换 checklist**

- [ ] 阶段 1 本地打包 jar + dist
- [ ] 阶段 2 目录建立、文件上传
- [ ] 阶段 3 compose / .env / nginx.conf 三件套就位
- [ ] 阶段 4 mysqldump 导出进 init 目录
- [ ] 阶段 5 停裸机 → compose up → 三容器 Up
- [ ] 数据验证：SHOW TABLES + 浏览器登录刷题
- [ ] 阶段 6 停宿主机 mysqld，裸机数据保留 1~2 周
