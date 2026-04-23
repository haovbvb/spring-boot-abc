# spring-boot-abc

企业级 Spring Boot 基础框架（Spring Boot 3.3 + JDK 17）。

## 模块结构

```
spring-boot-abc
├── abc-dependencies        # 统一 BOM
├── abc-common              # 通用响应/异常/分页/错误码
├── abc-framework
│   ├── abc-framework-web       # 统一响应、全局异常、TraceId、CORS、Jackson
│   ├── abc-framework-mybatis   # MyBatis-Plus、分页、乐观锁、审计填充、BaseEntity
│   ├── abc-framework-redis     # RedisTemplate、Redisson、@RateLimit 限流
│   ├── abc-framework-security  # Spring Security 6 + JWT
│   ├── abc-framework-log       # @OperationLog 注解 + AOP
│   └── abc-framework-openapi   # SpringDoc OpenAPI 3
└── abc-application         # 启动模块 + 示例 user / auth
```

## 构建运行

```bash
# 1. 准备 MySQL(abc) 和 Redis，按需改 application-dev.yml
mvn -DskipTests clean package
java -jar abc-application/target/abc-application.jar

# 2. Docker
docker build -t abc/app:1.0.0 .
docker run --rm -p 8080:8080 -e ABC_JWT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx abc/app:1.0.0
```

## 核心能力

- **统一响应**：`R<T>{code,message,data,traceId,timestamp}`
- **全局异常**：`BizException` + `GlobalExceptionHandler`
- **TraceId**：`X-Trace-Id` 贯穿日志 MDC 与响应头
- **鉴权**：`/auth/register` 注册用户并返回 JWT，`/auth/login` 签发 JWT；`@PreAuthorize` 支持 RBAC
- **限流**：`@RateLimit(limit=20, period=1, key="#id")`
- **操作日志**：`@OperationLog(module="user", value="query")`
- **审计字段**：继承 `BaseEntity` 自动填充 `createTime/updateTime/createBy/updateBy/deleted`
- **文档**：访问 `/swagger-ui.html`
- **监控**：`/actuator/health`, `/actuator/prometheus`

## 规范约定

- 分层：`controller → service → mapper`；DTO/VO/Entity 分离，MapStruct 转换。
- 错误码 6 位：业务域(2)+模块(2)+序号(2)，集中在枚举维护。
- 日志字段：`traceId` 必带；生产 profile 额外输出 JSON 到 `logs/*.json.log`。
- 密钥：`ABC_JWT_SECRET` 等一律从配置中心/K8s Secret 注入，禁止明文入库。
- 提交：Checkstyle + Spotless + SonarQube（建议在 CI 强制）。

## 后续扩展建议

1. 接入 Nacos 作为配置中心与注册中心（`spring-cloud-starter-alibaba-nacos-*`）。
2. 多数据源（dynamic-datasource-spring-boot-starter）与分库分表（ShardingSphere）。
3. 消息：RocketMQ / Kafka starter + 事务消息模板。
4. 链路：SkyWalking / OpenTelemetry Java Agent，免代码接入。
5. 代码生成：`abc-generator` 模块基于 MyBatis-Plus Generator。
6. CI/CD：GitHub Actions / GitLab CI + Helm Chart 部署到 K8s。

## CI/CD（GitHub Actions + Docker）

目录：

- [.github/workflows/ci.yml](.github/workflows/ci.yml)：PR/push 触发，执行 `mvn verify`，上传 jar。
- [.github/workflows/release.yml](.github/workflows/release.yml)：main 分支或 `v*` tag 触发，构建多架构镜像推到 GHCR，再通过 SSH 部署到服务器。
- [Dockerfile](Dockerfile)：多阶段构建（Maven → JRE），内置 `HEALTHCHECK`。
- [deploy/docker-compose.dev.yml](deploy/docker-compose.dev.yml)：本地开发 MySQL 8.4 + Redis 7。
- [deploy/docker-compose.prod.yml](deploy/docker-compose.prod.yml)：服务器端一键拉起 app + mysql + redis。
- [deploy/.env.example](deploy/.env.example)：生产敏感变量模板。
- [abc-application/src/main/resources/db/migration/common/V1\_\_init_schema.sql](abc-application/src/main/resources/db/migration/common/V1__init_schema.sql)：通用 schema 迁移。
- [abc-application/src/main/resources/db/migration/dev/R\_\_seed_demo_user.sql](abc-application/src/main/resources/db/migration/dev/R__seed_demo_user.sql)：dev 示例数据迁移。

### 本地开发（推荐：应用本机跑 + 中间件 Docker）

```bash
# 启中间件
docker compose -f deploy/docker-compose.dev.yml up -d

# 跑应用
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl abc-application -am spring-boot:run
```

说明：

- 应用启动时由 Flyway 自动执行数据库迁移（dev / prod 路径统一）。
- 首次迁移会创建 `flyway_schema_history` 表并应用 `db/migration` 中的脚本。
- 若你已存在旧数据，想重新初始化，可执行：

```bash
docker compose -f deploy/docker-compose.dev.yml down
docker volume rm deploy_abc-mysql
docker compose -f deploy/docker-compose.dev.yml up -d
mvn -pl abc-application -am spring-boot:run
```

### 本地端到端自测

