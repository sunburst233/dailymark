# DailyMark 后端文档

> 本文档以**当前代码的实际行为**为准。与 [DESIGN.md](DESIGN.md) 的差异见文末“已知问题与待办”。前端见 [FRONTEND.md](FRONTEND.md)。

## 1. 概述与技术栈

单个 Spring Boot 进程同时提供 REST API、图片静态访问和前端静态资源(构建时将前端 `dist` 复制到 `src/main/resources/static`)。

| 用途 | 选型 |
| --- | --- |
| 运行时 | Java 17,Spring Boot 3.3.5 |
| Web | Spring Web MVC |
| 持久化 | Spring Data JPA + H2(文件模式) |
| 安全 | Spring Security + 自定义 `ApiKeyFilter` |
| 运维 | Actuator(仅暴露 `health`) |
| 构建 | Maven |

## 2. 目录与模块

```text
backend/src/main/java/com/dailymark/
├── DailyMarkApplication.java
├── config/
│   ├── SecurityConfig.java      # 无状态、关闭 CSRF,注册 ApiKeyFilter
│   ├── ApiKeyFilter.java        # Bearer Key 校验(仅写请求)
│   ├── StorageProperties.java   # dailymark.* 配置,启动校验并创建目录
│   └── WebConfig.java           # /assets/** 资源处理器;SVG 加 CSP 头
├── controller/
│   ├── DocumentController.java  # /api/docs
│   ├── AssetController.java     # /api/assets
│   └── SpaForwardController.java# / 与 /doc/** 转发到 /index.html
├── service/
│   ├── DocumentService.java     # 校验、标题/slug 提取、搜索、覆盖
│   ├── AssetService.java        # 图片校验与存储
│   └── StorageService.java      # 文件落盘(临时文件 + 原子移动)
├── repository/                  # DocumentRepository, AssetRepository
├── entity/                      # Document, Asset
├── dto/                         # DocSummary, DocDetail, AssetResponse, ErrorResponse
└── exception/                   # BusinessException, GlobalExceptionHandler
```

## 3. 数据模型与磁盘布局

### 3.1 `document`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID, PK | 文档 ID |
| `slug` | VARCHAR(255) | 文件名去扩展名、转小写、非字母数字(Unicode)替换为 `-` |
| `title` | VARCHAR(255) | 正文第一个 `# ` 一级标题;没有则用文件名去扩展名 |
| `filename` | VARCHAR(255), UNIQUE | 上传时的原始文件名(已去路径),用于判断同名覆盖 |
| `storage_path` | VARCHAR(512) | `{id}.md` |
| `size` | BIGINT | 字节数 |
| `content` | LOB | 正文副本,用于搜索 |
| `uploaded_at` | TIMESTAMP(Instant) | 首次上传时间,排序依据,覆盖时不变 |
| `updated_at` | TIMESTAMP(Instant) | 最后修改时间 |

索引:`idx_document_uploaded_at(uploaded_at)`(升序索引,查询为倒序)。

### 3.2 `asset`

`id`、`filename`(`{id}.{ext}`,唯一)、`original_name`、`storage_path`、`content_type`、`size`、`uploaded_at`。

### 3.3 磁盘布局

```text
{data-dir}
├── db/        # H2 数据库 dailymark.mv.db
├── docs/      # {id}.md
└── assets/    # {id}.{ext}
```

写入流程:`file.transferTo({target}.tmp)` → 原子移动(`ATOMIC_MOVE`,不支持时退化为普通移动并覆盖)→ 保存数据库记录。数据库中的 `content` 是搜索用副本。

## 4. API 详解

JSON 使用 camelCase;时间为 `Instant` 序列化的 ISO-8601 字符串(UTC,以 Spring 默认 Jackson 配置为准)。写接口需要请求头 `Authorization: Bearer <key>`。

示例变量:

```bash
KEY=<your-api-key>
HOST=http://localhost:8080
```

