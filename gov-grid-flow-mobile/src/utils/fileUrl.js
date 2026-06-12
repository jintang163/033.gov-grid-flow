const baseURL = import.meta.env.VITE_API_BASE_URL || ''

export const getFullFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:')) {
    return url
  }
  if (url.startsWith('/')) {
    return baseURL + url
  }
  return baseURL + '/' + url
}

export const getBaseURL = () => {
  return baseURL
}
