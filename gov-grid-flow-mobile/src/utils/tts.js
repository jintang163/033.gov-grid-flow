const TTS_MODE = {
  AUTO: 'auto',
  WEB: 'web',
  MINIAPP: 'miniapp'
}

class TTSManager {
  constructor() {
    this.available = false
    this.mode = TTS_MODE.AUTO
    this.speaking = false
    this.currentUtterance = null
    this.innerAudioContext = null
    this.plugin = null
    this.wechatPluginReady = false

    this._detectMode()
    this._initOfflineCache()
    this._bindNetworkListener()
  }

  _detectMode() {
    try {
      if (typeof wx !== 'undefined' && (wx.createInnerAudioContext || wx.getSystemInfoSync)) {
        this.mode = TTS_MODE.MINIAPP
        this.available = true
        this._initMiniApp()
        return
      }
    } catch (e) {
      console.warn('[TTS] 小程序环境检测异常，降级Web TTS:', e)
    }

    if (typeof window !== 'undefined' && 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window) {
      this.mode = TTS_MODE.WEB
      this.available = true
      this._initWeb()
      return
    }

    this.available = false
    console.warn('[TTS] 当前环境不支持语音合成，将使用离线缓存+振动提示模式')
  }

  _initWeb() {
    const synth = window.speechSynthesis
    if (synth.onvoiceschanged !== undefined) {
      synth.onvoiceschanged = () => {
        this._webVoices = synth.getVoices()
      }
    } else {
      this._webVoices = synth.getVoices()
    }
    window.addEventListener('beforeunload', () => {
      try { synth.cancel() } catch (e) {}
    })
  }

  _initMiniApp() {
    try {
      this.innerAudioContext = wx.createInnerAudioContext()
      this.innerAudioContext.onEnded(() => {
        this.speaking = false
        this._onEnd && this._onEnd()
      })
      this.innerAudioContext.onError((err) => {
        console.error('[TTS] 小程序播放错误:', err)
        this.speaking = false
        if (err && (err.errMsg || '').includes('cancel')) {
          this._onEnd && this._onEnd()
        } else {
          this._onError && this._onError(err)
        }
      })
      this.innerAudioContext.onStop(() => {
        this.speaking = false
      })
      this.innerAudioContext.onCanplay(() => {
        if (this.speaking && this.innerAudioContext) {
          this.innerAudioContext.play()
        }
      })

      try {
        const app = getApp && getApp()
        if (app && app.globalData && app.globalData.voicePlugin) {
          this.plugin = app.globalData.voicePlugin
          this.wechatPluginReady = true
        }
      } catch (e) {}

      if (!this.plugin) {
        try {
          this.plugin = requirePlugin && requirePlugin('WechatSI')
          if (this.plugin) this.wechatPluginReady = true
        } catch (e) {}
      }

      if (!this.plugin) {
        try {
          this.plugin = requirePlugin && requirePlugin('wx-tts')
          if (this.plugin) this.wechatPluginReady = true
        } catch (e) {}
      }
    } catch (e) {
      console.warn('[TTS] 小程序初始化失败:', e)
      if ('speechSynthesis' in window) {
        this.mode = TTS_MODE.WEB
        this._initWeb()
      }
    }
  }

  _initOfflineCache() {
    try {
      const cache = localStorage.getItem('tts_offline_cache')
      this._offlineCache = cache ? JSON.parse(cache) : {}
    } catch (e) {
      this._offlineCache = {}
    }
    try {
      const queue = localStorage.getItem('tts_offline_queue')
      this._offlineQueue = queue ? JSON.parse(queue) : []
    } catch (e) {
      this._offlineQueue = []
    }
  }

  _saveOfflineCache() {
    try {
      const keys = Object.keys(this._offlineCache)
      if (keys.length > 200) {
        const sorted = keys.sort((a, b) => (this._offlineCache[b].t || 0) - (this._offlineCache[a].t || 0))
        sorted.slice(150).forEach(k => delete this._offlineCache[k])
      }
      localStorage.setItem('tts_offline_cache', JSON.stringify(this._offlineCache))
    } catch (e) {}
  }

  _bindNetworkListener() {
    if (typeof network !== 'undefined' && network.onOffline) {
      network.onOffline(() => this._isOnline = false)
      network.onOnline(() => {
        this._isOnline = true
        this._flushOfflineQueue()
      })
      try { this._isOnline = network.isOnline() } catch (e) { this._isOnline = true }
    } else if (typeof window !== 'undefined' && 'navigator' in window) {
      this._isOnline = navigator.onLine !== false
      window.addEventListener && window.addEventListener('online', () => {
        this._isOnline = true
        this._flushOfflineQueue()
      })
      window.addEventListener && window.addEventListener('offline', () => { this._isOnline = false })
    } else {
      this._isOnline = true
    }
  }