### 4.1 接口一览

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/docs?q=` | 否 | 文档列表,按 `uploadedAt` 倒序;`q` 非空时按标题/正文搜索 |
| GET | `/api/docs/latest` | 否 | 最新文档(含正文),无文档返回 404 |
| GET | `/api/docs/{id}` | 否 | 指定文档(含正文) |
| POST | `/api/docs` | 是 | 上传文档,字段 `file`,可选 `overwrite=true` |
| PUT | `/api/docs/{id}` | 是 | 替换指定文档内容,字段 `file` |
| DELETE | `/api/docs/{id}` | 是 | 删除文档,返回 204 |
| POST | `/api/assets` | 是 | 上传图片,字段 `file` |
| DELETE | `/api/assets/{id}` | 是 | 删除图片,返回 204 |
| GET | `/assets/{filename}` | 否 | 图片访问(缓存 1 小时) |
| GET | `/actuator/health` | 否 | 健康检查 |

### 4.2 数据结构

`DocSummary`:

```json
{
  "id": "3f1c...",
  "slug": "2026-10-14-daily",
  "title": "10 月 14 日日报",
  "filename": "2026-10-14-daily.md",
  "size": 2048,
  "uploadedAt": "2026-10-14T12:00:00Z",
  "updatedAt": "2026-10-14T12:00:00Z"
}
```

`DocDetail` = `DocSummary` + `"content": "..."`。

`AssetResponse`:`{ "id", "url": "/assets/{id}.{ext}", "originalName", "size" }`(设计稿示例;实际字段以 `AssetResponse` 为准)。

### 4.3 文档上传 / 覆盖

```bash
# 新建
curl -X POST "$HOST/api/docs" -H "Authorization: Bearer $KEY" -F "file=@2026-10-14-daily.md"
# 覆盖同名文档
curl -X POST "$HOST/api/docs?overwrite=true" -H "Authorization: Bearer $KEY" -F "file=@2026-10-14-daily.md"
# 按 ID 替换
curl -X PUT "$HOST/api/docs/<id>" -H "Authorization: Bearer $KEY" -F "file=@new.md"
# 删除
curl -X DELETE "$HOST/api/docs/<id>" -H "Authorization: Bearer $KEY"
```

| 场景 | 结果 |
| --- | --- |
| `filename` 不存在 | 新建,`201 Created` + `DocSummary` |
| `filename` 已存在且无 `overwrite=true` | `409 Conflict` |
| `filename` 已存在且 `overwrite=true` | 覆盖,`200 OK`;保留 `id` 与 `uploadedAt`,更新 `updatedAt`、`title`、`size`、`slug`、`content` |
| `PUT /api/docs/{id}` | 替换内容,文件名不变;规则同覆盖,返回 `200` |
| 并发创建同名 | 唯一约束冲突转换为 `409` |

注意:`PUT` 只校验新文件的扩展名/大小/UTF-8,不要求新文件名与原文档一致。

### 4.4 图片

```bash
curl -X POST "$HOST/api/assets" -H "Authorization: Bearer $KEY" -F "file=@chart.png;type=image/png"
```

在 Markdown 中使用返回的 `url`:`![](/assets/xxx.png)`。

### 4.5 校验与限制

| 项 | 规则 | 失败状态码 |
| --- | --- | --- |
| 文档扩展名 | `.md` / `.markdown`(不区分大小写) | 400 |
| 文档大小 | ≤ 5 MB(业务层校验) | 413 |
| 文档编码 | 严格 UTF-8,否则拒绝 | 400 |
| 图片扩展名 | png / jpg / jpeg / gif / webp / svg | 400 |
| 图片大小 | ≤ 10 MB | 413 |
| 图片 MIME | 必须与扩展名一致(jpg/jpeg → `image/jpeg`,svg → `image/svg+xml`,其余 `image/{ext}`) | 400 |
| 文件名 | 去掉路径部分,长度 ≤ 255,不能为空/`.`/`..` | 400 |
| 请求体 | multipart 单文件 10 MB,整个请求 11 MB | 413 |

SVG 通过 `/assets/*.svg` 访问时附带 `Content-Security-Policy: script-src 'none'`。

### 4.6 错误格式

由 `GlobalExceptionHandler` 统一处理 `BusinessException`、`MaxUploadSizeExceededException` 和兜底 `Exception`(500,`Internal server error`)。

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Document with filename '2026-10-14-daily.md' already exists",
  "path": "/api/docs",
  "timestamp": "2026-10-14T21:00:00+09:00"
}
```

状态码:400 参数/校验、401 未鉴权、404 不存在、409 同名冲突、413 过大、500 内部错误。

### 4.7 搜索

`DocumentRepository.search`:对 `title` 与 `content` 做不区分大小写的 `LIKE '%kw%'`(`escape '\'`),关键词中的 `\`、`%`、`_` 已转义;结果按 `uploadedAt` 倒序。适用于数千篇以内。

## 5. 鉴权

- 单个固定 API Key,来自 `DAILYMARK_API_KEY`;请求头 `Authorization: Bearer <key>`。
- `ApiKeyFilter`(`OncePerRequestFilter`)仅对 **URI 以 `/api/` 开头且方法不是 GET/HEAD/OPTIONS** 的请求校验。
- 比较使用 `MessageDigest.isEqual`(常量时间)。
- 通过后设置 `ROLE_UPLOADER` 认证;失败直接返回 `401` JSON。
- `SecurityConfig`:关闭 CSRF、`STATELESS`,所有请求 `permitAll`,鉴权完全由过滤器完成。
- 启动校验(`StorageProperties#validate`):Key 为空或长度 < 32 时抛出 `IllegalStateException`,应用启动失败;同时创建 `data`、`docs`、`assets`、`db` 目录。
- 生成 Key:`openssl rand -hex 32`。

## 6. 配置项

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DAILYMARK_API_KEY` | 无(必填,≥32 字符) | 写接口 Key |
| `DAILYMARK_DATA_DIR` | `${user.dir}/data`(Docker 中通过挂载使用 `/data`,见下) | 数据根目录 |
| `SERVER_PORT` | `8080` | 服务端口 |
| `TZ` | compose 中为 `Asia/Tokyo` | 容器时区 |
| `JAVA_OPTS` | `-Xms128m -Xmx512m`(Dockerfile) | JVM 参数 |

注意:`application.yml` 的默认数据目录是 `${user.dir}/data`;Docker 镜像工作目录为 `/app`,因此容器内默认会落在 `/app/data` 而不是挂载的 `/data`,**部署时需显式设置 `DAILYMARK_DATA_DIR=/data`**(见待办)。

`.env.example`:

```text
DAILYMARK_API_KEY=replace-with-at-least-32-random-characters
SERVER_PORT=8080
DAILYMARK_DATA_DIR=./data
```

其他关键配置:`spring.jpa.hibernate.ddl-auto=update`、`open-in-view=false`、`forward-headers-strategy=framework`、Actuator 仅暴露 `health`。

## 7. 开发、构建、测试、部署

### 本地运行

```bash
cd backend
# PowerShell: $env:DAILYMARK_API_KEY="<至少32位>"
export DAILYMARK_API_KEY=$(openssl rand -hex 32)
mvn spring-boot:run
```

前端开发时由 Vite 将 `/api`、`/assets` 代理到 `http://localhost:8080`。

### 测试与打包

```bash
mvn test            # 目前仅有 DocumentServiceTest
mvn package -DskipTests
```

### Docker

```bash
cp .env.example .env     # 编辑 DAILYMARK_API_KEY
mkdir -p data && sudo chown 1001 data   # 容器用户 UID 1001
docker compose up -d --build
```

Dockerfile 为三阶段:`node:20-alpine` 构建前端 → `maven:3.9-eclipse-temurin-17` 构建后端并把 `dist` 放入 `static` → `eclipse-temurin:17-jre` 运行(用户 `dailymark`,`VOLUME /data`,端口 8080)。`docker-compose.yml` 挂载 `./data:/data`,并要求设置 `DAILYMARK_API_KEY`。

生产环境应置于 Nginx/Caddy/Traefik 之后终止 TLS;备份只需备份数据目录。

## 8. 已知问题与待办(与设计稿差异)

1. **Docker 数据目录**:compose 未设置 `DAILYMARK_DATA_DIR`,默认 `${user.dir}/data`(容器内 `/app/data`)可能不在挂载卷内,且 `/app` 对 `dailymark` 用户可能不可写。建议在 compose 中设置 `DAILYMARK_DATA_DIR: /data`,或把默认值改为 `/data`。
2. 401 响应体为手写 JSON,缺少 `path`、`timestamp`,与统一错误格式不一致。
3. `SecurityConfig` 全部 `permitAll`;设计稿中的 `hasRole("UPLOADER")` 与 `authenticationEntryPoint` 未实现。
4. `SpaForwardController` 仅转发 `/`、`/doc/**`;其他未知路径返回后端 404,前端 404 页在生产环境不可达(设计稿为“非 `/api` 等前缀且不含 `.` 的路径都回退”)。
5. `spring.web.resources.static-locations` 额外包含 assets 目录,与 `/assets/**` 处理器重复,可能暴露 `/{filename}` 路径下的图片。
6. 删除文档/图片时磁盘文件删除失败被静默忽略(`IOException` 被吞掉);先删库后删文件。
7. 上传/覆盖的写入顺序是先写文件、再写库;库操作失败时可能残留文件。覆盖时旧文件被直接替换,事务回滚无法恢复。
8. 设计稿要求的上传/删除操作日志、鉴权失败日志(来源 IP + 路径)、`X-Frame-Options` 等响应头未专门配置(依赖 Spring Security 默认)。
9. `Dockerfile` 无 `HEALTHCHECK`;前端阶段用 `npm install` 而非 `npm ci`。
10. `/api/docs/latest` 通过加载全部文档(含 `content`)再取第一个,文档多时效率低;列表接口同样加载完整实体。
11. 设计稿中 `uk_document_filename`、`idx_document_uploaded_at (DESC)` 的命名/降序未完全对应。
12. 测试覆盖仅 `DocumentServiceTest` 一例,缺少 `AssetService`、鉴权过滤器、控制器与覆盖/冲突场景的测试。
