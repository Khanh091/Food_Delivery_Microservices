import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'node:path'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Keep the frontend's existing Keycloak/API .env while loading only the public tile key from project root.
  const rootEnv = loadEnv(mode, resolve(process.cwd(), '../..'), 'VITE_')
  return {
    plugins: [react()],
    define: {
      'import.meta.env.VITE_VIETMAP_TILEMAP_KEY': JSON.stringify(rootEnv.VITE_VIETMAP_TILEMAP_KEY ?? ''),
    },
  }
})
