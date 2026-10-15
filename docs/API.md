# DailyMark API 使用文档

本文档描述 DailyMark 当前实现提供的 HTTP API。示例使用 `curl`，响应 JSON 字段采用 camelCase。

## 1. 基本信息

- Base URL：`http://localhost:8080`
- API 前缀：`/api`
- 字符编码：请求上传的 Markdown 文件必须是合法 UTF-8
- 时间格式：ISO-8601，例如 `2026-10-14T12:00:00Z`
- 文档和图片的读取接口公开；上传、替换和删除接口需要 API Key

设置示例变量：

```bash
HOST=http://localhost:8080
KEY=<your-api-key>
```

## 2. 鉴权

所有写操作都必须携带：

```http
Authorization: Bearer <DAILYMARK_API_KEY>
```

API Key 由服务端环境变量 `DAILYMARK_API_KEY` 配置，长度必须至少为 32 个字符。读取接口不需要鉴权。

鉴权失败：

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json
```

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentication required"
}
```

## 3. 接口总览

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/docs` | 否 | 查询文档列表，可搜索 |
| `GET` | `/api/docs/latest` | 否 | 获取最新文档及正文 |
| `GET` | `/api/docs/{id}` | 否 | 获取指定文档及正文 |
| `POST` | `/api/docs` | 是 | 上传文档，可覆盖同名文档 |
| `PUT` | `/api/docs/{id}` | 是 | 替换指定文档的正文 |
| `DELETE` | `/api/docs/{id}` | 是 | 删除文档 |
| `POST` | `/api/assets` | 是 | 上传图片 |
| `DELETE` | `/api/assets/{id}` | 是 | 删除图片 |
| `GET` | `/assets/{filename}` | 否 | 访问已上传图片 |
| `GET` | `/actuator/health` | 否 | 健康检查 |

`{id}` 是 UUID。所有成功的 JSON 响应均使用 `Content-Type: application/json`。

## 4. 文档接口

### 4.1 查询文档列表

```http
GET /api/docs
GET /api/docs?q={keyword}
```

`q` 可选；非空时按标题和正文进行不区分大小写的包含搜索。结果按 `uploadedAt` 倒序返回。接口不返回正文。

```bash
curl "$HOST/api/docs"
curl "$HOST/api/docs?q=日报"
```

响应：`200 OK`

```json
[
  {
    "id": "3f1c2a7e-4f7a-4b5e-8a0e-123456789abc",
    "slug": "2026-10-14-daily",
    "title": "10 月 14 日日报",
    "filename": "2026-10-14-daily.md",
    "size": 2048,
    "uploadedAt": "2026-10-14T12:00:00Z",
    "updatedAt": "2026-10-14T12:00:00Z"
  }
]
```

### 4.2 获取最新文档

```http
GET /api/docs/latest
```

```bash
curl "$HOST/api/docs/latest"
```

响应：`200 OK`，响应对象包含文档元数据和 `content` 正文：

```json
{
  "id": "3f1c2a7e-4f7a-4b5e-8a0e-123456789abc",
  "slug": "2026-10-14-daily",
  "title": "10 月 14 日日报",
  "filename": "2026-10-14-daily.md",
  "size": 2048,
  "uploadedAt": "2026-10-14T12:00:00Z",
  "updatedAt": "2026-10-14T12:00:00Z",
  "content": "# 10 月 14 日日报\n\n正文内容"
}
```

没有任何文档时响应 `404 Not Found`。

### 4.3 获取指定文档

```http
GET /api/docs/{id}
```

```bash
curl "$HOST/api/docs/3f1c2a7e-4f7a-4b5e-8a0e-123456789abc"
```

响应结构与“获取最新文档”相同。文档不存在时响应 `404 Not Found`。

### 4.4 上传文档

```http
POST /api/docs
POST /api/docs?overwrite=true
Content-Type: multipart/form-data
```

请求必须包含名为 `file` 的 multipart 文件字段。支持的扩展名为 `.md` 和 `.markdown`，不区分大小写。

```bash
# 新建文档
curl -X POST "$HOST/api/docs" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@2026-10-14-daily.md"

# 同名时允许覆盖
curl -X POST "$HOST/api/docs?overwrite=true" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@2026-10-14-daily.md"
```

处理规则：

| 情况 | 状态码 | 说明 |
| --- | --- | --- |
| 文件名不存在 | `201 Created` | 创建新文档 |
| 文件名已存在，未设置 `overwrite=true` | `409 Conflict` | 不覆盖已有文档 |
| 文件名已存在，且 `overwrite=true` | `200 OK` | 覆盖已有文档，保留原 `id` 和 `uploadedAt` |

成功响应为 `DocSummary`，不包含正文。

文件名会去除客户端提交的路径部分；同名判断使用处理后的文件名。文档标题取正文中第一个一级标题（`# 标题`），没有一级标题时使用去掉扩展名的文件名。

