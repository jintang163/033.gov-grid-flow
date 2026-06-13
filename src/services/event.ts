import Taro from '@tarojs/taro';
import type { OfflineEvent, EventType, EventPriority } from '@/types/event';
import type { BatchSyncRequest, BatchSyncResponse } from '@/types/sync';

const BASE_URL = 'http://localhost:8080/api';

export interface ReportEventRequest {
  clientId?: string;
  eventTimestamp?: number;
  eventType: EventType;
  type?: EventType;
  title: string;
  description: string;
  priority: EventPriority;
  images?: string[];
  imageUrls?: string[];
  lng?: number;
  lat?: number;
  longitude?: number;
  latitude?: number;
  address: string;
  anonymous: number;
  reporterId?: string | number;
  reporterName?: string;
  reporterPhone?: string;
  gridId?: number;
  voiceUrl?: string;
  videos?: string[];
}

export interface ReportEventResponse {
  id: number;
  clientId?: string;
  eventNo?: string;
  status: string;
  message: string;
}

const normalizePriority = (p: string): string => {
  if (!p) return 'NORMAL';
  const map: Record<string, string> = {
    low: 'LOW',
    medium: 'NORMAL',
    normal: 'NORMAL',
    high: 'HIGH',
    urgent: 'URGENT'
  };
  return map[p.toLowerCase()] || 'NORMAL';
};

const toBackendPayload = (req: ReportEventRequest): Record<string, any> => {
  const payload: Record<string, any> = {
    clientId: req.clientId,
    eventTimestamp: req.eventTimestamp,
    eventType: req.eventType || req.type,
    title: req.title,
    description: req.description,
    priority: normalizePriority(req.priority),
    address: req.address,
    anonymous: req.anonymous,
    reporterId: req.reporterId,
    reporterName: req.reporterName,
    reporterPhone: req.reporterPhone,
    gridId: req.gridId,
    voiceUrl: req.voiceUrl,
    videos: req.videos
  };

  const resolvedImages = req.images && req.images.length > 0 ? req.images : req.imageUrls;
  if (resolvedImages && resolvedImages.length > 0) {
    payload.images = resolvedImages;
    payload.imageUrls = resolvedImages;
  }

  if (req.lng !== undefined) {
    payload.lng = req.lng;
    payload.longitude = req.lng;
  } else if (req.longitude !== undefined) {
    payload.lng = req.longitude;
    payload.longitude = req.longitude;
  }

  if (req.lat !== undefined) {
    payload.lat = req.lat;
    payload.latitude = req.lat;
  } else if (req.latitude !== undefined) {
    payload.lat = req.latitude;
    payload.latitude = req.latitude;
  }

  return payload;
};

export const reportEvent = async (data: ReportEventRequest): Promise<ReportEventResponse> => {
  console.log('[EventService] 上报事件:', data);
  try {
    const payload = toBackendPayload(data);
    const response = await Taro.request({
      url: `${BASE_URL}/event/report`,
      method: 'POST',
      data: payload,
      header: {
        'Content-Type': 'application/json',
        'X-User-Id': '1'
      },
      timeout: 30000
    });

    if (response.statusCode === 200 && response.data.success) {
      console.log('[EventService] 事件上报成功:', response.data);
      return {
        id: response.data.data?.id || 0,
        clientId: data.clientId,
        eventNo: response.data.data?.eventNo,
        status: 'success',
        message: response.data.message || '上报成功'
      };
    }

    throw new Error(response.data?.message || '上报失败');
  } catch (error) {
    console.error('[EventService] 事件上报失败:', error);
    throw error;
  }
};

export const uploadImage = async (filePath: string): Promise<string> => {
  console.log('[EventService] 上传图片:', filePath);
  try {
    const response = await Taro.uploadFile({
      url: `${BASE_URL}/file/upload`,
      filePath,
      name: 'file',
      header: {
        'X-User-Id': '1'
      },
      timeout: 60000
    });

    if (response.statusCode === 200) {
      const data = JSON.parse(response.data);
      if (data.success) {
        console.log('[EventService] 图片上传成功:', data.data);
        return data.data || '';
      }
      throw new Error(data.message || '上传失败');
    }

    throw new Error(`上传失败，状态码: ${response.statusCode}`);
  } catch (error) {
    console.error('[EventService] 图片上传失败:', error);
    throw error;
  }
};

