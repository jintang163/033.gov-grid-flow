import request from '@/utils/request'

export function queryAuditLogs(data) {
  return request({
    url: '/audit-log/query',
    method: 'post',
    data
  })
}

export function getAuditLogDetail(id) {
  return request({
    url: `/audit-log/${id}`,
    method: 'get'
  })
}

export function exportAuditPdf(data) {
  return request({
    url: '/audit-log/export-pdf',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

export function verifyLogIntegrity(id) {
  return request({
    url: `/audit-log/verify/${id}`,
    method: 'get'
  })
}
