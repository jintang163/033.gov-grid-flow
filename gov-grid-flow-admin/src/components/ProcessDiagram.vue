<template>
  <div class="process-diagram">
    <div v-if="loading" class="diagram-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载流程图...</span>
    </div>
    <div v-else-if="diagramUrl" class="diagram-container">
      <img :src="diagramUrl" alt="流程图" class="diagram-image" @click="handlePreview" />
      <div class="diagram-actions">
        <el-button type="primary" size="small" @click="handlePreview">
          <el-icon><ZoomIn /></el-icon>
          放大查看
        </el-button>
        <el-button size="small" @click="refreshDiagram">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
    <el-empty v-else description="暂无流程图数据" />
    <el-dialog v-model="previewVisible" title="流程图预览" width="80%" top="5vh">
      <div class="preview-container">
        <img v-if="diagramUrl" :src="diagramUrl" alt="流程图" class="preview-image" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getEventProcessDiagram } from '@/api/event'
import { ElMessage } from 'element-plus'

const props = defineProps({
  processInstanceId: {
    type: String,
    default: ''
  }
})

const loading = ref(false)
const diagramUrl = ref('')
const previewVisible = ref(false)

async function loadDiagram() {
  if (!props.processInstanceId) {
    diagramUrl.value = ''
    return
  }
  loading.value = true
  try {
    const res = await getEventProcessDiagram(props.processInstanceId)
    if (res instanceof Blob) {
      const reader = new FileReader()
      reader.onload = e => {
        diagramUrl.value = e.target.result
      }
      reader.readAsDataURL(res)
    } else if (res.data) {
      if (typeof res.data === 'string' && res.data.startsWith('data:')) {
        diagramUrl.value = res.data
      } else if (typeof res.data === 'string') {
        diagramUrl.value = `data:image/png;base64,${res.data}`
      } else if (res.data instanceof Blob) {
        const reader = new FileReader()
        reader.onload = e => {
          diagramUrl.value = e.target.result
        }
        reader.readAsDataURL(res.data)
      }
    }
  } catch (error) {
    console.error('加载流程图失败:', error)
    ElMessage.warning('流程图加载失败')
    diagramUrl.value = ''
  } finally {
    loading.value = false
  }
}

function refreshDiagram() {
  loadDiagram()
}

function handlePreview() {
  if (diagramUrl.value) {
    previewVisible.value = true
  }
}

watch(() => props.processInstanceId, () => {
  loadDiagram()
})

onMounted(() => {
  loadDiagram()
})
</script>

<style lang="scss" scoped>
.process-diagram {
  .diagram-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    gap: 12px;
    color: #909399;

    .el-icon {
      color: #409EFF;
    }
  }

  .diagram-container {
    text-align: center;

    .diagram-image {
      max-width: 100%;
      max-height: 300px;
      cursor: pointer;
      border: 1px solid #ebeef5;
      border-radius: 4px;
      padding: 8px;
      background: #fff;
    }

    .diagram-actions {
      margin-top: 12px;
      display: flex;
      justify-content: center;
      gap: 10px;
    }
  }

  .preview-container {
    text-align: center;
    overflow: auto;

    .preview-image {
      max-width: 100%;
      height: auto;
    }
  }
}
</style>
