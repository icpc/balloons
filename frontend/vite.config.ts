/// <reference types="node" />

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.BACKEND_URL ?? 'http://localhost:8001',
        changeOrigin: true,
      },
      '/api/balloons': {
        target: process.env.BACKEND_URL ?? 'ws://localhost:8001',
        ws: true,
      },
    },
  },
});
