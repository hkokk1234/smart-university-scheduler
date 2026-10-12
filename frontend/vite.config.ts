import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig(({ mode }) => {
  const envDir =fileURLToPath(new URL('..', import.meta.url))
  const env ={ ...loadEnv(mode, envDir, ''), ...process.env }

  return {
    plugins: [react(), tailwindcss()],
    envDir,
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: {
      host: '127.0.0.1',
      port: Number(env.FRONTEND_PORT || 5173),
      strictPort: true,
      proxy: {
        '/actuator': { target: env.BACKEND_PROXY_TARGET || 'http://localhost:8080' },
        '/api': { target: env.BACKEND_PROXY_TARGET || 'http://localhost:8080' },
      },
    },
    preview: { host: '127.0.0.1', port: 4173, strictPort: true },
  }
})
