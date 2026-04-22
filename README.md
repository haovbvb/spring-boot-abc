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
- **鉴权**：`/auth/login` 签发 JWT；`@PreAuthorize` 支持 RBAC
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
- [deploy/sql/mysql/01_init_abc.sql](deploy/sql/mysql/01_init_abc.sql)：本地 MySQL 初始化脚本（建表 + demo 数据）。

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

- `deploy/sql/mysql/01_init_abc.sql` 会在 **MySQL 数据卷为空时** 自动执行。
- 若你已存在旧数据，想重新初始化，可执行：

```bash
docker compose -f deploy/docker-compose.dev.yml down
docker volume rm deploy_abc-mysql
docker compose -f deploy/docker-compose.dev.yml up -d
```

### 本地端到端自测

```bash
# 1) 健康检查
curl -s http://localhost:8080/actuator/health

# 2) 登录拿 token
LOGIN_JSON=$(curl -s -X POST http://localhost:8080/auth/login \
	-H 'Content-Type: application/json' \
	-d '{"username":"demo","password":"demo"}')
TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

# 3) 调鉴权接口
curl -s http://localhost:8080/users/1 -H "Authorization: Bearer $TOKEN"
```

### Redis / Redisson 环境策略

- 开发环境：在 [abc-application/src/main/resources/application-dev.yml](abc-application/src/main/resources/application-dev.yml) 中排除 `RedissonAutoConfigurationV2`，避免本地 Redis 无密码时触发 AUTH 报错。
- 生产环境：在 [deploy/docker-compose.prod.yml](deploy/docker-compose.prod.yml) 中启用 Redis 密码（`REDIS_PASSWORD`），并通过 `SPRING_DATA_REDIS_PASSWORD` 注入应用，支持 Redisson 正常连接。

### 服务器首次准备

```bash
# 1. 服务器登录并安装 docker / docker compose
# 2. 创建独立网络（release.yml 中使用）
docker network create abc-net || true

# 3. 生成 SSH 部署密钥（本地执行，公钥写入服务器 ~/.ssh/authorized_keys）
ssh-keygen -t ed25519 -C deploy -f ./deploy_key

# 4. 在 GitHub 仓库 Settings → Secrets and variables → Actions 配置：
#    DEPLOY_HOST     服务器 IP/域名
#    DEPLOY_USER     部署用户名
#    DEPLOY_PORT     SSH 端口（如 22）
#    DEPLOY_SSH_KEY  deploy_key 的私钥全文
#    ABC_JWT_SECRET  JWT 密钥（>=32 字节）
#    REDIS_PASSWORD  Redis 密码（生产环境必填）
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

### 服务器通过 compose 运行（可选替代 release.yml 里的 docker run）

```bash
cd /opt/abc
cp deploy/.env.example .env && vim .env
# 将 prod compose 中 image 改为你自己的 GHCR 镜像
docker compose -f deploy/docker-compose.prod.yml pull
docker compose -f deploy/docker-compose.prod.yml up -d
```

### 常见坑位

- GHCR 推送需要 `permissions: packages: write`（已在 workflow 配置）。
- 镜像默认私有，仓库 → Packages → 镜像设置里改可见性或设置 pull secret。
- 多架构构建较慢，仅生产 tag 发布时启用 `linux/arm64` 可降低日常 CI 时间。
- 服务器防火墙只放开 80/443，8080 走内网或反向代理（Nginx/Caddy）。
- dev 环境 MySQL 初始化脚本只在 **首次建卷** 时执行；已有卷不会重复执行。
- 若本地 Redis 不设密码且启用 Redisson 自动装配，可能出现 `ERR AUTH`，请保持 dev 环境的 Redisson 排除配置。
