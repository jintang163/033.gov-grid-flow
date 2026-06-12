<template>
  <div class="user-page">
    <el-row :gutter="16">
      <el-col :span="5">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="card-header">
              <span>网格组织</span>
            </div>
          </template>
          <GridTree ref="gridTreeRef" @node-click="handleTreeNodeClick" />
        </el-card>
      </el-col>
      <el-col :span="19">
        <el-card shadow="never">
          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="用户名">
              <el-input v-model="searchForm.keyword" placeholder="用户名/姓名/手机号" clearable style="width: 200px" />
            </el-form-item>
            <el-form-item label="角色">
              <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 140px">
                <el-option
                  v-for="item in roleOptions"
                  :key="item.code"
                  :label="item.name"
                  :value="item.code"
                />
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
              <el-button type="warning" @click="handleImport">
                <el-icon><Upload /></el-icon>批量导入
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="tableData" border stripe v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="realName" label="姓名" width="100" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
            <el-table-column label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="getRoleTagType(row.role)" size="small">
                  {{ getRoleName(row.role) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="所属网格" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.gridId">网格ID: {{ row.gridId }}</span>
                <span v-else style="color: #999">未分配</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="handleAssignRole(row)">分配角色</el-button>
                <el-button link type="primary" size="small" @click="handleAssignGrid(row)">分配网格</el-button>
                <el-button link type="warning" size="small" @click="handleResetPwd(row)">重置密码</el-button>
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
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            style="margin-top: 16px; justify-content: flex-end; display: flex"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="formData.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="formData.role" style="width: 100%">
            <el-option
              v-for="item in roleOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogType === 'add'" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-select v-model="selectedRole" style="width: 100%">
        <el-option
          v-for="item in roleOptions"
          :key="item.code"
          :label="item.name"
          :value="item.code"
        />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssignRole">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="gridDialogVisible" title="分配网格" width="400px">
      <el-tree-select
        v-model="selectedGridId"
        :data="gridTreeOptions"
        :props="{ label: 'gridName', value: 'id', children: 'children' }"
        placeholder="请选择网格"
        check-strictly
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="gridDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssignGrid">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入网格员" width="500px">
      <el-alert
        title="Excel格式说明"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      >
        <p>列顺序：用户名、姓名、手机号、邮箱、角色、密码</p>
        <p>说明：角色默认为网格员，密码默认为123456</p>
        <p>导入的用户将自动分配到当前选中的网格</p>
      </el-alert>
      <el-form-item label="目标网格" required style="margin-bottom: 16px">
        <el-tree-select
          v-model="importGridId"
          :data="gridTreeOptions"
          :props="{ label: 'gridName', value: 'id', children: 'children' }"
          placeholder="请选择导入的目标网格"
          check-strictly
          style="width: 100%"
        />
      </el-form-item>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 xlsx/xls 文件
          </div>
        </template>
      </el-upload>
      <div v-if="importResult" class="import-result">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="成功数量">{{ importResult.successCount }}</el-descriptions-item>
          <el-descriptions-item label="失败数量">{{ importResult.failCount }}</el-descriptions-item>
        </el-descriptions>
        <el-table v-if="importResult.failList?.length" :data="importResult.failList" size="small" style="margin-top: 12px">
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="error" label="失败原因" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import GridTree from '@/components/GridTree.vue'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  updateUserRole,
  updateUserGrid,
  importUsers,
  getRoleList
} from '@/api/user'
import { getGridTree } from '@/api/grid'

const gridTreeRef = ref(null)
const formRef = ref(null)
const uploadRef = ref(null)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('')
const roleDialogVisible = ref(false)
const gridDialogVisible = ref(false)
const importDialogVisible = ref(false)
const currentGrid = ref(null)
const importResult = ref(null)
const selectedRole = ref('')
const selectedGridId = ref(null)
const importGridId = ref(null)
const roleOptions = ref([])
const gridTreeOptions = ref([])

const searchForm = reactive({
  keyword: '',
  role: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const formData = reactive({
  id: null,
  username: '',
  realName: '',
  phone: '',
  email: '',
  role: 'worker',
  password: '',
  status: 1
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const roleTagMap = {
  admin: 'danger',
  street_manager: 'warning',
  grid_leader: 'primary',
  worker: 'success',
  handler: 'info',
  supervisor: 'danger'
}

function getRoleName(role) {
  const item = roleOptions.value.find(r => r.code === role)
  return item ? item.name : role
}

function getRoleTagType(role) {
  return roleTagMap[role] || 'info'
}

function formatDate(date) {
  if (!date) return ''
  return new Date(date).toLocaleString()
}

async function loadTableData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      role: searchForm.role
    }
    if (currentGrid.value) {
      params.gridId = currentGrid.value.id
    }
    const res = await getUserList(params)
    if (res.data) {
      tableData.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadRoleOptions() {
  try {
    const res = await getRoleList()
    roleOptions.value = res.data || []
  } catch (e) {
    console.error('加载角色列表失败')
  }
}

async function loadGridTreeOptions() {
  try {
    const res = await getGridTree()
    gridTreeOptions.value = res.data || []
  } catch (e) {
    console.error('加载网格树失败')
  }
}

function handleTreeNodeClick(data) {
  currentGrid.value = data
  pagination.pageNum = 1
  loadTableData()
}

function handleSearch() {
  pagination.pageNum = 1
  loadTableData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.role = ''
  pagination.pageNum = 1
  loadTableData()
}

function handlePageChange(page) {
  pagination.pageNum = page
  loadTableData()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadTableData()
}

function handleAdd() {
  dialogType.value = 'add'
  dialogTitle.value = '新增用户'
  Object.assign(formData, {
    id: null,
    username: '',
    realName: '',
    phone: '',
    email: '',
    role: 'worker',
    password: '123456',
    status: 1
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑用户'
  Object.assign(formData, row)
  formData.password = ''
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadTableData()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    if (dialogType.value === 'add') {
      const data = { ...formData }
      if (currentGrid.value) {
        data.gridId = currentGrid.value.id
      }
      await createUser(data)
      ElMessage.success('新增成功')
    } else if (dialogType.value === 'edit') {
      await updateUser(formData.id, formData)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadTableData()
  } catch (e) {
    if (e !== false) {
      ElMessage.error('操作失败')
    }
  }
}

function handleAssignRole(row) {
  selectedRole.value = row.role
  formData.id = row.id
  roleDialogVisible.value = true
}

async function confirmAssignRole() {
  try {
    await updateUserRole(formData.id, selectedRole.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadTableData()
  } catch (e) {
    ElMessage.error('角色分配失败')
  }
}

function handleAssignGrid(row) {
  selectedGridId.value = row.gridId
  formData.id = row.id
  loadGridTreeOptions()
  gridDialogVisible.value = true
}

async function confirmAssignGrid() {
  try {
    await updateUserGrid(formData.id, selectedGridId.value)
    ElMessage.success('网格分配成功')
    gridDialogVisible.value = false
    loadTableData()
  } catch (e) {
    ElMessage.error('网格分配失败')
  }
}

async function handleResetPwd(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      inputPattern: /.{6,}/,
      inputErrorMessage: '密码至少6位'
    })
    await resetPassword(row.id, value)
    ElMessage.success('密码重置成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('密码重置失败')
    }
  }
}

function handleImport() {
  importResult.value = null
  importGridId.value = currentGrid.value?.id || null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  loadGridTreeOptions()
  importDialogVisible.value = true
}

async function handleImportSubmit() {
  if (!importGridId.value) {
    ElMessage.warning('请先选择导入的目标网格')
    return
  }
  if (!uploadRef.value?.files?.length) {
    ElMessage.warning('请先选择文件')
    return
  }
  const file = uploadRef.value.files[0].raw
  try {
    const res = await importUsers(file, importGridId.value)
    importResult.value = res.data
    ElMessage.success(`导入完成，成功${res.data.successCount}条，失败${res.data.failCount}条`)
    loadTableData()
  } catch (e) {
    ElMessage.error('导入失败')
  }
}

onMounted(() => {
  loadTableData()
  loadRoleOptions()
})
</script>

<style lang="scss" scoped>
.user-page {
  .tree-card {
    height: calc(100vh - 100px);
    overflow: hidden;

    :deep(.el-card__body) {
      height: calc(100% - 57px);
      overflow-y: auto;
      padding: 12px;
    }
  }

  .search-form {
    margin-bottom: 16px;
  }

  .import-result {
    margin-top: 16px;
  }
}
</style>
