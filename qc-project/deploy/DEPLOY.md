# 心内六科质控系统 — 阿里云部署手册

## 一、推荐架构

```
用户浏览器
    │  HTTP/HTTPS
    ▼
阿里云 ECS（Ubuntu 22.04）
├── Nginx 容器  :80   → 托管前端静态文件
│       │  /api/* 反向代理
│       ▼
├── Spring Boot 容器  :8080  → 后端 REST API
│       │  JDBC
│       ▼
└── MySQL 容器  :3306  → 数据持久化
         │  数据卷
         ▼
    /var/lib/mysql（ECS 磁盘）
```

> 如需高可用，可将 MySQL 替换为**阿里云 RDS MySQL 8.0**，将静态文件托管到 **OSS + CDN**。

---

## 二、ECS 服务器配置建议

| 配置项 | 推荐值 |
|--------|--------|
| 规格   | 2 核 4 GB（ecs.c7.large） |
| 操作系统 | Ubuntu 22.04 LTS |
| 系统盘  | 40 GB SSD |
| 数据盘  | 50 GB（挂载到 /data，存放 MySQL 数据卷） |
| 安全组  | 入站开放 80、443（HTTPS 可选）；关闭 3306 外网访问 |
| 公网带宽 | 5 Mbps 按量付费 |

---

## 三、一键部署步骤

### 3.1 初始化服务器

```bash
# 1. 登录 ECS
ssh root@<你的ECS公网IP>

# 2. 更新系统
apt update && apt upgrade -y

# 3. 安装 Docker + Docker Compose
curl -fsSL https://get.docker.com | bash
apt install -y docker-compose-plugin

# 4. 启动 Docker 并设置开机自启
systemctl enable --now docker

# 5. 验证
docker --version && docker compose version
```

### 3.2 上传项目文件

```bash
# 在本机执行（将项目打包上传到 ECS）
scp -r ./qc-project root@<ECS公网IP>:/opt/qc-project

# 或使用 git clone（推荐）
# git clone https://your-repo/qc-project.git /opt/qc-project
```

### 3.3 配置环境变量

```bash
cd /opt/qc-project/deploy

# 复制并编辑环境变量
cp .env.example .env
vi .env

# 必须修改的项：
# MYSQL_ROOT_PASSWORD  → 设置强密码
# DB_PASS              → 设置强密码
# JWT_SECRET           → 至少 32 位随机字符串
```

**生成安全的 JWT_SECRET：**
```bash
openssl rand -base64 48
```

### 3.4 启动所有服务

```bash
cd /opt/qc-project/deploy

# 构建并启动（首次约需 3-5 分钟下载镜像、编译 Java）
docker compose up -d --build

# 查看启动日志
docker compose logs -f

# 确认三个容器均为 running
docker compose ps
```

### 3.5 验证部署

```bash
# 检查后端健康
curl http://localhost:8080/api/nurses/list

# 检查 Nginx
curl http://localhost:80

# 检查数据库
docker exec qc-mysql mysql -uqc_user -pqc_pass_2024 qc_system -e "SELECT COUNT(*) FROM nurse;"
```

浏览器访问 `http://<ECS公网IP>` 看到登录页即为成功。

---

## 四、日常运维命令

```bash
# 查看所有容器状态
docker compose -f /opt/qc-project/deploy/docker-compose.yml ps

# 查看后端实时日志
docker logs -f qc-backend

# 查看 Nginx 访问日志
docker exec qc-nginx tail -f /var/log/nginx/qc_access.log

# 重启某个服务
docker compose -f /opt/qc-project/deploy/docker-compose.yml restart backend

# 更新后端（重新编译部署）
cd /opt/qc-project/deploy
docker compose up -d --build backend

# 更新前端（直接替换文件，Nginx 自动生效）
cp -r /path/to/new-frontend/* /opt/qc-project/frontend/
```

---

## 五、数据库备份与恢复

### 备份

```bash
# 手动备份
docker exec qc-mysql \
  mysqldump -uqc_user -pqc_pass_2024 qc_system \
  > /data/backup/qc_$(date +%Y%m%d_%H%M).sql

# 配置每日自动备份（crontab）
crontab -e
# 加入：每天凌晨 2 点备份，保留 30 天
# 0 2 * * * docker exec qc-mysql mysqldump -uqc_user -pqc_pass_2024 qc_system > /data/backup/qc_$(date +\%Y\%m\%d).sql && find /data/backup -mtime +30 -delete
```

### 恢复

```bash
cat /data/backup/qc_20241201.sql | \
  docker exec -i qc-mysql mysql -uqc_user -pqc_pass_2024 qc_system
```

---

## 六、API 接口一览

