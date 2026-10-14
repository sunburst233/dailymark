export type DocSummary = {
  id: string; slug: string; title: string; filename: string; size: number
  uploadedAt: string; updatedAt: string
}
export type DocDetail = DocSummary & { content: string }

async function request<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (!response.ok) throw new Error(response.status === 404 ? '文档不存在' : '请求失败，请稍后重试')
  return response.json() as Promise<T>
}
export const api = {
  list: (query = '') => request<DocSummary[]>(`/api/docs${query ? `?q=${encodeURIComponent(query)}` : ''}`),
  latest: () => request<DocDetail>('/api/docs/latest'),
  get: (id: string) => request<DocDetail>(`/api/docs/${id}`)
}
