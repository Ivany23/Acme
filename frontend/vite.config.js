import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em desenvolvimento, /api é encaminhado para o backend local
    proxy: {
      '/api': process.env.API_LOCAL || 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./testes/setup.js'],
    include: ['testes/**/*.test.{js,jsx}'],
    restoreMocks: true,
  },
})