> Base URL：`http://<域名>/api`

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/auth/login` | 无 | 护士登录，返回 JWT |
| GET  | `/nurses/list` | 无 | 获取启用护士列表 |
| GET  | `/nurses/all` | 管理员 | 获取全部护士 |
| POST | `/nurses` | 管理员 | 新增护士 |
| PUT  | `/nurses/{id}` | 管理员 | 修改护士 |
| DELETE | `/nurses/{id}` | 管理员 | 删除护士 |
| POST | `/reports` | 护士 | 提交/更新日报 |
| GET  | `/reports/mine` | 护士 | 我的历史日报 |
| GET  | `/reports/by-date/{date}` | 护士 | 按日期查日报 |
| GET  | `/reports/admin/all` | 管理员 | 所有日报（可筛选） |
| DELETE | `/reports/{id}` | 管理员 | 删除日报 |
| GET  | `/reports/admin/export` | 管理员 | 导出 Excel |
| POST | `/config/password` | 管理员 | 修改密码 |

**JWT 使用方式：**
```
Authorization: Bearer <token>
```

---

## 七、常见问题

**Q: 首次启动后端报数据库连接失败？**  
A: MySQL 启动需要约 30 秒，等待后执行 `docker compose restart backend`。

**Q: 如何配置 HTTPS？**  
A: 推荐使用阿里云 SLB（负载均衡）在前端终止 SSL，后端保持 HTTP 即可。或在 ECS 上安装证书：
```nginx
# nginx.conf 中添加
listen 443 ssl;
ssl_certificate     /etc/nginx/certs/fullchain.pem;
ssl_certificate_key /etc/nginx/certs/privkey.pem;
```

**Q: 后端 OutOfMemoryError？**  
A: 调整 Dockerfile 中的 `-Xmx512m` 参数，4 GB 内存服务器建议设为 `-Xmx1g`。

**Q: 如何使用阿里云 RDS 替换 MySQL 容器？**  
A: 修改 `.env` 中 `DB_HOST` 为 RDS 内网地址，删除 `docker-compose.yml` 中的 `mysql` 服务即可。

---

## 八、完整目录结构

```
qc-project/
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/xnl/qc/
│       ├── QcSystemApplication.java
│       ├── config/WebConfig.java
│       ├── controller/          ← 4 个 Controller
│       ├── dto/Dto.java
│       ├── entity/              ← 4 个 Entity
│       ├── exception/           ← BusinessException + GlobalExceptionHandler
│       ├── repository/          ← 3 个 Repository
│       ├── security/            ← JwtUtil + JwtFilter
│       └── service/             ← 4 个接口 + 4 个实现
│           └── impl/
├── frontend/
│   ├── index.html               ← 登录页
│   ├── css/main.css
│   ├── js/
│   │   ├── api.js               ← 所有接口封装
│   │   ├── constants.js         ← 质控类别定义
│   │   └── ui.js                ← Toast/Loading/Confirm
│   └── pages/
│       └── main.html            ← 主应用页（填报+数据+设置）
└── deploy/
    ├── schema.sql               ← 数据库建表+初始化
    ├── docker-compose.yml       ← 一键启动
    ├── nginx.conf               ← Nginx 配置
    └── .env.example             ← 环境变量模板
```

---

## 九、v2 版本变更说明（与最新 HTML 对齐）

### 数据库变更
| 表 | 变更内容 |
|----|---------|
| `nurse` | 新增 `password` 字段（每人独立密码，默认 `XNL226`） |
| `day_report` | 原 `adm_total` 拆分为 `adm_yesterday`（昨日）+ `adm_today`（今日，自动计算）|
| `qc_item` | 新增 `na_count`（不适用人数）、`names_input_type`（'free'\|'multi'）字段 |

**数据库迁移脚本（已有数据库升级用）：**
```sql
USE qc_system;
-- 护士表加密码字段
ALTER TABLE nurse ADD COLUMN IF NOT EXISTS password VARCHAR(100) NOT NULL DEFAULT 'XNL226' COMMENT '个人密码';
-- 日报表出入院字段重命名
ALTER TABLE day_report
  CHANGE COLUMN adm_total adm_yesterday INT COMMENT '昨日患者总人数',
  ADD COLUMN adm_today INT COMMENT '今日患者总人数' AFTER adm_yesterday;
-- 条目表新增字段
ALTER TABLE qc_item
  ADD COLUMN IF NOT EXISTS na_count INT COMMENT '不适用人数',
  ADD COLUMN IF NOT EXISTS names_input_type VARCHAR(10) NOT NULL DEFAULT 'multi' COMMENT 'free|multi';
```

### 接口变更
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/change-password` | 护士修改自己的密码（新增）|
| POST | `/api/nurses/reset-password` | 管理员重置指定护士密码（新增）|
| POST | `/api/config/admin-password` | 修改管理员密码（原 `/api/config/password` 重命名）|

### 质控条目变更（新增 3 条）
- **基础护理**新增：`护理员能说出重点患者的风险因素：一级、压疮、高跌、管路、皮肤`（手工输入）
- **防跌措施**新增：`护理员能说出分管高跌患者及风险因素`（手工输入）
- **防跌措施**新增：`科室高跌患者均有红色腕带（尤其关注转入和术后）`（多选）

### FREE_INPUT_KEYS（手工输入条目）
后端 `ReportServiceImpl.FREE_INPUT_KEYS` 与前端 `constants.js` 保持同步：
- `basic_0`、`basic_1`（基础护理所有条目）
- `fall_8`（护理员能说出分管高跌患者及风险因素）
