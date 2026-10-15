import { useEffect, useMemo, useState } from 'react'
import { Link, Route, Routes, useLocation, useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import remarkMath from 'remark-math'
import rehypeHighlight from 'rehype-highlight'
import rehypeKatex from 'rehype-katex'
import rehypeSanitize from 'rehype-sanitize'
import rehypeSlug from 'rehype-slug'
import 'github-markdown-css/github-markdown-light.css'
import 'katex/dist/katex.min.css'
import 'highlight.js/styles/github.css'
import { api, DocDetail, DocSummary } from './api'

export const dateKey = (date: string) => new Intl.DateTimeFormat('sv-SE').format(new Date(date))
const formatDate = (date: string) => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date))
const headings = (content: string) => content.split('\n').flatMap(line => {
  const match = /^(#{1,3})\s+(.+?)\s*#*\s*$/.exec(line)
  return match ? [{ level: match[1].length, text: match[2], id: match[2].toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '') }] : []
})

function Sidebar({ docs, open, onClose, query, setQuery }: { docs: DocSummary[], open: boolean, onClose: () => void, query: string, setQuery: (v: string) => void }) {
  const location = useLocation()
  const groups = useMemo(() => docs.reduce<Record<string, DocSummary[]>>((all, doc) => {
    const key = dateKey(doc.uploadedAt); (all[key] ??= []).push(doc); return all
  }, {}), [docs])
  return <aside className={`sidebar ${open ? 'is-open' : ''}`}>
    <div className="brand"><Link to="/" onClick={onClose}>DailyMark</Link><button className="close-button" onClick={onClose} aria-label="关闭菜单">×</button></div>
    <label className="search"><span className="sr-only">搜索文档</span><input value={query} onChange={e => setQuery(e.target.value)} placeholder="搜索文档..." /></label>
    <nav className="doc-list" aria-label="文档列表">
      {Object.entries(groups).map(([date, items]) => <section key={date}><h2>{date}</h2>{items.map(doc =>
        <Link onClick={onClose} className={location.pathname === `/doc/${doc.id}` ? 'active' : ''} key={doc.id} to={`/doc/${doc.id}`}>{doc.title}</Link>)}</section>)}
      {!docs.length && <p className="muted">没有找到文档</p>}
    </nav>
  </aside>
}

function Toc({ content }: { content: string }) {
  const items = headings(content)
  if (!items.length) return null
  return <aside className="toc"><h2>目录</h2><nav>{items.map(item =>
    <a className={`toc-level-${item.level}`} key={`${item.id}-${item.text}`} href={`#${item.id}`}>{item.text}</a>)}</nav></aside>
}

function MarkdownRenderer({ content }: { content: string }) {
  return <div className="markdown-body">
    <ReactMarkdown remarkPlugins={[remarkGfm, remarkMath]} rehypePlugins={[rehypeSanitize, rehypeKatex, rehypeHighlight, rehypeSlug]}
      urlTransform={url => url.startsWith('/assets/') ? `${import.meta.env.BASE_URL}${url.slice('/'.length)}` : url}
      components={{ a: ({ href, children, ...props }) => <a href={href} target={href?.startsWith('http') ? '_blank' : undefined} rel={href?.startsWith('http') ? 'noopener noreferrer' : undefined} {...props}>{children}</a> }}>
      {content}
    </ReactMarkdown>
  </div>
}

function DocView({ doc, loading, error }: { doc?: DocDetail, loading: boolean, error?: string }) {
  if (loading) return <main className="content"><div className="skeleton" /></main>
  if (error) return <main className="content empty"><h1>{error}</h1><Link to="/">返回首页</Link></main>
  if (!doc) return <main className="content empty"><h1>还没有文档</h1><p>通过 API 上传第一篇 Markdown 文档后，它会显示在这里。</p></main>
  return <main className="content"><header className="doc-header"><h1>{doc.title}</h1><p>上传于 {formatDate(doc.uploadedAt)} · 更新于 {formatDate(doc.updatedAt)}</p></header><MarkdownRenderer content={doc.content} /></main>
}

function Reader() {
  const [docs, setDocs] = useState<DocSummary[]>([])
  const [doc, setDoc] = useState<DocDetail>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string>()
  const [query, setQuery] = useState('')
  const [drawer, setDrawer] = useState(false)
  const location = useLocation()
  const params = useParams()
  const navigate = useNavigate()
  useEffect(() => { api.list().then(setDocs).catch(() => setDocs([])) }, [])
  useEffect(() => {
    setLoading(true); setError(undefined)
    const load = params.id ? api.get(params.id) : api.latest()
    load.then(setDoc).catch(e => { setDoc(undefined); setError(e.message) }).finally(() => setLoading(false))
  }, [params.id, location.pathname])
  useEffect(() => {
    const timer = window.setTimeout(() => api.list(query).then(setDocs).catch(() => undefined), 300)
    return () => window.clearTimeout(timer)
  }, [query])
  useEffect(() => { if (!drawer) return; const close = (e: KeyboardEvent) => e.key === 'Escape' && setDrawer(false); document.addEventListener('keydown', close); document.body.style.overflow = 'hidden'; return () => { document.removeEventListener('keydown', close); document.body.style.overflow = '' } }, [drawer])
  const current = doc?.id
  return <div className="app-shell">
    <button className="menu-button" aria-expanded={drawer} aria-label="打开菜单" onClick={() => setDrawer(true)}>☰</button>
    <div className={`drawer-backdrop ${drawer ? 'visible' : ''}`} onClick={() => setDrawer(false)} />
    <Sidebar docs={docs} open={drawer} onClose={() => setDrawer(false)} query={query} setQuery={setQuery} />
    <DocView doc={doc} loading={loading} error={error} />
    {current && <Toc content={doc?.content ?? ''} />}
    {!loading && !error && !doc && <button className="hidden" onClick={() => navigate('/')}>首页</button>}
  </div>
}

export default function App() {
  return <Routes><Route path="/" element={<Reader />} /><Route path="/doc/:id" element={<Reader />} /><Route path="*" element={<main className="not-found"><h1>404</h1><Link to="/">返回首页</Link></main>} /></Routes>
}
