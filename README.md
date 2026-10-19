# DailyMark

DailyMark 是一个轻量的 Markdown 文档展示站点。文档通过带 API Key 鉴权的接口上传，浏览者无需登录即可在网页中阅读、搜索和查看图片。

## 功能

- Markdown 文档公开浏览，首页默认展示最新文档
- 按上传时间分组的文档列表和标题/正文搜索
- 支持 GFM、代码高亮、数学公式、标题目录
- 通过 API 上传、覆盖、替换和删除 Markdown 文档
- 上传 Markdown 中使用的图片，并通过公开 URL 引用
- 写操作使用 Bearer API Key 鉴权，读操作公开
- H2 文件数据库和本地文件存储，适合个人或小规模文档库

前端目前是只读阅读器，不提供网页端上传、编辑或登录功能。

## 技术栈

- 前端：React 18、Vite、TypeScript、React Router、react-markdown
- 后端：Java 17、Spring Boot 3.3、Spring MVC、Spring Data JPA、H2
- 安全：Spring Security、自定义 API Key 过滤器
- 部署：Docker Compose，单个 Spring Boot 进程同时提供 API 和前端静态资源

## 快速开始

### Docker Compose 部署

1. 生成一个至少 32 个字符的 API Key：

   ```bash
   openssl rand -hex 32
   ```

2. 在项目根目录创建 `.env`：

   ```dotenv
   DAILYMARK_API_KEY=<生成的 API Key>
   SERVER_PORT=8080
   ```

3. 准备数据目录并启动：

   ```bash
   mkdir data
   docker compose up -d --build
   ```

   容器以 UID `1001` 的 `dailymark` 用户运行，Linux 环境需要确保 `data` 目录及其内容可被该用户读写：

   ```bash
   sudo chown -R 1001:1001 data
   ```

4. 打开 <http://localhost:8080>，健康检查地址为 <http://localhost:8080/actuator/health>。

Docker Compose 会将 `DAILYMARK_DATA_DIR` 设置为 `/data`，并把本地 `./data` 挂载到容器中的 `/data`。数据库、Markdown 文件和图片都会保存在该目录中。

### 本地开发

先启动后端：

```bash
cd backend

# PowerShell
$env:DAILYMARK_API_KEY = "<至少 32 个字符的 API Key>"

mvn spring-boot:run
```

再启动前端开发服务器：

```bash
cd frontend
npm install
npm run dev
```

Vite 会将 `/api` 和 `/assets` 请求代理到 `http://localhost:8080`。

## 常用命令

```bash
# 前端
cd frontend
npm run build
npm test

# 后端
cd backend
mvn test
mvn package -DskipTests
```

## API 示例

写操作需要携带：

```http
Authorization: Bearer <DAILYMARK_API_KEY>
```

设置变量后，可以使用以下命令：

```bash
HOST=http://localhost:8080
KEY=<your-api-key>

# 查询文档列表（无需鉴权）
curl "$HOST/api/docs"

# 搜索文档（无需鉴权）
curl "$HOST/api/docs?q=日报"

# 上传 Markdown 文档
curl -X POST "$HOST/api/docs" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@2026-10-14-daily.md"

# 上传图片
curl -X POST "$HOST/api/assets" \
  -H "Authorization: Bearer $KEY" \
  -F "file=@chart.png;type=image/png"
```

上传图片后，将响应中的 `url` 写入 Markdown，例如：

```markdown
![图表](/assets/<asset-id>.png)
```

完整接口、请求参数、响应结构和错误码见 [API 文档](docs/API.md)。

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DAILYMARK_API_KEY` | 无，必填 | 写接口使用的 API Key，至少 32 个字符 |
| `DAILYMARK_DATA_DIR` | `${user.dir}/data` | 数据根目录；Docker Compose 中为 `/data` |
| `SERVER_PORT` | `8080` | 服务端口 |
| `TZ` | 系统时区 | Docker Compose 中为 `Asia/Tokyo` |
| `JAVA_OPTS` | `-Xms128m -Xmx512m` | Docker 容器中的 JVM 参数 |

数据目录结构：

```text
data/
├── db/       # H2 数据库
├── docs/     # Markdown 文件
└── assets/   # 图片文件
```

备份整个 `data` 目录即可备份应用数据。生产环境建议在 Nginx、Caddy 或 Traefik 后运行，并通过 HTTPS 传输 API Key。

## 项目结构

```text
DailyMark/
├── frontend/             # React 阅读器和 Vite 配置
├── backend/              # Spring Boot API、存储和鉴权
├── docs/
│   ├── API.md            # HTTP API 说明
│   ├── BACKEND.md        # 后端实现、配置和部署
│   ├── FRONTEND.md       # 前端实现和开发说明
│   └── DESIGN.md         # 项目设计文档
├── Dockerfile
├── docker-compose.yml
└── .env.example
```

## 文档

- [API 文档](docs/API.md)
- [后端文档](docs/BACKEND.md)
- [前端文档](docs/FRONTEND.md)
- [设计文档](docs/DESIGN.md)

