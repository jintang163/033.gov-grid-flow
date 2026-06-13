const STORAGE_KEY = 'offline_events'

const isLocalStorageAvailable = () => {
  try {
    const testKey = '__test__'
    localStorage.setItem(testKey, testKey)
    localStorage.removeItem(testKey)
    return true
  } catch (e) {
    return false
  }
}

const readEvents = () => {
  try {
    if (!isLocalStorageAvailable()) return []
    const data = localStorage.getItem(STORAGE_KEY)
    if (!data) return []
    const parsed = JSON.parse(data)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    return []
  }
}

const writeEvents = (events) => {
  try {
    if (!isLocalStorageAvailable()) return false
    localStorage.setItem(STORAGE_KEY, JSON.stringify(events))
    return true
  } catch (e) {
    return false
  }
}

export const generateClientId = () => {
  const timestamp = Date.now()
  const random = Math.random().toString(36).slice(2, 8).padEnd(6, '0')
  return `evt_${timestamp}_${random}`
}

export const saveEvent = (event) => {
  try {
    const events = readEvents()
    const clientId = event.clientId || generateClientId()
    const now = Date.now()
    const savedEvent = {
      clientId,
      eventTimestamp: event.eventTimestamp || now,
      title: event.title || '',
      eventType: event.eventType || event.type || '',
      type: event.type || event.eventType || '',
      description: event.description || '',
      priority: event.priority || 'normal',
      lng: event.lng || event.longitude || 0,
      lat: event.lat || event.latitude || 0,
      longitude: event.longitude || event.lng || 0,
      latitude: event.latitude || event.lat || 0,
      address: event.address || '',
      images: event.images || event.imageUrls || [],
      imageUrls: event.imageUrls || event.images || [],
      videos: event.videos || [],
      voiceUrl: event.voiceUrl || '',
      anonymous: event.anonymous || false,
      reporterName: event.reporterName || '',
      reporterPhone: event.reporterPhone || '',
      reporterId: event.reporterId || '',
      gridId: event.gridId || '',
      status: event.status || 'pending',
      serverId: event.serverId || '',
      retryCount: event.retryCount || 0,
      lastError: event.lastError || '',
      syncedAt: event.syncedAt || null,
      savedAt: now
    }
    events.push(savedEvent)
    writeEvents(events)
    return savedEvent
  } catch (e) {
    return null
  }
}

export const getEventList = () => {
  try {
    const events = readEvents()
    return events.sort((a, b) => (a.eventTimestamp || 0) - (b.eventTimestamp || 0))
  } catch (e) {
    return []
  }
}

export const getPendingEvents = () => {
  try {
    const events = getEventList()
    return events.filter(
      (e) => e.status === 'pending' || e.status === 'syncing' || e.status === 'failed'
    )
  } catch (e) {
    return []
  }
}

export const getEventById = (clientId) => {
  try {
    const events = readEvents()
    return events.find((e) => e.clientId === clientId) || null
  } catch (e) {
    return null
  }
}

export const updateEvent = (clientId, updates) => {
  try {
    const events = readEvents()
    const index = events.findIndex((e) => e.clientId === clientId)
    if (index === -1) return null
    events[index] = { ...events[index], ...updates }
    writeEvents(events)
    return events[index]
  } catch (e) {
    return null
  }
}

export const markSyncing = (clientId) => {
  return updateEvent(clientId, { status: 'syncing' })
}

export const markSynced = (clientId, serverId) => {
  return updateEvent(clientId, {
    status: 'synced',
    serverId: serverId || '',
    syncedAt: Date.now()
  })
}

export const markFailed = (clientId, error) => {
  try {
    const event = getEventById(clientId)
    if (!event) return null
    return updateEvent(clientId, {
      status: 'failed',
      lastError: error || '',
      retryCount: (event.retryCount || 0) + 1
    })
  } catch (e) {
    return null
  }
}

export const deleteEvent = (clientId) => {
  try {
    const events = readEvents()
    const filtered = events.filter((e) => e.clientId !== clientId)
    writeEvents(filtered)
    return true
  } catch (e) {
    return false
  }
}

export const clearSynced = () => {
  try {
    const events = readEvents()
    const filtered = events.filter((e) => e.status !== 'synced')
    writeEvents(filtered)
    return true
  } catch (e) {
    return false
  }
}

export const getStats = () => {
  try {
    const events = readEvents()
    return {
      total: events.length,
      pending: events.filter((e) => e.status === 'pending').length,
      syncing: events.filter((e) => e.status === 'syncing').length,
      failed: events.filter((e) => e.status === 'failed').length,
      synced: events.filter((e) => e.status === 'synced').length
    }
  } catch (e) {
    return { total: 0, pending: 0, syncing: 0, failed: 0, synced: 0 }
  }
}
