import { useCallback } from 'react';
import Taro from '@tarojs/taro';
import { useOfflineStore } from '@/store/useOfflineStore';
import { useSyncStore } from '@/store/useSyncStore';
import { saveEvent, deleteEvent, getEventByClientId } from '@/utils/offlineDB';
import { generateClientId, generateImageId } from '@/utils/uuid';
import type { OfflineEvent, EventType, EventPriority, EventImage, LocationInfo } from '@/types/event';

export interface CreateEventParams {
  type: EventType;
  title: string;
  description: string;
  priority: EventPriority;
  images: Array<{ path: string; size: number }>;
  location: LocationInfo;
  reporterId: string;
  reporterName: string;
  anonymous: number;
}

export const useOfflineDB = () => {
  const { events, pendingEvents, addEvent, updateEvent, removeEvent, refreshAll } = useOfflineStore();
  const { setPendingCount } = useSyncStore();

  const createEvent = useCallback(async (params: CreateEventParams): Promise<OfflineEvent> => {
    const now = Date.now();
    const eventImages: EventImage[] = params.images.map(img => ({
      id: generateImageId(),
      localPath: img.path,
      size: img.size,
      uploadStatus: 'pending',
      createdAt: now
    }));

    const event: OfflineEvent = {
      clientId: generateClientId(),
      type: params.type,
      title: params.title,
      description: params.description,
      priority: params.priority,
      images: eventImages,
      location: params.location,
      reporterId: params.reporterId,
      reporterName: params.reporterName,
      anonymous: params.anonymous,
      createdAt: now,
      updatedAt: now,
      syncStatus: 'pending',
      syncRetryCount: 0
    };

    await saveEvent(event);
    await refreshAll();
    console.log('[useOfflineDB] 事件已创建:', event.clientId);

    Taro.showToast({
      title: '已保存到本地',
      icon: 'success'
    });

    return event;
  }, [addEvent, refreshAll]);

  const updateExistingEvent = useCallback(async (event: OfflineEvent): Promise<void> => {
    const updatedEvent = {
      ...event,
      updatedAt: Date.now()
    };
    await saveEvent(updatedEvent);
    await refreshAll();
    console.log('[useOfflineDB] 事件已更新:', event.clientId);
  }, [updateEvent, refreshAll]);

  const deleteEventById = useCallback(async (clientId: string): Promise<void> => {
    await Taro.showModal({
      title: '确认删除',
      content: '确定要删除这条本地记录吗？',
      success: async (res) => {
        if (res.confirm) {
          await deleteEvent(clientId);
          await refreshAll();
          Taro.showToast({
            title: '删除成功',
            icon: 'success'
          });
        }
      }
    });
  }, [removeEvent, refreshAll]);

  const getEvent = useCallback(async (clientId: string): Promise<OfflineEvent | null> => {
    return getEventByClientId(clientId);
  }, []);

  const getEventsByStatus = useCallback((status: string) => {
    return events.filter(e => e.syncStatus === status);
  }, [events]);

  const getPendingCount = useCallback(() => {
    const count = pendingEvents.length;
    setPendingCount(count);
    return count;
  }, [pendingEvents, setPendingCount]);

  const formatFileSize = useCallback((bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }, []);

  const formatDate = useCallback((timestamp: number): string => {
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  }, []);

  const formatRelativeTime = useCallback((timestamp: number): string => {
    const now = Date.now();
    const diff = now - timestamp;

    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    if (diff < 7 * 86400000) return Math.floor(diff / 86400000) + '天前';

    return formatDate(timestamp);
  }, [formatDate]);

  return {
    events,
    pendingEvents,
    createEvent,
    updateEvent: updateExistingEvent,
    deleteEvent: deleteEventById,
    getEvent,
    getEventsByStatus,
    getPendingCount,
    formatFileSize,
    formatDate,
    formatRelativeTime
  };
};
