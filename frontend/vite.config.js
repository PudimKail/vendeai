import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Em desenvolvimento (npm run dev, porta 5173) as chamadas /api vão para o Spring Boot na 8081.
// No build, o React é gerado dentro do Spring Boot, que serve tudo em um só lugar.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8081',
      '/uploads': 'http://localhost:8081',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
})
