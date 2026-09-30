import process from 'node:process'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const backendUrl =
  process.env.CONTRACT_API_URL ?? 'http://127.0.0.1:8081'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: backendUrl,
        changeOrigin: true,
      },
    },
  },
})