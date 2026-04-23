# AI 协作指南（spring-boot-abc）

> 这是给 AI/Copilot 的精简上下文。阅读后直接动手，不要再逐一探测。

## 技术栈基线

- Java 17（Temurin），Spring Boot 3.3.4
- Maven 多模块 + BOM 统一版本
- MyBatis-Plus + Flyway（MySQL 8.4）
- Redis 7 + Redisson
- Spring Security 6 + JWT（jjwt）
- SpringDoc OpenAPI 3
- Lombok + MapStruct 1.6

## 构建 / 运行

```bash
# 必须先切 JDK17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# 编译
mvn -DskipTests clean package

# 启动（本地需先起中间件）
docker compose -f deploy/docker-compose.dev.yml up -d
mvn -pl abc-application -am spring-boot:run
```

应用端口：**容器内 8080，宿主机 18080**（生产）。

## 模块职责（改代码先看这里）

| 模块                     | 只能放什么                                                                           |
| ------------------------ | ------------------------------------------------------------------------------------ |
| `abc-dependencies`       | BOM，仅管理版本号                                                                    |
| `abc-common`             | `R<T>` / `ResultCode` / `IErrorCode` / `BizException` / `PageResult`，无 Spring 依赖 |
| `abc-framework-web`      | 统一响应、全局异常、TraceId、CORS、Jackson                                           |
| `abc-framework-mybatis`  | MP 配置、分页、乐观锁、`BaseEntity` 审计填充                                         |
| `abc-framework-redis`    | RedisTemplate、Redisson、`@RateLimit` 限流                                           |
| `abc-framework-security` | Spring Security 配置、JWT、鉴权异常处理                                              |
| `abc-framework-log`      | `@OperationLog` 注解 + AOP                                                           |
| `abc-framework-openapi`  | SpringDoc 配置                                                                       |
| `abc-application`        | 业务代码（启动模块）                                                                 |

**绝对禁止**：在 framework-\* 里写业务；在 application 里放可复用基础设施。

## 代码约定（写新接口必遵守）

1. **目录分层**：`controller → service → mapper`，按领域聚合：
   ```
   com.abc.<domain>/
     controller/  dto/  vo/  entity/  mapper/  service/
   ```
2. **不要直接返回 Entity** —— 一律用 `dto`（入参）/`vo`（出参）。实体字段如 `password` 禁止出接口。
3. **响应体必须 `R<T>`**：`return R.ok(data)` / `return R.fail(ResultCode.X)`。
4. **业务失败抛 `BizException`**，已有全局处理器接管，不要在 Controller 里 try-catch。
5. **入参校验**用 Jakarta Validation（`@NotBlank`/`@Size` 等）+ `@Valid`。
6. **鉴权**：接口默认需 JWT；公开接口显式放行（见 `SecurityConfig`）。RBAC 用 `@PreAuthorize("hasRole('X')")`。
7. **新错误码**：加到 `ResultCode` 枚举，6 位：`业务域(2)+模块(2)+序号(2)`。
8. **日志**：用 `@Slf4j`；关键操作加 `@OperationLog(module=..., value=...)`；TraceId 会自动进 MDC。

## 数据库迁移

- 通用：`abc-application/src/main/resources/db/migration/common/V{n}__desc.sql`
- dev 专属：`.../db/migration/dev/...`（仅 dev profile 执行）
- **不要改历史 V 脚本**，新增追加版本号即可。

## CI/CD 踩坑备忘（AI 常犯）

1. **GitHub Actions job-level `env:` 块里不能引用 `env.XXX`**，只能用字面量 / `vars.*` / `secrets.*`。
2. **多架构构建只在 tag 触发**（`v*`），main 推送只构建 amd64。
3. `deploy/docker-compose.prod.yml` 是唯一生产 compose，通过 scp 上传到服务器 `/opt/services/<service>/`。
4. `.env` 在部署步骤里动态生成，不要 commit 真实 secret。
5. 新增环境变量需同时改：workflow 的 `env:` → `.env` 生成块 → `docker-compose.prod.yml` → `deploy/.env.example`。
6. 版本号通过 `APP_VERSION=${{ github.sha }}` 注入，健康检查 `/actuator/health/deploy` 可读回。

## 测试

- 位置：`abc-application/src/test/java/com/abc/...`
- 纯逻辑优先用 JUnit 5 + Mockito（不启 Spring 上下文）
- 集成测试用 `@SpringBootTest`，数据库用 Testcontainers（未内置，按需添加）

## 不要做的事

- ❌ 不要在 controller 里注入 `Mapper` 直接查（样例暂有，新代码应走 service 层）
- ❌ 不要把密码/JWT secret 写进 yml，用 `${ABC_JWT_SECRET:fallback}` 环境变量
- ❌ 不要加 `@Autowired` 字段注入，用构造器（`@RequiredArgsConstructor`）
- ❌ 不要在 Entity 上加 `@JsonIgnore` 当脱敏手段，用独立 VO
- ❌ 不要提交 `.env`、`*.log`、`target/`
