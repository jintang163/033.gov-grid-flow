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

    this._detectMode()
  }

  _detectMode() {
    if (typeof wx !== 'undefined' && wx.createInnerAudioContext) {
      this.mode = TTS_MODE.MINIAPP
      this.available = true
      this._initMiniApp()
    } else if ('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window) {
      this.mode = TTS_MODE.WEB
      this.available = true
      this._initWeb()
    } else {
      this.available = false
      console.warn('[TTS] 当前环境不支持语音合成')
    }
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
        this._onError && this._onError(err)
      })
      this.innerAudioContext.onStop(() => {
        this.speaking = false
      })
    } catch (e) {
      console.warn('[TTS] 小程序初始化失败:', e)
    }
  }

  speak(text, options = {}) {
    return new Promise((resolve, reject) => {
      if (!this.available) {
        reject(new Error('语音合成不可用'))
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
        voiceIndex = 0
      } = options

      if (this.mode === TTS_MODE.WEB) {
        this._speakWeb(text, { rate, pitch, volume, lang, voiceIndex })
      } else if (this.mode === TTS_MODE.MINIAPP) {
        this._speakMiniApp(text, { rate, pitch, volume, lang })
      }
    })
  }

  _speakWeb(text, options) {
    const { rate, pitch, volume, lang, voiceIndex } = options
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = lang
    utterance.rate = Math.max(0.1, Math.min(10, rate))
    utterance.pitch = pitch
    utterance.volume = volume

    const voices = this._webVoices || window.speechSynthesis.getVoices()
    const zhVoices = voices.filter(v => v.lang.startsWith('zh'))
    if (zhVoices.length > 0 && voiceIndex < zhVoices.length) {
      utterance.voice = zhVoices[voiceIndex]
    }

    utterance.onend = () => {
      this.speaking = false
      this._onEnd && this._onEnd()
    }
    utterance.onerror = (e) => {
      console.error('[TTS] Web语音合成错误:', e)
      this.speaking = false
      this._onError && this._onError(e)
    }
    utterance.onstart = () => {
      this.speaking = true
    }

    this.currentUtterance = utterance
    window.speechSynthesis.speak(utterance)
    this.speaking = true
  }

  _speakMiniApp(text, options) {
    const { rate, lang } = options
    if (typeof wx !== 'undefined' && wx.createPluginContext) {
      try {
        if (!this.plugin) {
          this.plugin = requirePlugin('WechatSI')
        }
        if (this.plugin && this.plugin.textToSpeech) {
          this.plugin.textToSpeech({
            lang: lang === 'zh-CN' ? 'zh_CN' : lang,
            tts: true,
            content: text,
            speed: Math.max(0.5, Math.min(2, rate)),
            success: (res) => {
              if (res.filename && this.innerAudioContext) {
                this.innerAudioContext.src = res.filename
                this.innerAudioContext.play()
                this.speaking = true
              } else if (res.src) {
                this.innerAudioContext.src = res.src
                this.innerAudioContext.play()
                this.speaking = true
              } else {
                this._speakFallback(text, options)
              }
            },
            fail: (err) => {
              console.warn('[TTS] 微信语音合成插件失败，尝试内建TTS:', err)
              this._speakFallback(text, options)
            }
          })
        } else {
          this._speakFallback(text, options)
        }
      } catch (e) {
        console.warn('[TTS] 小程序语音合成异常:', e)
        this._speakFallback(text, options)
      }
    } else {
      this._speakFallback(text, options)
    }
  }

  _speakFallback(text, options) {
    if ('speechSynthesis' in window) {
      this._speakWeb(text, options)
    } else {
      const { rate } = options
      console.log('[TTS] 语音播放（文字转语音不可用）:', text)
      const speakTime = Math.max(500, Math.min(10000, text.length * 200 / rate))
      this.speaking = true
      setTimeout(() => {
        this.speaking = false
        this._onEnd && this._onEnd()
      }, speakTime)
    }
  }

  stop() {
    if (this.mode === TTS_MODE.WEB) {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try {
        this.innerAudioContext.stop()
      } catch (e) {}
    }
    this.speaking = false
    this.currentUtterance = null
  }

  pause() {
    if (this.mode === TTS_MODE.WEB) {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.pause()
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try {
        this.innerAudioContext.pause()
      } catch (e) {}
    }
  }

  resume() {
    if (this.mode === TTS_MODE.WEB) {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.resume()
      }
    } else if (this.mode === TTS_MODE.MINIAPP && this.innerAudioContext) {
      try {
        this.innerAudioContext.play()
      } catch (e) {}
    }
  }

  isSpeaking() {
    return this.speaking
  }

  isAvailable() {
    return this.available
  }

  getMode() {
    return this.mode
  }

  getVoices() {
    if (this.mode === TTS_MODE.WEB && 'speechSynthesis' in window) {
      return window.speechSynthesis.getVoices()
    }
    return []
  }

  getZhVoices() {
    return this.getVoices().filter(v => v.lang.startsWith('zh'))
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
export const getVoices = () => ttsManager.getVoices()
export const getZhVoices = () => ttsManager.getZhVoices()

export default ttsManager
