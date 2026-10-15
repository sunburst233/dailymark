import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '..', '')
  const basePath = (env.VITE_BASE_PATH || env.DAILYMARK_BASE_PATH || '').replace(/\/$/, '')
  const prefix = basePath || ''
  return {
    plugins: [react()],
    envDir: '..',
    base: `${prefix}/` || '/',
    server: {
      proxy: {
        [`${prefix}/api` || '/api']: 'http://localhost:8080',
        [`${prefix}/assets` || '/assets']: 'http://localhost:8080'
      }
    }
  }
})
