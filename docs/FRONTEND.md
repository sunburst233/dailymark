# DailyMark 前端文档

> 本文档以**当前代码的实际行为**为准。与 [DESIGN.md](DESIGN.md) 的差异见文末“已知问题与待办”。后端见 [BACKEND.md](BACKEND.md)。

## 1. 概述与技术栈

前端是一个只读的 React 单页应用,用于浏览后端提供的 Markdown 文档;不包含上传、编辑、登录功能。

| 用途 | 选型 |
| --- | --- |
| 框架 / 构建 | React 18 + Vite 5 + TypeScript 5 |
| 路由 | React Router v6 |
| Markdown | `react-markdown` 9 + `remark-gfm` + `remark-math` |
| 后处理 | `rehype-sanitize`、`rehype-katex`、`rehype-highlight`、`rehype-slug` |
| 样式 | `github-markdown-css`(light)、`katex` CSS、`highlight.js` github 主题、`styles.css` |
| 测试 | Vitest |

## 2. 目录与组件

```text
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.tsx        # 入口,挂载 BrowserRouter
    ├── App.tsx         # 路由 + 所有组件
    ├── api.ts          # fetch 封装与类型
    ├── styles.css
    ├── App.test.ts     # dateKey 测试
    └── vite-env.d.ts
```

`App.tsx` 内的组件:

| 组件 | 职责 |
| --- | --- |
| `App` | 路由:`/`、`/doc/:id` 渲染 `Reader`,`*` 渲染 404 |
| `Reader` | 加载列表与当前文档,管理搜索词、抽屉状态,组合下列组件 |
| `Sidebar` | 站点名、搜索框、按日期分组的文档列表,当前文档高亮 |
| `DocView` | 加载骨架 / 错误 / 空状态 / 标题与时间 / 正文 |
| `MarkdownRenderer` | Markdown 渲染 |
| `Toc` | 右侧目录(由正文 h1–h3 提取) |

导出的工具函数:`dateKey(date)`(按浏览器本地日期输出 `YYYY-MM-DD`,用于分组和测试)。

## 3. 路由与数据流

| 路由 | 行为 |
| --- | --- |
| `/` | 请求 `GET /api/docs/latest`,展示最新文档;无文档时显示“还没有文档” |
| `/doc/:id` | 请求 `GET /api/docs/{id}`;404 时显示“文档不存在”,其他失败显示“请求失败,请稍后重试” |
| 其他 | 前端 404 页(生产环境见待办 1) |

`api.ts`:

```ts
api.list(query?)  // GET /api/docs[?q=...]  -> DocSummary[]
api.latest()      // GET /api/docs/latest   -> DocDetail
api.get(id)       // GET /api/docs/{id}     -> DocDetail
```

类型:`DocSummary { id, slug, title, filename, size, uploadedAt, updatedAt }`,`DocDetail = DocSummary & { content }`。只使用 GET,不携带 API Key。

数据流:

- 首次进入时加载一次完整列表。
- 搜索框输入后防抖 300 ms 再请求 `api.list(query)`,结果直接替换侧边栏列表。
- 路由 `id` 或 `pathname` 变化时重新加载当前文档。
- 列表按 `uploadedAt` 的本地日期分组,组内保持后端返回的倒序。
- 页面不做实时推送,上传后需刷新页面。

## 4. Markdown 渲染

插件顺序:

```text
remarkGfm → remarkMath → rehypeSanitize(默认 schema) → rehypeKatex → rehypeHighlight → rehypeSlug
```

- 支持 GFM(表格、任务列表、删除线)、KaTeX 公式、代码高亮、标题锚点。
- `rehypeSanitize` 位于 katex/highlight 之前,只过滤原始内容。
- 链接:以 `http` 开头的外链加 `target="_blank"` 和 `rel="noopener noreferrer"`。
- 图片:原样使用 Markdown 中的地址。推荐先调用 `POST /api/assets`,再在文档里写 `![](/assets/xxx.png)`。
- 目录:`Toc` 用手写的 `headings()` 逐行匹配 `^#{1,3}\s+...`,生成 id 并作为 `#id` 锚点;点击使用浏览器原生锚点跳转。

## 5. UI 行为

- 布局:左侧边栏 + 主内容区 + 右侧目录(`.app-shell`)。
- 加载中显示骨架屏(`.skeleton`);错误显示错误信息和“返回首页”链接。
- 文档头部显示标题、上传时间、更新时间(`zh-CN` 格式)。
- 移动端:菜单按钮 `☰` 打开侧边栏抽屉;点击遮罩、关闭按钮、选择文档或按 `Esc` 关闭;抽屉打开时锁定 `body` 滚动。具体断点见 `styles.css`。
- 侧边栏无结果时显示“没有找到文档”。

## 6. 开发、构建、测试

```bash
cd frontend
npm install
npm run dev      # Vite 开发服务器,/api 和 /assets 代理到 http://localhost:8080
npm run build    # tsc -b && vite build,产物在 dist/
npm test         # vitest run
```

开发时需要先启动后端(见 BACKEND.md)。生产构建时,`Dockerfile` 会把 `frontend/dist` 复制到后端 `src/main/resources/static`,由 Spring Boot 一并提供。

## 7. 已知问题与待办(与设计稿差异)

1. **生产环境 404 页不可达**:后端 SPA 回退只转发 `/` 与 `/doc/**`,其他未知路径由后端返回 404。
2. 未按设计稿拆分 `components/` 目录,所有组件都在 `App.tsx`;样式为单个 `styles.css` 而非 `styles/` 目录。
3. 未实现图片相对路径改写(`urlTransform`),相对路径图片不会指向 `/assets/`。
4. 未配置自定义 `rehype-sanitize` schema,使用默认 schema。
5. 目录无滚动时当前章节高亮;设计稿要求高亮。
6. 目录 id 由手写 slug 生成,而正文标题 id 由 `rehype-slug`(github-slugger 规则)生成,重复标题、特殊字符、中英混排时二者可能不一致,导致跳转失效。
7. 首次列表加载失败被静默忽略(设为空数组),用户只会看到“没有找到文档”。
8. 日期分组使用浏览器本地时区,设计稿说明以服务器时区为准,不同用户看到的分组可能不同。
9. `Reader` 中隐藏的“首页”按钮(`.hidden`)是冗余代码。
10. 当文档加载出错时仍会请求并展示列表,但没有重试入口;搜索请求失败同样被忽略。
11. `Reader` 在 `/` 与 `/doc/:id` 两条路由下重复挂载,路由切换会重置搜索词、重新加载列表。
12. 测试覆盖仅 `dateKey`,缺少渲染、路由、搜索防抖的测试。
