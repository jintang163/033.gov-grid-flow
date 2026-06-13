export const isOnline = () => {
  try {
    if (typeof navigator === 'undefined' || typeof navigator.onLine === 'undefined') {
      return true
    }
    return navigator.onLine
  } catch (e) {
    return true
  }
}

export const onOffline = (callback) => {
  try {
    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      window.addEventListener('offline', callback)
    }
  } catch (e) {}
}

export const onOnline = (callback) => {
  try {
    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      window.addEventListener('online', callback)
    }
  } catch (e) {}
}

export const offOffline = (callback) => {
  try {
    if (typeof window !== 'undefined' && typeof window.removeEventListener === 'function') {
      window.removeEventListener('offline', callback)
    }
  } catch (e) {}
}

export const offOnline = (callback) => {
  try {
    if (typeof window !== 'undefined' && typeof window.removeEventListener === 'function') {
      window.removeEventListener('online', callback)
    }
  } catch (e) {}
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

export const retryWithBackoff = async (fn, options = {}) => {
  const { maxRetries = 3, initialDelay = 1000, factor = 2 } = options
  let lastError = null
  let delay = initialDelay

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error
      if (attempt >= maxRetries) {
        break
      }
      await sleep(delay)
      delay = delay * factor
    }
  }

  throw lastError
}
