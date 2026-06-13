<template>
  <div class="grid-tree">
    <el-input
      v-model="innerFilterText"
      placeholder="输入关键词搜索"
      clearable
      size="default"
      style="margin-bottom: 12px"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      :filter-node-method="filterNode"
      :expand-on-click-node="false"
      node-key="id"
      default-expand-all
      highlight-current
      @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <span class="custom-tree-node">
          <el-icon class="node-icon">
            <component :is="getLevelIcon(data.gridLevel)" />
          </el-icon>
          <span class="node-label">{{ node.label }}</span>
          <el-tag
            v-if="data.gridLevel"
            :type="getLevelTagType(data.gridLevel)"
            size="small"
            class="node-tag"
          >
            {{ getLevelName(data.gridLevel) }}
          </el-tag>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { Search, OfficeBuilding, HomeFilled, Grid } from '@element-plus/icons-vue'
import { getGridTree } from '@/api/grid'
import { ElMessage } from 'element-plus'

const props = defineProps({
  filterText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['node-click'])

const treeRef = ref(null)
const treeData = ref([])
const innerFilterText = ref('')

const treeProps = {
  label: 'gridName',
  children: 'children'
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

function getLevelIcon(level) {
  const icons = {
    1: OfficeBuilding,
    2: HomeFilled,
    3: Grid,
    4: Grid
  }
  return icons[level] || Grid
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

function filterNode(value, data) {
  if (!value) return true
  return data.gridName?.includes(value) || data.gridCode?.includes(value)
}

function handleNodeClick(data) {
  emit('node-click', data)
}

async function loadTreeData() {
  try {
    const res = await getGridTree()
    treeData.value = res.data || []
  } catch (e) {
    ElMessage.error('加载网格树失败')
  }
}

watch(() => props.filterText, (val) => {
  innerFilterText.value = val
  treeRef.value?.filter(val)
})

watch(innerFilterText, (val) => {
  treeRef.value?.filter(val)
})

function setCurrentNode(id) {
  treeRef.value?.setCurrentKey(id)
}

function getCurrentNode() {
  return treeRef.value?.getCurrentNode()
}

onMounted(() => {
  loadTreeData()
})

defineExpose({
  loadTreeData,
  setCurrentNode,
  getCurrentNode
})
</script>

<style lang="scss" scoped>
.grid-tree {
  padding: 12px;
  background: #fff;
  border-radius: 4px;

  .custom-tree-node {
    flex: 1;
    display: flex;
    align-items: center;
    font-size: 14px;
    padding-right: 8px;

    .node-icon {
      margin-right: 6px;
      color: #409eff;
    }

    .node-label {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .node-tag {
      margin-left: 8px;
    }
  }
}
</style>
