import request from '@/utils/request'

export function getGridList(params) {
  return request({
    url: '/grid/list',
    method: 'get',
    params
  })
}

export function getGridTree() {
  return request({
    url: '/grid/tree',
    method: 'get'
  })
}

export function getGridTreeByLevel(level) {
  return request({
    url: '/grid/tree/level',
    method: 'get',
    params: { level }
  })
}

export function getGridChildren(parentId) {
  return request({
    url: '/grid/children',
    method: 'get',
    params: { parentId }
  })
}

export function getGridDetail(id) {
  return request({
    url: `/grid/${id}`,
    method: 'get'
  })
}

export function createGrid(data) {
  return request({
    url: '/grid',
    method: 'post',
    data
  })
}

export function updateGrid(data) {
  return request({
    url: '/grid',
    method: 'put',
    data
  })
}

export function deleteGrid(id) {
  return request({
    url: `/grid/${id}`,
    method: 'delete'
  })
}

export function getGridAll() {
  return request({
    url: '/grid/all',
    method: 'get'
  })
}

export function getDeptList(params) {
  return request({
    url: '/dept/list',
    method: 'get',
    params
  })
}

export function getDeptTree() {
  return request({
    url: '/dept/tree',
    method: 'get'
  })
}

export function createDept(data) {
  return request({
    url: '/dept',
    method: 'post',
    data
  })
}

export function updateDept(id, data) {
  return request({
    url: `/dept/${id}`,
    method: 'put',
    data
  })
}

export function deleteDept(id) {
  return request({
    url: `/dept/${id}`,
    method: 'delete'
  })
}

export function getGridMemberList(params) {
  return request({
    url: '/grid/member/list',
    method: 'get',
    params
  })
}

export function addGridMember(data) {
  return request({
    url: '/grid/member',
    method: 'post',
    data
  })
}

export function removeGridMember(id) {
  return request({
    url: `/grid/member/${id}`,
    method: 'delete'
  })
}

export function getGridPage(params) {
  return request({
    url: '/grid/page',
    method: 'get',
    params
  })
}

export function getGrid(id) {
  return request({
    url: `/grid/${id}`,
    method: 'get'
  })
}

export function getGridMembers(gridId) {
  return request({
    url: '/grid/members',
    method: 'get',
    params: { gridId }
  })
}

export function getGridLevelOptions() {
  return [
    { value: 1, label: '街道' },
    { value: 2, label: '社区' },
    { value: 3, label: '网格' },
    { value: 4, label: '微网格' }
  ]
}
