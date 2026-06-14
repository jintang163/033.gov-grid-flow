import request from '@/utils/request'

export function getEvidenceList(params) {
  return request({
    url: '/blockchain/evidence/list',
    method: 'get',
    params
  })
}

export function getEvidenceByEventId(eventId) {
  return request({
    url: `/blockchain/evidence/event/${eventId}`,
    method: 'get'
  })
}

export function createEvidence(eventId) {
  return request({
    url: `/blockchain/evidence/create/${eventId}`,
    method: 'post'
  })
}

export function verifyEvidence(evidenceId) {
  return request({
    url: `/blockchain/evidence/verify/${evidenceId}`,
    method: 'post'
  })
}

export function getEvidenceDetail(evidenceId) {
  return request({
    url: `/blockchain/evidence/${evidenceId}`,
    method: 'get'
  })
}
