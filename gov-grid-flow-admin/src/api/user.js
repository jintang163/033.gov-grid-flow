import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getUserInfo() {
  return request({
    url: '/auth/userinfo',
    method: 'get'
  })
}

export function getUserList(params) {
  return request({
    url: '/user/page',
    method: 'get',
    params
  })
}

export function getUserDetail(id) {
  return request({
    url: `/user/${id}`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/user',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/user`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/user/${id}`,
    method: 'delete'
  })
}

export function resetPassword(id, password) {
  return request({
    url: `/user/${id}/reset-password`,
    method: 'put',
    params: { password }
  })
}

export function updatePassword(data) {
  return request({
    url: '/user/password',
    method: 'put',
    data
  })
}

export function updateUserRole(id, role) {
  return request({
    url: `/user/${id}/role`,
    method: 'put',
    params: { role }
  })
}

export function updateUserGrid(id, gridId) {
  return request({
    url: `/user/${id}/grid`,
    method: 'put',
    params: { gridId }
  })
}

export function importUsers(file, gridId) {
  const formData = new FormData()
  formData.append('file', file)
  if (gridId) {
    formData.append('gridId', gridId)
  }
  return request({
    url: '/user/import',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function getRoleList() {
  return request({
    url: '/user/roles',
    method: 'get'
  })
}