### 4.5 替换指定文档

```http
PUT /api/docs/{id}
Content-Type: multipart/form-data
```

```bash
curl -X PUT "$HOST/api/docs/3f1c2a7e-4f7a-4b5e-8a0e-123456789abc" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@updated-content.md"
```

响应：`200 OK`，返回 `DocSummary`。

替换只更新正文、标题、大小和更新时间；文档的原始 `filename`、`id` 和 `uploadedAt` 保持不变。新上传文件的文件名不会成为该文档的新文件名。文档不存在时响应 `404 Not Found`。

### 4.6 删除文档

```http
DELETE /api/docs/{id}
```

```bash
curl -X DELETE "$HOST/api/docs/3f1c2a7e-4f7a-4b5e-8a0e-123456789abc" \
  -H "Authorization: Bearer $KEY"
```

成功响应：`204 No Content`，无响应正文。文档不存在时响应 `404 Not Found`。

## 5. 图片接口

### 5.1 上传图片

```http
POST /api/assets
Content-Type: multipart/form-data
```

请求字段必须为 `file`。支持 `png`、`jpg`、`jpeg`、`gif`、`webp` 和 `svg`，并且 MIME 类型必须与扩展名匹配。

```bash
curl -X POST "$HOST/api/assets" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@chart.png;type=image/png"
```

响应：`200 OK`

```json
{
  "id": "a9e2b7d3-5c61-4c9f-9d23-123456789abc",
  "url": "/assets/a9e2b7d3-5c61-4c9f-9d23-123456789abc.png",
  "originalName": "chart.png",
  "size": 102400
}
```

建议在 Markdown 中直接使用响应中的 `url`：

```markdown
![图表](/assets/a9e2b7d3-5c61-4c9f-9d23-123456789abc.png)
```

图片 URL 公开可访问，并设置 1 小时缓存。访问 SVG 时会附加 `Content-Security-Policy: script-src 'none'`。

### 5.2 删除图片

```http
DELETE /api/assets/{id}
```

```bash
curl -X DELETE "$HOST/api/assets/a9e2b7d3-5c61-4c9f-9d23-123456789abc" \
  -H "Authorization: Bearer $KEY"
```

成功响应：`204 No Content`。图片不存在时响应 `404 Not Found`。

## 6. 健康检查

```http
GET /actuator/health
```

```bash
curl "$HOST/actuator/health"
```

服务正常时返回 `200 OK`，通常响应：

```json
{
  "status": "UP"
}
```

## 7. 文件限制

| 类型 | 限制 |
| --- | --- |
| Markdown 扩展名 | `.md`、`.markdown` |
| Markdown 大小 | 不超过 5 MiB |
| Markdown 编码 | 合法 UTF-8 |
| 图片扩展名 | `.png`、`.jpg`、`.jpeg`、`.gif`、`.webp`、`.svg` |
| 图片大小 | 不超过 10 MiB |
| 文件名 | 非空，去除路径后不超过 255 个字符，不能为 `.` 或 `..` |
| 单文件 multipart 限制 | 10 MB |
| 单次请求 multipart 限制 | 11 MB |

## 8. 错误响应

除鉴权失败外，业务异常统一返回以下结构：

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Document with filename '2026-10-14-daily.md' already exists",
  "path": "/api/docs",
  "timestamp": "2026-10-14T12:00:00Z"
}
```

常见状态码：

| 状态码 | 含义 | 常见原因 |
| --- | --- | --- |
| `400` | Bad Request | 文件缺失、扩展名不支持、编码无效、MIME 类型不匹配 |
| `401` | Unauthorized | 写操作缺少或携带了错误的 API Key |
| `404` | Not Found | 文档或图片不存在，或没有任何文档可供获取 |
| `409` | Conflict | 上传同名文档且未设置 `overwrite=true` |
| `413` | Payload Too Large | 文件超过业务限制或 multipart 请求限制 |
| `500` | Internal Server Error | 未预期的服务端错误 |

## 9. 完整调用流程示例

```bash
# 1. 上传图片
curl -X POST "$HOST/api/assets" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@chart.png;type=image/png"

# 2. 在 Markdown 中引用返回的图片 URL，并上传文档
curl -X POST "$HOST/api/docs" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@2026-10-14-daily.md"

# 3. 查询文档列表
curl "$HOST/api/docs"

# 4. 获取指定文档
curl "$HOST/api/docs/<document-id>"

# 5. 删除文档
curl -X DELETE "$HOST/api/docs/<document-id>" \
  -H "Authorization: Bearer $KEY"
```
