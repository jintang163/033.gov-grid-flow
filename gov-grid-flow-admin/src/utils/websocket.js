export class DashboardWebSocket {
  constructor(url, options = {}) {
    this.url = url
    this.options = options
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = options.maxReconnectAttempts || 10
    this.reconnectInterval = options.reconnectInterval || 5000
    this.heartbeatInterval = options.heartbeatInterval || 30000
    this.heartbeatTimer = null
    this.reconnectTimer = null
    this.isManualClose = false
    this.onMessage = options.onMessage || (() => {})
    this.onOpen = options.onOpen || (() => {})
    this.onClose = options.onClose || (() => {})
    this.onError = options.onError || (() => {})
  }

  connect() {
    if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    this.isManualClose = false
    this.ws = new WebSocket(this.url)

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this.startHeartbeat()
      this.onOpen()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'pong') return
        this.onMessage(data)
      } catch (e) {
        this.onMessage(event.data)
      }
    }

    this.ws.onclose = () => {
      this.stopHeartbeat()
      this.onClose()
      if (!this.isManualClose) {
        this.tryReconnect()
      }
    }

    this.ws.onerror = (error) => {
      this.onError(error)
    }
  }

  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send('ping')
      }
    }, this.heartbeatInterval)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  tryReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return
    }
    this.reconnectAttempts++
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.reconnectInterval * Math.min(this.reconnectAttempts, 5))
  }

  disconnect() {
    this.isManualClose = true
    this.stopHeartbeat()
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }
}

export function createDashboardWS(onMessage, onOpen, onClose) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const baseUrl = import.meta.env.VITE_APP_BASE_API || '/api'
  const wsUrl = `${protocol}//${host}${baseUrl}/ws/dashboard`

  return new DashboardWebSocket(wsUrl, {
    onMessage,
    onOpen,
    onClose,
    maxReconnectAttempts: 20,
    reconnectInterval: 5000,
    heartbeatInterval: 30000
  })
}