```bash
# 1) 健康检查（基础）
curl -s http://localhost:8080/actuator/health

# 1.1) 健康检查（部署版本）
curl -s http://localhost:8080/actuator/health/deploy

# 1.2) 仅提取当前部署版本（APP_VERSION）
curl -s http://localhost:8080/actuator/health/deploy | sed -n 's/.*"version":"\([^"]*\)".*/\1/p'

# 2) 注册并拿 token
REG_USER="demo_$(date +%s)"
REGISTER_JSON=$(curl -s -X POST http://localhost:8080/auth/register \
	-H 'Content-Type: application/json' \
	-d "{\"username\":\"$REG_USER\",\"password\":\"demo123\",\"nickname\":\"Demo User\"}")
TOKEN=$(echo "$REGISTER_JSON" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# 3) 调鉴权接口
curl -s http://localhost:8080/users/1 -H "Authorization: Bearer $TOKEN"
```

### Redis / Redisson 环境策略

- 开发环境：在 [abc-application/src/main/resources/application-dev.yml](abc-application/src/main/resources/application-dev.yml) 中排除 `RedissonAutoConfigurationV2`，避免本地 Redis 无密码时触发 AUTH 报错。
- 生产环境：在 [deploy/docker-compose.prod.yml](deploy/docker-compose.prod.yml) 中启用 Redis 密码（`REDIS_PASSWORD`），并通过 `SPRING_DATA_REDIS_PASSWORD` 注入应用，支持 Redisson 正常连接。

### Flyway 迁移策略

- 通用迁移放在 `db/migration/common`，dev 与 prod 都会执行。
- dev 专属迁移放在 `db/migration/dev`，仅 dev profile 执行（例如 demo 数据）。
- 新增结构变更时，请追加新的 `V{version}__*.sql`，不要改历史版本脚本。

### 服务器首次准备

```bash
# 1. 服务器登录并安装 docker / docker compose
# 2. 生成 SSH 部署密钥（本地执行，公钥写入服务器 ~/.ssh/authorized_keys）
ssh-keygen -t ed25519 -C deploy -f ./deploy_key

# 3. 在 GitHub 仓库 Settings → Secrets and variables → Actions 配置：
#    DEPLOY_HOST     服务器 IP/域名
#    DEPLOY_USER     部署用户名
#    DEPLOY_PORT     SSH 端口（如 22）
#    DEPLOY_SSH_KEY  deploy_key 的私钥全文
#    MYSQL_ROOT_PASSWORD / MYSQL_APP_PASSWORD / REDIS_PASSWORD / ABC_JWT_SECRET
#                    以上可配为 Secrets（不配则使用 compose 默认值）
#    DEPLOY_SERVICE_NAME（Repository Variable，可选）例如 abc-user / abc-order
#    DEPLOY_ROOT（Repository Variable，可选）默认 /opt/services
#    DEPLOY_DATA_ROOT（Repository Variable，可选）默认 /data/services
#    在 Settings → Environments 新建 "production"，可加保护规则。
```

### 发布流程

```bash
# 普通发布：合并到 main 自动触发 release.yml
git push origin main

# 语义化版本发布：打 tag 会生成 v1.0.0 / 1.0 两个镜像标签
git tag v1.0.0 && git push origin v1.0.0
```

镜像地址形如：`ghcr.io/<org>/<repo>:1.0.0`。GHCR 默认为私有，需 `docker login ghcr.io` 后拉取。

### 服务器通过 compose 手动运行（与 release.yml 同构）

```bash
cd /opt/abc
cp deploy/.env.example .env && vim .env
# 显式宿主机数据目录（可在 .env 中调整 ABC_DATA_ROOT）
mkdir -p /data/services/abc/mysql /data/services/abc/redis /data/services/abc/app-logs
# 确保 docker 有权限写入目录
chown -R 999:999 /data/services/abc/mysql /data/services/abc/redis
chown -R 1000:1000 /data/services/abc/app-logs
# 不改 .env 也可直接运行（compose 内置了同款默认值）
# 将 prod compose 中 image 改为你自己的 GHCR 镜像
docker compose -f deploy/docker-compose.prod.yml pull
docker compose -f deploy/docker-compose.prod.yml up -d
```

自动发布说明：

`release.yml` 现在会在服务器自动创建目录，上传仓库中的固定 `deploy/docker-compose.prod.yml`，并按环境变量动态生成 `.env`。

- 部署目录：`${DEPLOY_ROOT}/${SERVICE_NAME}`（默认 `/opt/services/<service>`）
- 数据目录：`${DEPLOY_DATA_ROOT}/${SERVICE_NAME}/mysql|redis|app-logs`（默认 `/data/services/<service>/...`）
- 应用对外端口统一固定为 `18080`。
- 手动触发 `workflow_dispatch` 时可填写 `service_name`，端口不再作为输入项。
- 发布后可通过 `/actuator/health/deploy` 查看 `version`（git sha）、`image`、`buildVersion` 等字段，判断是否为最新版本。

### 常见坑位

- GHCR 推送需要 `permissions: packages: write`（已在 workflow 配置）。
- 镜像默认私有，仓库 → Packages → 镜像设置里改可见性或设置 pull secret。
- 多架构构建较慢，仅生产 tag 发布时启用 `linux/arm64` 可降低日常 CI 时间。
- 服务器防火墙只放开 80/443，8080 走内网或反向代理（Nginx/Caddy）。
- Flyway 在应用启动阶段执行；MySQL 仅启动完成并不代表迁移已完成。
- 若本地 Redis 不设密码且启用 Redisson 自动装配，可能出现 `ERR AUTH`，请保持 dev 环境的 Redisson 排除配置。
