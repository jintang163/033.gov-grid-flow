import Taro from '@tarojs/taro';
import type { OfflineEvent, EventType, EventPriority } from '@/types/event';
import type { BatchSyncRequest, BatchSyncResponse } from '@/types/sync';

const BASE_URL = 'http://localhost:8080/api';

export interface ReportEventRequest {
  type: EventType;
  title: string;
  description: string;
  priority: EventPriority;
  imageUrls: string[];
  longitude: number;
  latitude: number;
  address: string;
  anonymous: number;
  clientId?: string;
  timestamp?: number;
}

export interface ReportEventResponse {
  id: number;
  clientId?: string;
  status: string;
  message: string;
}

export const reportEvent = async (data: ReportEventRequest): Promise<ReportEventResponse> => {
  console.log('[EventService] 上报事件:', data);
  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/report`,
      method: 'POST',
      data: {
        ...data,
        clientId: data.clientId,
        eventTimestamp: data.timestamp
      },
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

  const request: BatchSyncRequest = {
    events: events.map(e => ({
      ...e,
      images: e.images.map(img => ({
        ...img,
        localPath: img.localPath,
        remoteUrl: img.remoteUrl || ''
      }))
    })),
    timestamp: Date.now(),
    deviceId: 'mobile-device-001'
  };

  try {
    const response = await Taro.request({
      url: `${BASE_URL}/event/batch-sync`,
      method: 'POST',
      data: request,
      header: {
        'Content-Type': 'application/json',
        'X-User-Id': '1'
      },
      timeout: 120000
    });

    if (response.statusCode === 200 && response.data.success) {
      console.log('[EventService] 批量同步成功:', response.data);
      return response.data as BatchSyncResponse;
    }

    throw new Error(response.data?.message || '批量同步失败');
  } catch (error) {
    console.error('[EventService] 批量同步失败:', error);
    throw error;
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