  _flushOfflineQueue() {
    if (this._offlineQueue.length === 0) return
    const queue = [...this._offlineQueue]
    this._offlineQueue = []
    try { localStorage.setItem('tts_offline_queue', '[]') } catch (e) {}

    queue.forEach((item, idx) => {
      setTimeout(() => {
        this.speak(item.text, item.options || {}).catch(err => {
          console.warn('[TTS] 离线队列重放失败:', item.text.slice(0, 30), err)
        })
      }, idx * 500)
    })
    console.info(`[TTS] 网络恢复，重放 ${queue.length} 条离线播报`)
  }

  _queueOffline(text, options) {
    this._offlineQueue.push({ text, options, time: Date.now() })
    if (this._offlineQueue.length > 50) {
      this._offlineQueue = this._offlineQueue.slice(-30)
    }
    try { localStorage.setItem('tts_offline_queue', JSON.stringify(this._offlineQueue)) } catch (e) {}
    this._notifyUser(text)
  }

  _notifyUser(text) {
    try {
      if (typeof wx !== 'undefined' && wx.vibrateShort) {
        wx.vibrateShort({ type: 'medium' })
      }
      if (typeof notificationService !== 'undefined') {
        const snippet = text.length > 30 ? text.slice(0, 30) + '...' : text
        notificationService && notificationService.localNotify && notificationService.localNotify('语音播报提示', snippet)
      }
    } catch (e) {}
    console.log(`[TTS] (离线模式) ${text.slice(0, 60)}...`)
  }

  speak(text, options = {}) {
    return new Promise((resolve, reject) => {
      if (!this.available) {
        if (text) this._notifyUser(text)
        resolve()
        return
      }
      if (this.speaking) {
        this.stop()
      }

      this._onEnd = resolve
      this._onError = reject

      const {
        rate = 1.0,
        pitch = 1.0,
        volume = 1.0,
        lang = 'zh-CN',
        voiceIndex = 0,
        useCache = true,
        priority = 0
      } = options

      const cacheKey = useCache
        ? `${lang}_${rate}_${pitch}_${volume}_${text.length > 100 ? text.slice(0, 100) : text}`
        : null

      if (this._isOnline === false && this.mode === TTS_MODE.MINIAPP && this.plugin) {
        const cached = useCache ? this._offlineCache[cacheKey] : null
        if (cached && cached.src && this.innerAudioContext) {
          console.info('[TTS] 使用离线音频缓存播放')
          try {
            this.innerAudioContext.src = cached.src
            this.speaking = true
            this.innerAudioContext.play()
            return
          } catch (e) {
            console.warn('[TTS] 缓存播放失败，使用振动+控制台模式')
          }
        }
        this._queueOffline(text, options)
        resolve()
        return
      }

      if (this.mode === TTS_MODE.WEB) {
        this._speakWeb(text, { rate, pitch, volume, lang, voiceIndex, cacheKey })
      } else if (this.mode === TTS_MODE.MINIAPP) {
        this._speakMiniApp(text, { rate, pitch, volume, lang, cacheKey })
      }
    })
  }

  _speakWeb(text, options) {
    const { rate, pitch, volume, lang, voiceIndex, cacheKey } = options
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = lang
    utterance.rate = Math.max(0.1, Math.min(10, rate))
    utterance.pitch = pitch
    utterance.volume = volume

    const voices = this._webVoices || window.speechSynthesis.getVoices()
    const zhVoices = voices.filter(v => v.lang.startsWith('zh'))
    if (zhVoices.length > 0 && voiceIndex < zhVoices.length) {
      utterance.voice = zhVoices[voiceIndex]
    } else if (voices.length > 0) {
      utterance.voice = voices[0]
    }

    utterance.onend = () => {
      this.speaking = false
      this._cacheResult(cacheKey, { type: 'web', t: Date.now() })
      this._onEnd && this._onEnd()
    }
    utterance.onerror = (e) => {
      console.error('[TTS] Web语音合成错误:', e)
      this.speaking = false
      if (e && e.error === 'canceled' || e.error === 'interrupted') {
        this._onEnd && this._onEnd()
      } else {
        this._onError && this._onError(e)
      }
    }
    utterance.onstart = () => { this.speaking = true }
    utterance.onpause = () => {}
    utterance.onresume = () => {}

    this.currentUtterance = utterance
    window.speechSynthesis.speak(utterance)
    this.speaking = true

    if (window.speechSynthesis.paused) {
      window.speechSynthesis.resume()
    }

    if (zhVoices.length === 0) {
      setTimeout(() => {
        try {
          const newVoices = window.speechSynthesis.getVoices()
          const newZh = newVoices.filter(v => v.lang.startsWith('zh'))
          if (newZh.length > 0 && this.currentUtterance === utterance) {
            window.speechSynthesis.cancel()
            utterance.voice = newZh[0]
            window.speechSynthesis.speak(utterance)
          }
        } catch (e) {}
      }, 300)
    }
  }

