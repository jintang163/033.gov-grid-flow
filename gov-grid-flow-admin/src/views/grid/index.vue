<template>
  <div class="grid-page">
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never" class="tree-card">
          <template #header>
            <div class="card-header">
              <span>网格组织</span>
              <el-button type="primary" size="small" @click="handleAddRoot">
                <el-icon><Plus /></el-icon>新增
              </el-button>
            </div>
          </template>
          <GridTree ref="gridTreeRef" @node-click="handleTreeNodeClick" />
        </el-card>
      </el-col>
      <el-col :span="18">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ currentGrid ? currentGrid.gridName : '网格列表' }}</span>
              <div>
                <el-button type="primary" size="small" @click="handleAdd">
                  <el-icon><Plus /></el-icon>新增子网格
                </el-button>
              </div>
            </div>
          </template>

          <el-form :inline="true" :model="searchForm" class="search-form">
            <el-form-item label="网格名称">
              <el-input v-model="searchForm.keyword" placeholder="请输入关键词" clearable />
            </el-form-item>
            <el-form-item label="网格层级">
              <el-select v-model="searchForm.gridLevel" placeholder="全部" clearable style="width: 120px">
                <el-option label="街道" :value="1" />
                <el-option label="社区" :value="2" />
                <el-option label="网格" :value="3" />
                <el-option label="微网格" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">
                <el-icon><Search /></el-icon>搜索
              </el-button>
              <el-button @click="handleReset">
                <el-icon><Refresh /></el-icon>重置
              </el-button>
            </el-form-item>
          </el-form>

          <el-table :data="tableData" border stripe v-loading="loading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="gridCode" label="网格编码" width="140" />
            <el-table-column prop="gridName" label="网格名称" min-width="160" />
            <el-table-column label="网格层级" width="100">
              <template #default="{ row }">
                <el-tag :type="getLevelTagType(row.gridLevel)" size="small">
                  {{ getLevelName(row.gridLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="gridLeaderName" label="网格长" width="100" />
            <el-table-column prop="area" label="面积(km²)" width="120" />
            <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? '启用' : '禁用' }}
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
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            style="margin-top: 16px; justify-content: flex-end; display: flex"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="网格编码" prop="gridCode">
          <el-input v-model="formData.gridCode" placeholder="请输入网格编码" />
        </el-form-item>
        <el-form-item label="网格名称" prop="gridName">
          <el-input v-model="formData.gridName" placeholder="请输入网格名称" />
        </el-form-item>
        <el-form-item label="网格层级" prop="gridLevel">
          <el-select v-model="formData.gridLevel" placeholder="请选择网格层级" style="width: 100%">
            <el-option label="街道" :value="1" />
            <el-option label="社区" :value="2" />
            <el-option label="网格" :value="3" />
            <el-option label="微网格" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级网格" prop="parentId">
          <el-tree-select
            v-model="formData.parentId"
            :data="treeOptions"
            :props="{ label: 'gridName', value: 'id', children: 'children' }"
            placeholder="请选择上级网格"
            check-strictly
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="面积" prop="area">
          <el-input-number v-model="formData.area" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import GridTree from '@/components/GridTree.vue'
import { getGridPage, createGrid, updateGrid, deleteGrid, getGridTree } from '@/api/grid'

const gridTreeRef = ref(null)
const formRef = ref(null)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogType = ref('')
const currentGrid = ref(null)
const treeOptions = ref([])

const searchForm = reactive({
  keyword: '',
  gridLevel: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const formData = reactive({
  id: null,
  gridCode: '',
  gridName: '',
  gridLevel: 3,
  parentId: null,
  area: 0,
  address: '',
  status: 1,
  sort: 0
})

const formRules = {
  gridCode: [{ required: true, message: '请输入网格编码', trigger: 'blur' }],
  gridName: [{ required: true, message: '请输入网格名称', trigger: 'blur' }],
  gridLevel: [{ required: true, message: '请选择网格层级', trigger: 'change' }]
}

const levelMap = {
  1: '街道',
  2: '社区',
  3: '网格',
  4: '微网格'
}

function getLevelName(level) {
  return levelMap[level] || '未知'
}

function getLevelTagType(level) {
  const types = {
    1: 'danger',
    2: 'warning',
    3: 'primary',
    4: 'success'
  }
  return types[level] || 'info'
}

async function loadTableData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword
    }
    if (currentGrid.value) {
      params.parentId = currentGrid.value.id
    }
    const res = await getGridPage(params)
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

async function loadTreeOptions() {
  try {
    const res = await getGridTree()
    treeOptions.value = res.data || []
  } catch (e) {
    console.error('加载网格树选项失败')
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
  searchForm.gridLevel = null
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

function handleAddRoot() {
  dialogType.value = 'add'
  dialogTitle.value = '新增网格'
  Object.assign(formData, {
    id: null,
    gridCode: '',
    gridName: '',
    gridLevel: 1,
    parentId: null,
    area: 0,
    address: '',
    status: 1,
    sort: 0
  })
  loadTreeOptions()
  dialogVisible.value = true
}

function handleAdd() {
  dialogType.value = 'add'
  dialogTitle.value = '新增子网格'
  Object.assign(formData, {
    id: null,
    gridCode: '',
    gridName: '',
    gridLevel: currentGrid.value ? (currentGrid.value.gridLevel || 0) + 1 : 3,
    parentId: currentGrid.value ? currentGrid.value.id : null,
    area: 0,
    address: '',
    status: 1,
    sort: 0
  })
  loadTreeOptions()
  dialogVisible.value = true
}

function handleView(row) {
  dialogType.value = 'view'
  dialogTitle.value = '查看网格'
  Object.assign(formData, row)
  loadTreeOptions()
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑网格'
  Object.assign(formData, row)
  loadTreeOptions()
  dialogVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该网格吗？删除后子网格也将被删除。', '提示', {
      type: 'warning'
    })
    await deleteGrid(row.id)
    ElMessage.success('删除成功')
    gridTreeRef.value?.loadTreeData()
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
      await createGrid(formData)
      ElMessage.success('新增成功')
    } else if (dialogType.value === 'edit') {
      await updateGrid(formData)
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    gridTreeRef.value?.loadTreeData()
    loadTableData()
  } catch (e) {
    if (e !== false) {
      ElMessage.error('操作失败')
    }
  }
}

onMounted(() => {
  loadTableData()
})
</script>

<style lang="scss" scoped>
.grid-page {
  .tree-card {
    height: calc(100vh - 100px);
    overflow: hidden;

    :deep(.el-card__body) {
      height: calc(100% - 57px);
      overflow-y: auto;
      padding: 12px;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 16px;
  }
}
</style>
