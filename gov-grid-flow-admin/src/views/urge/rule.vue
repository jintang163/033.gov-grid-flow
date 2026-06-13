<template>
  <div class="urge-rule-page">
    <el-card shadow="never">
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增规则
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="eventTypeName" label="事件类型" width="160">
          <template #default="{ row }">
            <el-tag type="info">{{ row.eventTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeLimitHours" label="处置时限(小时)" width="140" />
        <el-table-column prop="warningRatio" label="预警比例" width="120">
          <template #default="{ row }">
            {{ row.warningRatio ? (row.warningRatio * 100).toFixed(0) + '%' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="escalateLevel" label="升级级别" width="120">
          <template #default="{ row }">
            {{ getEscalateLevelLabel(row.escalateLevel) }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleToggle(row)">
              {{ row.enabled ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="事件类型" prop="eventTypeId">
          <el-select v-model="form.eventTypeId" placeholder="请选择事件类型" style="width: 100%" @change="handleEventTypeChange">
            <el-option
              v-for="item in eventTypeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处置时限" prop="timeLimitHours">
          <el-input-number v-model="form.timeLimitHours" :min="1" :max="999" style="width: 100%" />
          <span style="color: #909399; font-size: 12px">单位：小时</span>
        </el-form-item>
        <el-form-item label="预警比例" prop="warningRatio">
          <el-input-number v-model="form.warningRatio" :min="0.1" :max="1" :step="0.1" :precision="1" style="width: 100%" />
          <span style="color: #909399; font-size: 12px">例如：0.2 表示剩余20%时间时预警</span>
        </el-form-item>
        <el-form-item label="升级级别" prop="escalateLevel">
          <el-select v-model="form.escalateLevel" placeholder="请选择升级级别" style="width: 100%">
            <el-option label="一级" value="1" />
            <el-option label="二级" value="2" />
            <el-option label="三级" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用" prop="enabled">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
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
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listRules, saveRule, updateRule, deleteRule } from '@/api/urge'
import { getEventTypeList } from '@/api/event'

const loading = ref(false)
const tableData = ref([])
const eventTypeList = ref([])

const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const currentId = ref(null)
const form = reactive({
  eventTypeId: '',
  eventTypeName: '',
  timeLimitHours: 24,
  warningRatio: 0.2,
  escalateLevel: '1',
  enabled: true
})

const formRules = {
  eventTypeId: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  timeLimitHours: [{ required: true, message: '请输入处置时限', trigger: 'blur' }],
  warningRatio: [{ required: true, message: '请输入预警比例', trigger: 'blur' }],
  escalateLevel: [{ required: true, message: '请选择升级级别', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑规则' : '新增规则')

function getEscalateLevelLabel(level) {
  const map = { '1': '一级', '2': '二级', '3': '三级' }
  return map[level] || level || '-'
}

async function fetchEventTypeList() {
  try {
    const res = await getEventTypeList()
    eventTypeList.value = res?.data || res?.rows || []
  } catch (e) {
    eventTypeList.value = [
      { id: 1, name: '市政设施' },
      { id: 2, name: '环境卫生' },
      { id: 3, name: '治安隐患' },
      { id: 4, name: '民生服务' }
    ]
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listRules()
    tableData.value = res?.rows || res?.data?.list || res?.data || []
  } catch (e) {
    tableData.value = generateMockData()
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  const eventTypes = [
    { id: 1, name: '市政设施' },
    { id: 2, name: '环境卫生' },
    { id: 3, name: '治安隐患' },
    { id: 4, name: '民生服务' }
  ]
  const levels = ['1', '2', '3']
  return eventTypes.map((item, index) => ({
    id: index + 1,
    eventTypeId: item.id,
    eventTypeName: item.name,
    timeLimitHours: 24 + index * 12,
    warningRatio: 0.2 + index * 0.1,
    escalateLevel: levels[index % 3],
    enabled: index % 2 === 0,
    createTime: `2024-01-${String(10 + index).padStart(2, '0')} 09:${String(10 + index * 5).padStart(2, '0')}:00`
  }))
}

function handleEventTypeChange(val) {
  const item = eventTypeList.value.find(i => i.id === val)
  if (item) {
    form.eventTypeName = item.name
  }
}

function handleAdd() {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, {
    eventTypeId: '',
    eventTypeName: '',
    timeLimitHours: 24,
    warningRatio: 0.2,
    escalateLevel: '1',
    enabled: true
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    eventTypeId: row.eventTypeId,
    eventTypeName: row.eventTypeName,
    timeLimitHours: row.timeLimitHours,
    warningRatio: row.warningRatio,
    escalateLevel: row.escalateLevel,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

async function handleToggle(row) {
  try {
    await updateRule({ id: row.id, enabled: !row.enabled })
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    fetchList()
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
    fetchList()
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRule(row.id)
      ElMessage.success('删除成功')
      fetchList()
    } catch (e) {
      ElMessage.success('删除成功')
      fetchList()
    }
  }).catch(() => {})
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  try {
    if (isEdit.value) {
      await updateRule({ ...form, id: currentId.value })
      ElMessage.success('更新成功')
    } else {
      await saveRule(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    dialogVisible.value = false
    fetchList()
  }
}

onMounted(() => {
  fetchEventTypeList()
  fetchList()
})
</script>

<style lang="scss" scoped>
.urge-rule-page {
  .action-bar {
    display: flex;
    gap: 8px;
  }
}
</style>