export const batchSyncEvents = async (events: OfflineEvent[]): Promise<BatchSyncResponse> => {
  console.log('[EventService] 批量同步事件，数量:', events.length);

  const backendEvents: ReportEventRequest[] = events.map(e => {
    const imageUrls = e.images
      .map(img => img.remoteUrl || img.localPath)
      .filter(url => url && url.length > 0);

    return {
      clientId: e.clientId,
      eventTimestamp: e.createdAt,
      eventType: e.type,
      title: e.title,
      description: e.description,
      priority: e.priority,
      imageUrls,
      images: imageUrls,
      longitude: e.location?.longitude,
      latitude: e.location?.latitude,
      lng: e.location?.longitude,
      lat: e.location?.latitude,
      address: e.location?.address,
      anonymous: e.anonymous,
      reporterId: e.reporterId,
      reporterName: e.reporterName
    };
  });

  const requestData = {
    events: backendEvents.map(e => toBackendPayload(e)),
    timestamp: Date.now(),
    deviceId: 'mobile-device-001'
  };

  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/batch-sync`,
      method: 'POST',
      data: requestData,
      header: {
        'Content-Type': 'application/json',
        'X-User-Id': '1'
      },
      timeout: 120000
    });

    if (response.statusCode === 200 && response.data.success) {
      console.log('[EventService] 批量同步成功:', response.data);
      const raw = response.data.data || response.data;
      const results = raw.results || [];
      return {
        success: raw.success !== false,
        message: raw.message || '批量同步完成',
        results: results.map((r: any) => ({
          clientId: r.clientId,
          serverId: r.serverId,
          success: r.success !== false,
          error: r.error
        }))
      };
    }

    throw new Error(response.data?.message || '批量同步失败');
  } catch (error) {
    console.error('[EventService] 批量同步失败:', error);
    throw error;
  }
};

export const syncAll = batchSyncEvents;

export const getEventByClientId = async (clientId: string): Promise<any> => {
  console.log('[EventService] 根据clientId查询事件:', clientId);
  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/client/${clientId}`,
      method: 'GET',
      header: {
        'X-User-Id': '1'
      }
    });

    if (response.statusCode === 200 && response.data.success) {
      return response.data.data;
    }
    return null;
  } catch (error) {
    console.error('[EventService] clientId查询失败:', error);
    return null;
  }
};

export const getEventList = async (params: {
  page?: number;
  size?: number;
  status?: string;
}): Promise<{ list: any[]; total: number }> => {
  console.log('[EventService] 获取事件列表:', params);
  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/list`,
      method: 'GET',
      data: params,
      header: {
        'X-User-Id': '1'
      }
    });

    if (response.statusCode === 200 && response.data.success) {
      return {
        list: response.data.data?.records || [],
        total: response.data.data?.total || 0
      };
    }

    throw new Error(response.data?.message || '获取列表失败');
  } catch (error) {
    console.error('[EventService] 获取事件列表失败:', error);
    return { list: [], total: 0 };
  }
};

export const getEventTypeList = async (): Promise<Array<{ code: string; name: string }>> => {
  console.log('[EventService] 获取事件类型列表');
  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/type/list`,
      method: 'GET',
      header: {
        'X-User-Id': '1'
      }
    });

    if (response.statusCode === 200 && response.data.success) {
      return response.data.data || [];
    }

    throw new Error(response.data?.message || '获取类型列表失败');
  } catch (error) {
    console.error('[EventService] 获取事件类型列表失败:', error);
    return [
      { code: 'environment', name: '环境卫生' },
      { code: 'public_facility', name: '公共设施' },
      { code: 'dispute', name: '矛盾纠纷' },
      { code: 'safety_hazard', name: '安全隐患' },
      { code: 'traffic', name: '交通出行' },
      { code: 'service', name: '民生服务' },
      { code: 'security', name: '治安问题' },
      { code: 'other', name: '其他问题' }
    ];
  }
};

export const getEventDetail = async (id: number): Promise<any> => {
  console.log('[EventService] 获取事件详情:', id);
  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/${id}`,
      method: 'GET',
      header: {
        'X-User-Id': '1'
      }
    });

    if (response.statusCode === 200 && response.data.success) {
      return response.data.data;
    }

    throw new Error(response.data?.message || '获取详情失败');
  } catch (error) {
    console.error('[EventService] 获取事件详情失败:', error);
    return null;
  }
};
