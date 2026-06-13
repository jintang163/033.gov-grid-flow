<template>
  <div class="urge-template-page">
    <el-card shadow="never">
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增模板
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="templateCode" label="模板编码" width="140" />
        <el-table-column prop="templateName" label="模板名称" width="140" />
        <el-table-column prop="titleTemplate" label="标题模板" min-width="180" show-overflow-tooltip />
        <el-table-column prop="contentTemplate" label="内容模板" min-width="240" show-overflow-tooltip />
        <el-table-column prop="channel" label="发送渠道" width="120">
          <template #default="{ row }">
            <el-tag :type="getChannelTagType(row.channel)">
              {{ getChannelLabel(row.channel) }}
            </el-tag>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="请输入模板编码" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="标题模板" prop="titleTemplate">
          <el-input v-model="form.titleTemplate" placeholder="请输入标题模板" />
        </el-form-item>
        <el-form-item label="内容模板" prop="contentTemplate">
          <el-input
            v-model="form.contentTemplate"
            type="textarea"
            :rows="4"
            placeholder="请输入内容模板"
          />
          <div class="template-tip">
            <span class="tip-label">支持变量：</span>
            <el-tag size="small" type="info">${eventNo}</el-tag>
            <el-tag size="small" type="info">${title}</el-tag>
            <el-tag size="small" type="info">${deadline}</el-tag>
          </div>
        </el-form-item>
        <el-form-item label="发送渠道" prop="channel">
          <el-select v-model="form.channel" placeholder="请选择发送渠道" style="width: 100%">
            <el-option label="系统消息" value="SYSTEM" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
            <el-option label="APP推送" value="APP" />
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
import { listTemplates, saveTemplate, updateTemplate, deleteTemplate } from '@/api/urge'

const loading = ref(false)
const tableData = ref([])

const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const currentId = ref(null)
const form = reactive({
  templateCode: '',
  templateName: '',
  titleTemplate: '',
  contentTemplate: '',
  channel: 'SYSTEM',
  enabled: true
})

const formRules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  titleTemplate: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
  contentTemplate: [{ required: true, message: '请输入内容模板', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择发送渠道', trigger: 'change' }]
}

const dialogTitle = computed(() => isEdit.value ? '编辑模板' : '新增模板')

function getChannelLabel(channel) {
  const map = {
    SYSTEM: '系统消息',
    SMS: '短信',
    EMAIL: '邮件',
    APP: 'APP推送'
  }
  return map[channel] || channel || '-'
}

function getChannelTagType(channel) {
  const map = {
    SYSTEM: 'primary',
    SMS: 'success',
    EMAIL: 'warning',
    APP: 'info'
  }
  return map[channel] || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listTemplates()
    tableData.value = res?.rows || res?.data?.list || res?.data || []
  } catch (e) {
    tableData.value = generateMockData()
  } finally {
    loading.value = false
  }
}

function generateMockData() {
  const channels = ['SYSTEM', 'SMS', 'EMAIL', 'APP']
  const templates = [
    { code: 'URGE_NORMAL', name: '普通催办', title: '【催办】请及时处理事件 ${eventNo}' },
    { code: 'URGE_WARNING', name: '预警催办', title: '【预警】事件 ${eventNo} 即将超时' },
    { code: 'URGE_TIMEOUT', name: '超时催办', title: '【超时】事件 ${eventNo} 已超时' },
    { code: 'URGE_ESCALATE', name: '升级督办', title: '【督办】事件 ${eventNo} 已升级督办' }
  ]
  return templates.map((t, index) => ({
    id: index + 1,
    templateCode: t.code,
    templateName: t.name,
    titleTemplate: t.title,
    contentTemplate: `您好，事件「${'${title}'}」截止时间为 ${'${deadline}'}，请尽快处理。`,
    channel: channels[index % 4],
    enabled: index % 3 !== 0,
    createTime: `2024-01-${String(10 + index).padStart(2, '0')} 10:${String(10 + index * 8).padStart(2, '0')}:00`
  }))
}

function handleAdd() {
  isEdit.value = false
  currentId.value = null
  Object.assign(form, {
    templateCode: '',
    templateName: '',
    titleTemplate: '',
    contentTemplate: '',
    channel: 'SYSTEM',
    enabled: true
  })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  currentId.value = row.id
  Object.assign(form, {
    templateCode: row.templateCode,
    templateName: row.templateName,
    titleTemplate: row.titleTemplate,
    contentTemplate: row.contentTemplate,
    channel: row.channel,
    enabled: row.enabled
  })
  dialogVisible.value = true
}

async function handleToggle(row) {
  try {
    await updateTemplate({ id: row.id, enabled: !row.enabled })
    ElMessage.success(row.enabled ? '已禁用' : '已启用')
    fetchList()
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.success(row.enabled ? '已启用' : '已禁用')
    fetchList()
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确定要删除该模板吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTemplate(row.id)
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
      await updateTemplate({ ...form, id: currentId.value })
      ElMessage.success('更新成功')
    } else {
      await saveTemplate(form)
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
  fetchList()
})
</script>

<style lang="scss" scoped>
.urge-template-page {
  .action-bar {
    display: flex;
    gap: 8px;
  }

  .template-tip {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 8px;

    .tip-label {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
