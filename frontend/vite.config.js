import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    // Le backend n'autorise (CORS) que ce port : on refuse de demarrer ailleurs
    // plutot que de basculer en silence sur 5176 et de ne plus voir aucune donnee.
    strictPort: true
  }
})
