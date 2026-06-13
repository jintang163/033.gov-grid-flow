import * as offlineDB from './offlineDB'
import * as network from './network'
import { batchSyncEvents } from '@/api'

export let isSyncing = false

let syncTimer = null
let onlineHandler = null

const BATCH_SIZE = 20

export const processQueue = async (userId, deviceId) => {
  const result = { successCount: 0, failedCount: 0, duplicateCount: 0 }

  try {
    if (isSyncing) return result
    if (!network.isOnline()) return result

    isSyncing = true

    const pendingEvents = offlineDB.getPendingEvents()
    if (!pendingEvents || pendingEvents.length === 0) {
      isSyncing = false
      return result
    }

    const sortedEvents = pendingEvents.sort(
      (a, b) => (a.eventTimestamp || 0) - (b.eventTimestamp || 0)
    )

    for (const event of sortedEvents) {
      offlineDB.markSyncing(event.clientId)
    }

    for (let i = 0; i < sortedEvents.length; i += BATCH_SIZE) {
      const batch = sortedEvents.slice(i, i + BATCH_SIZE)

      try {
        const syncData = {
          userId,
          deviceId,
          events: batch.map((e) => ({
            clientId: e.clientId,
            eventTimestamp: e.eventTimestamp,
            title: e.title,
            eventType: e.eventType || e.type,
            description: e.description,
            priority: e.priority,
            lng: e.lng || e.longitude,
            lat: e.lat || e.latitude,
            address: e.address,
            images: e.images || e.imageUrls,
            videos: e.videos,
            voiceUrl: e.voiceUrl,
            anonymous: e.anonymous,
            reporterName: e.reporterName,
            reporterPhone: e.reporterPhone,
            reporterId: e.reporterId,
            gridId: e.gridId
          }))
        }

        const res = await batchSyncEvents(syncData)

        if (res && res.data) {
          const { success = [], failed = [], duplicates = [] } = res.data

          for (const item of success) {
            offlineDB.markSynced(item.clientId, item.serverId)
            result.successCount++
          }

          for (const item of failed) {
            offlineDB.markFailed(item.clientId, item.error || '同步失败')
            result.failedCount++
          }

          for (const item of duplicates) {
            offlineDB.markSynced(item.clientId, item.serverId)
            result.duplicateCount++
          }
        }
      } catch (batchError) {
        for (const event of batch) {
          offlineDB.markFailed(
            event.clientId,
            (batchError && batchError.message) || '批量同步异常'
          )
          result.failedCount++
        }
      }
    }

    isSyncing = false
    return result
  } catch (e) {
    isSyncing = false
    return result
  }
}

export const startAutoSync = (interval = 30000, userId, deviceId) => {
  try {
    stopAutoSync()

    onlineHandler = () => {
      processQueue(userId, deviceId)
    }
    network.onOnline(onlineHandler)

    syncTimer = setInterval(() => {
      processQueue(userId, deviceId)
    }, interval)

    processQueue(userId, deviceId)
  } catch (e) {}
}

export const stopAutoSync = () => {
  try {
    if (syncTimer) {
      clearInterval(syncTimer)
      syncTimer = null
    }
    if (onlineHandler) {
      network.offOnline(onlineHandler)
      onlineHandler = null
    }
  } catch (e) {}
}