  _speakMiniApp(text, options) {
    const { rate, lang, cacheKey } = options
    const tryWechatPlugin = this.plugin && this.plugin.textToSpeech
    const tryMiniSpeech = typeof wx !== 'undefined' && wx.createMiniSpeechEngine

    if (tryMiniSpeech) {
      try {
        if (!this._speechEngine) {
          this._speechEngine = wx.createMiniSpeechEngine()
          this._speechEngine.onEnd && this._speechEngine.onEnd(() => {
            this.speaking = false
            this._onEnd && this._onEnd()
          })
          this._speechEngine.onError && this._speechEngine.onError((err) => {
            console.warn('[TTS] 微信原生TTS失败:', err)
            this._tryPluginTTS(text, rate, lang, cacheKey)
          })
        }
        this._speechEngine.speak({
          content: text,
          lang: lang === 'zh-CN' ? 'zh_CN' : lang,
          speed: Math.max(0.5, Math.min(2, rate)),
          success: () => { this.speaking = true },
          fail: (e) => { this._tryPluginTTS(text, rate, lang, cacheKey) }
        })
        return
      } catch (e) {
        console.warn('[TTS] 原生TTS异常:', e)
      }
    }

    this._tryPluginTTS(text, rate, lang, cacheKey)
  }

  _tryPluginTTS(text, rate, lang, cacheKey) {
    if (this.plugin && this.plugin.textToSpeech) {
      try {
        this.plugin.textToSpeech({
          lang: lang === 'zh-CN' ? 'zh_CN' : lang,
          tts: true,
          content: text,
          speed: Math.max(0.5, Math.min(2, rate)),
          success: (res) => {
            const audioSrc = res.filename || res.src || res.filePath || res.url
            if (audioSrc && this.innerAudioContext) {
              this._cacheResult(cacheKey, { type: 'miniapp', src: audioSrc, t: Date.now() })
              this.innerAudioContext.src = audioSrc
              this.speaking = true
              try { this.innerAudioContext.play() } catch (e) {}
            } else {
              this._speakFallback(text, { rate, lang })
            }
          },
          fail: (err) => {
            console.warn('[TTS] 微信语音合成插件失败，降级:', err)
            this._speakFallback(text, { rate, lang })
          }
        })
        return
      } catch (e) {
        console.warn('[TTS] 插件调用异常:', e)
      }
    }
    this._speakFallback(text, { rate, lang })
  }

  _cacheResult(key, value) {
    if (!key) return
    this._offlineCache[key] = value
    this._saveOfflineCache()
  }

  _speakFallback(text, options) {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      const { rate = 1.0, pitch = 1.0, volume = 1.0, lang = 'zh-CN' } = options
      this._speakWeb(text, { rate, pitch, volume, lang, voiceIndex: 0 })
    } else {
      const { rate = 1.0 } = options
      this._notifyUser(text)
      const speakTime = Math.max(500, Math.min(15000, text.length * 200 / rate))
      this.speaking = true
      setTimeout(() => {
        this.speaking = false
        this._onEnd && this._onEnd()
      }, speakTime)
    }
  }

  stop() {
    if (this.mode === TTS_MODE.WEB) {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        try { window.speechSynthesis.cancel() } catch (e) {}
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try {
        this.innerAudioContext.stop()
      } catch (e) {}
    }
    if (this._speechEngine && this._speechEngine.stop) {
      try { this._speechEngine.stop() } catch (e) {}
    }
    this.speaking = false
    this.currentUtterance = null
  }

  pause() {
    if (this.mode === TTS_MODE.WEB) {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        try { window.speechSynthesis.pause() } catch (e) {}
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try { this.innerAudioContext.pause() } catch (e) {}
    }
  }

  resume() {
    if (this.mode === TTS_MODE.WEB) {
      if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
        try { window.speechSynthesis.resume() } catch (e) {}
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try { this.innerAudioContext.play() } catch (e) {}
    }
  }

  isSpeaking() { return this.speaking }
  isAvailable() { return this.available }
  getMode() { return this.mode }
  isOnline() { return this._isOnline !== false }

  getVoices() {
    if (this.mode === TTS_MODE.WEB && typeof window !== 'undefined' && 'speechSynthesis' in window) {
      return window.speechSynthesis.getVoices()
    }
    return []
  }

  getZhVoices() {
    return this.getVoices().filter(v => v.lang && v.lang.startsWith('zh'))
  }

  getOfflineQueueSize() {
    return this._offlineQueue ? this._offlineQueue.length : 0
  }

  clearOfflineCache() {
    this._offlineCache = {}
    this._offlineQueue = []
    try { localStorage.removeItem('tts_offline_cache') } catch (e) {}
    try { localStorage.removeItem('tts_offline_queue') } catch (e) {}
  }
}

const ttsManager = new TTSManager()

export const speak = (text, options) => ttsManager.speak(text, options)
export const stop = () => ttsManager.stop()
export const pause = () => ttsManager.pause()
export const resume = () => ttsManager.resume()
export const isSpeaking = () => ttsManager.isSpeaking()
export const isAvailable = () => ttsManager.isAvailable()
export const getMode = () => ttsManager.getMode()
export const isOnline = () => ttsManager.isOnline()
export const getVoices = () => ttsManager.getVoices()
export const getZhVoices = () => ttsManager.getZhVoices()
export const getOfflineQueueSize = () => ttsManager.getOfflineQueueSize()
export const clearOfflineCache = () => ttsManager.clearOfflineCache()

export default ttsManager
