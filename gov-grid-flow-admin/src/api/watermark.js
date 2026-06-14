import request from '@/utils/request'

export const uploadWithWatermark = (file, reportTime, reporterName, eventNo, eventId, reporterId, sensitive) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('reportTime', reportTime)
  formData.append('reporterName', reporterName)
  if (eventNo) formData.append('eventNo', eventNo)
  if (eventId) formData.append('eventId', eventId)
  if (reporterId) formData.append('reporterId', reporterId)
  if (sensitive !== undefined) formData.append('sensitive', sensitive)

  return request({
    url: '/watermark/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const batchUploadWithWatermark = (files, reportTime, reporterName, eventNo, eventId, reporterId, sensitive) => {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))
  formData.append('reportTime', reportTime)
  formData.append('reporterName', reporterName)
  if (eventNo) formData.append('eventNo', eventNo)
  if (eventId) formData.append('eventId', eventId)
  if (reporterId) formData.append('reporterId', reporterId)
  if (sensitive !== undefined) formData.append('sensitive', sensitive)

  return request({
    url: '/watermark/upload/batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const checkTamper = (fileUrl) => {
  return request({
    url: '/watermark/check-tamper',
    method: 'get',
    params: { fileUrl }
  })
}

export const checkEventFilesTamper = (eventId) => {
  return request({
    url: `/watermark/check-event-tamper/${eventId}`,
    method: 'get'
  })
}

export default {
  uploadWithWatermark,
  batchUploadWithWatermark,
  checkTamper,
  checkEventFilesTamper
}
