import http from 'node:http'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import express from 'express'
import { WebSocketServer } from 'ws'
import { createApiRouter, createMockStore } from './server/mockApi.ts'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const PORT = Number(process.env.PORT || 3000)
const HOST = '0.0.0.0'

async function startServer() {
  const app = express()
  app.use(express.json({ limit: '10mb' }))
  app.use(express.urlencoded({ extended: true }))

  const store = createMockStore()
  app.use('/api', createApiRouter(store))

  const distDir = path.resolve(__dirname, 'xueyifang-frontend/dist')
  const indexHtmlPath = path.join(distDir, 'index.html')

  if (fs.existsSync(indexHtmlPath)) {
    app.use(express.static(distDir, { index: false }))
    app.get('*', (req, res, next) => {
      if (req.path.startsWith('/api')) return next()
      res.sendFile(indexHtmlPath)
    })
  } else {
    // Fallback to Vite dev middleware if dist has not been compiled yet
    const { createServer: createViteServer } = await import('vite')
    const frontendRoot = path.resolve(__dirname, 'xueyifang-frontend')
    const vite = await createViteServer({
      root: frontendRoot,
      configFile: path.join(frontendRoot, 'vite.config.ts'),
      server: {
        middlewareMode: true,
        hmr: false
      },
      appType: 'spa'
    })
    app.use(vite.middlewares)
  }

  const httpServer = http.createServer(app)

  // Multiplex WebSocket on the same port 3000 at /api/ws for chat & notification heartbeats
  const wss = new WebSocketServer({ server: httpServer, path: '/api/ws' })
  wss.on('connection', (ws) => {
    ws.send(JSON.stringify({ type: 'CONNECTED', data: { connectedAt: Date.now() } }))
    ws.on('message', (raw) => {
      try {
        const msg = JSON.parse(String(raw))
        if (msg?.type === 'PING') {
          ws.send(JSON.stringify({ type: 'PONG', data: { timestamp: Date.now() } }))
        }
      } catch {
        // Ignore malformed frames
      }
    })
  })

  httpServer.listen(PORT, HOST, () => {
    console.log(`[Xueyifang] Server listening on http://${HOST}:${PORT}`)
  })
}

startServer().catch((err) => {
  console.error('[Xueyifang] Failed to start server:', err)
  process.exit(1)
})
