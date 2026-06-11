<template>
  <div class="user-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" placeholder="请选择角色" clearable>
            <el-option label="管理员" value="1" />
            <el-option label="网格长" value="2" />
            <el-option label="网格员" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === '1' ? 'danger' : row.role === '2' ? 'warning' : 'info'">
              {{ row.role === '1' ? '管理员' : row.role === '2' ? '网格长' : '网格员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end; display: flex"
      />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'

const searchForm = reactive({
  username: '',
  phone: '',
  role: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([
  { id: 1, username: 'admin', nickname: '超级管理员', phone: '13800138000', email: 'admin@example.com', role: '1', status: '1', createTime: '2024-01-01 00:00:00' },
  { id: 2, username: 'zhangsan', nickname: '张三', phone: '13800138001', email: 'zhangsan@example.com', role: '2', status: '1', createTime: '2024-01-10 09:30:00' },
  { id: 3, username: 'lisi', nickname: '李四', phone: '13800138002', email: 'lisi@example.com', role: '3', status: '1', createTime: '2024-01-12 14:20:00' }
])

function handleSearch() {
  pagination.pageNum = 1
}

function handleReset() {
  searchForm.username = ''
  searchForm.phone = ''
  searchForm.role = ''
  pagination.pageNum = 1
}

function handleAdd() {
}

function handleView(row) {
}

function handleEdit(row) {
}

function handleDelete(row) {
}
</script>

<style lang="scss" scoped>
.user-page {
  .search-form {
    margin-bottom: 0;
  }
}
</style>
