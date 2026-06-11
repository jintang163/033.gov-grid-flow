<template>
  <div class="grid-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="网格名称">
          <el-input v-model="searchForm.name" placeholder="请输入网格名称" clearable />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-select v-model="searchForm.area" placeholder="请选择区域" clearable>
            <el-option label="东城区" value="1" />
            <el-option label="西城区" value="2" />
            <el-option label="南城区" value="3" />
            <el-option label="北城区" value="4" />
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
            <el-icon><Plus /></el-icon>新增网格
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="网格名称" />
        <el-table-column prop="area" label="所属区域" width="120" />
        <el-table-column prop="gridLeader" label="网格长" width="100" />
        <el-table-column prop="memberCount" label="网格员数量" width="120" />
        <el-table-column prop="population" label="服务人口" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'danger'">
              {{ row.status === '1' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
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
  name: '',
  area: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([
  { id: 1, name: '东城区第一网格', area: '东城区', gridLeader: '张三', memberCount: 15, population: 3500, status: '1' },
  { id: 2, name: '西城区第三网格', area: '西城区', gridLeader: '李四', memberCount: 12, population: 2800, status: '1' },
  { id: 3, name: '南城区第二网格', area: '南城区', gridLeader: '王五', memberCount: 18, population: 4200, status: '1' }
])

function handleSearch() {
  pagination.pageNum = 1
}

function handleReset() {
  searchForm.name = ''
  searchForm.area = ''
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
.grid-page {
  .search-form {
    margin-bottom: 0;
  }
}
</style>
