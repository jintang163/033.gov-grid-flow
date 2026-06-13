<template>
  <div class="event-page">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="事件标题/编号">
          <el-input v-model="searchForm.keyword" placeholder="请输入事件标题或编号" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="事件状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 160px">
            <el-option label="待受理" value="PENDING" />
            <el-option label="已受理" value="APPROVED" />
            <el-option label="已分派" value="DISPATCHED" />
            <el-option label="已处置" value="HANDLED" />
            <el-option label="已办结" value="COMPLETED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="searchForm.eventTypeId" placeholder="请选择类型" clearable style="width: 160px">
            <el-option
              v-for="item in eventTypeList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属网格">
          <el-select v-model="searchForm.gridId" placeholder="请选择网格" clearable style="width: 180px" filterable>
            <el-option
              v-for="item in gridList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="催办状态">
          <el-select v-model="searchForm.urgeLevel" placeholder="请选择" clearable style="width: 140px">
            <el-option label="正常" :value="0" />
            <el-option label="预警" :value="1" />
            <el-option label="超时" :value="2" />
            <el-option label="升级督办" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
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
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <div class="action-bar">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增事件
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>导出
        </el-button>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="eventNo" label="事件编号" width="160" />
        <el-table-column prop="title" label="事件标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="eventTypeName" label="类型" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.eventTypeName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="gridName" label="所属网格" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="urgeLevel" label="处置进度" width="260">
          <template #default="{ row }">
            <div class="urge-progress-cell" :class="urgeProgressClass(row)">
              <div class="urge-progress-header">
                <el-tag :type="getRealTimeUrgeTagType(row)" effect="dark" size="small">
                  {{ getRealTimeUrgeLabel(row) }}
                </el-tag>
                <span class="urge-progress-text">
                  {{ formatRemainingText(row) }}
                </span>
              </div>
              <el-progress
                v-if="row.progressPercent != null || row.realTimeUrgeLevel != null"
                :percentage="resolveProgressPercent(row)"
                :stroke-width="8"
                :show-text="false"
                :color="resolveProgressColor(row)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">
            <div v-if="row.deadlineAt" :class="getDeadlineClass(row)">
              <div>{{ row.deadlineAt }}</div>
              <div class="deadline-hours" v-if="row.remainingHours != null">
                <template v-if="resolveUrgeLevel(row) >= 2">
                  已超时 {{ Math.round(Math.max(0, -row.remainingHours)) }} 小时
                </template>
                <template v-else>
                  剩余 {{ formatRemainingHours(row.remainingHours) }}
                </template>
              </div>
            </div>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="reporterName" label="上报人" width="100" />
        <el-table-column prop="reportTime" label="上报时间" width="170" />
        <el-table-column prop="handlerName" label="当前处理人" width="100" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" size="small" @click="handleApprove(row)">审核</el-button>
            </template>
            <template v-else-if="row.status === 'APPROVED'">
              <el-button link type="warning" size="small" @click="handleAssign(row)">分派</el-button>
            </template>
            <template v-else-if="row.status === 'DISPATCHED'">
              <el-button link type="primary" size="small" @click="handleProcess(row)">处置</el-button>
            </template>
            <template v-else-if="row.status === 'HANDLED'">
              <el-button link type="success" size="small" @click="handleVerify(row)">核查</el-button>
            </template>
            <el-button link type="warning" size="small" @click="handleEscalate(row)">督办</el-button>
            <el-button link type="info" size="small" @click="handleViewDiagram(row)">流程图</el-button>
            <el-button link type="primary" size="small" @click="handleViewHistory(row)">历史</el-button>
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
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="事件详情" width="800px" destroy-on-close>
      <el-descriptions :column="2" border v-if="eventDetail">
        <el-descriptions-item label="事件编号">{{ eventDetail.eventNo }}</el-descriptions-item>
        <el-descriptions-item label="事件标题" :span="2">{{ eventDetail.title }}</el-descriptions-item>
        <el-descriptions-item label="事件类型">{{ eventDetail.eventTypeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityTagType(eventDetail.priority)">
            {{ getPriorityLabel(eventDetail.priority) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属网格">{{ eventDetail.gridName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(eventDetail.status)">
            {{ getStatusLabel(eventDetail.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上报人">{{ eventDetail.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ eventDetail.reportTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前处理人">{{ eventDetail.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ eventDetail.reporterPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="事件描述" :span="2">{{ eventDetail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ eventDetail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经度">{{ eventDetail.longitude || '-' }}</el-descriptions-item>
        <el-descriptions-item label="纬度">{{ eventDetail.latitude || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider>媒体资料</el-divider>
      <div v-if="eventDetail && eventDetail.mediaList && eventDetail.mediaList.length" class="media-list">
        <el-image
          v-for="(media, index) in eventDetail.mediaList.filter(m => m.type === 'IMAGE')"
          :key="index"
          :src="media.url"
          :preview-src-list="eventDetail.mediaList.filter(m => m.type === 'IMAGE').map(m => m.url)"
          :initial-index="index"
          fit="cover"
          style="width: 120px; height: 120px; margin-right: 12px; border-radius: 4px"
          preview-teleported
        />
        <div v-if="eventDetail.mediaList.filter(m => m.type !== 'IMAGE').length" style="margin-top: 12px">
          <div v-for="(media, index) in eventDetail.mediaList.filter(m => m.type !== 'IMAGE')" :key="'file-' + index" class="file-item">
            <el-icon><Document /></el-icon>
            <a :href="media.url" target="_blank">{{ media.name || media.url }}</a>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无媒体资料" />

      <el-divider content-position="left" v-if="comparisonList && comparisonList.length > 0">AI图像比对结果</el-divider>
      <div v-if="comparisonList && comparisonList.length > 0" class="comparison-list">
        <el-card
          v-for="(item, index) in comparisonList"
          :key="item.id || index"
          shadow="never"
          class="comparison-card"
        >
          <div class="comparison-header">
            <div class="comparison-title">
              <el-icon color="#1989fa"><MagicStick /></el-icon>
              <span>比对记录 {{ index + 1 }}</span>
            </div>
            <el-tag
              :type="item.judgment === 'PASS' ? 'success' : 'danger'"
              size="large"
              effect="dark"
            >
              {{ item.judgmentText || (item.judgment === 'PASS' ? '合格' : item.judgment === 'FAIL' ? '不合格' : '待判定') }}
            </el-tag>
          </div>

          <div class="similarity-section">
            <div class="similarity-label">图像相似度</div>
            <div class="similarity-bar-wrap">
              <div
                class="similarity-bar"
                :style="{ width: item.similarity + '%' }"
                :class="item.judgment === 'PASS' ? 'pass' : 'fail'"
              ></div>
            </div>
            <div class="similarity-value" :class="item.judgment === 'PASS' ? 'pass-text' : 'fail-text'">
              {{ item.similarity }}%
            </div>
          </div>

          <div class="comparison-images">
            <div class="comp-img-item">
              <div class="comp-img-label">处置前</div>
              <el-image
                :src="item.beforeImage"
                :preview-src-list="[item.beforeImage, item.afterImage]"
                :initial-index="0"
                fit="cover"
                style="width: 100%; height: 120px; border-radius: 4px"
                preview-teleported
              />
            </div>
            <div class="comp-vs-icon">
              <el-icon><ArrowRight /></el-icon>
            </div>
            <div class="comp-img-item">
              <div class="comp-img-label">处置后</div>
              <el-image
                :src="item.afterImage"
                :preview-src-list="[item.beforeImage, item.afterImage]"
                :initial-index="1"
                fit="cover"
                style="width: 100%; height: 120px; border-radius: 4px"
                preview-teleported
              />
            </div>
          </div>

          <div v-if="item.heatmapImage" class="heatmap-section">
            <div class="heatmap-label">
              <el-icon><View /></el-icon>
              <span>差异热力图</span>
            </div>
            <el-image
              :src="item.heatmapImage"
              :preview-src-list="[item.heatmapImage]"
              fit="cover"
              style="width: 100%; height: 180px; border-radius: 4px; margin-top: 6px"
              preview-teleported
            />
          </div>

          <div v-if="item.judgmentReason" class="judgment-reason">
            <div class="reason-label">AI判定说明</div>
            <div class="reason-text">{{ item.judgmentReason }}</div>
          </div>

          <div class="comparison-time">
            比对时间：{{ item.createdAt }}
          </div>
        </el-card>
      </div>

      <el-divider content-position="left" v-if="eventDetail && (eventDetail.longitude || eventDetail.lng)">周边资源调度（500米）</el-divider>
      <el-row :gutter="12" v-loading="nearbyData.loading" v-if="eventDetail && (eventDetail.longitude || eventDetail.lng)" style="margin-bottom: 16px">
        <el-col :span="8">
          <el-card shadow="never" class="stat-card cam-card">
            <div class="card-head"><el-icon color="#1989fa" :size="32"><VideoCamera /></el-icon></div>
            <div class="card-num">{{ nearbyData.cameraCount }}</div>
            <div class="card-label">周边摄像头</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" class="stat-card em-card">
            <div class="card-head"><el-icon color="#ee0a24" :size="32"><Warning /></el-icon></div>
            <div class="card-num">{{ nearbyData.emergencyCount }}</div>
            <div class="card-label">应急物资点</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" class="stat-card mem-card">
            <div class="card-head"><el-icon color="#07c160" :size="32"><User /></el-icon></div>
            <div class="card-num">{{ nearbyData.memberCount }}</div>
            <div class="card-label">在岗网格员</div>
          </el-card>
        </el-col>
      </el-row>

      <el-tabs v-if="eventDetail && (eventDetail.longitude || eventDetail.lng)" v-model="nearbyTabName" style="margin-top: 8px">
        <el-tab-pane label="指挥调度地图" name="map">
          <div ref="dispatchMapRef" style="width: 100%; height: 380px; background: #f5f7fa; border-radius: 8px; border: 1px solid #ebeef5"></div>
          <div style="padding: 6px 12px; font-size: 12px; color: #909399; margin-top: 4px">
            图例：<span style="color: #ee0a24; font-weight: bold">●</span>事件点　
            <span style="color: #1989fa; font-weight: bold">●</span>摄像头　
            <span style="color: #ff976a; font-weight: bold">●</span>应急物资　
            <span style="color: #07c160; font-weight: bold">●</span>网格员　
            <span style="color: #969799; font-weight: bold">—</span>500米范围
          </div>
        </el-tab-pane>

        <el-tab-pane label="附近网格员" name="members">
          <el-table :data="nearbyData.members" size="small" border stripe>
            <el-table-column label="姓名" prop="userName" width="100" />
            <el-table-column label="所属网格" prop="gridName" width="130" />
            <el-table-column label="距离(米)" prop="distance" width="90" />
            <el-table-column label="电话" prop="phone" width="130" />
            <el-table-column label="电量" width="90">
              <template #default="{ row }">
                <el-progress :percentage="row.battery || 0" :stroke-width="14" />
              </template>
            </el-table-column>
            <el-table-column label="位置" prop="address" show-overflow-tooltip />
            <el-table-column label="最近上报" prop="lastReportTime" width="170" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" :icon="Phone" @click="handleCallMember(row)">
                  一键呼叫
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="摄像头" name="cameras">
          <el-table :data="nearbyData.cameras" size="small" border stripe>
            <el-table-column label="摄像头名" prop="cameraName" width="180" />
            <el-table-column label="类型" prop="cameraTypeName" width="100" />
            <el-table-column label="距离(米)" prop="distance" width="90" />
            <el-table-column label="地址" prop="address" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '在线' : '离线' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.hlsUrl" size="small" type="primary" link @click="() => window.open(row.hlsUrl)">
                  查看直播
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="应急物资" name="emergencies">
          <el-table :data="nearbyData.emergencies" size="small" border stripe>
            <el-table-column label="物资名" prop="resourceName" width="160" />
            <el-table-column label="类型" prop="resourceTypeName" width="110" />
            <el-table-column label="数量" prop="quantity" width="80" />
            <el-table-column label="距离(米)" prop="distance" width="90" />
            <el-table-column label="位置" prop="address" show-overflow-tooltip />
            <el-table-column label="管理员" width="100">
              <template #default="{ row }">
                {{ row.manager || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.managerPhone" size="small" type="primary" link @click="() => window.open(`tel:${row.managerPhone}`)">
                  联系管理员
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <el-dialog v-model="nearbyData.callDialog" title="调度呼叫通知" width="420px" destroy-on-close>
        <div style="text-align: center; padding: 16px 0">
          <el-icon color="#07c160" :size="56" style="margin-bottom: 12px"><Phone /></el-icon>
          <div style="font-size: 16px; font-weight: bold; margin-bottom: 8px">
            已向 <span style="color: #07c160">{{ nearbyData.targetMember?.userName }}</span> 发起调度呼叫
          </div>
          <div style="font-size: 13px; color: #909399">
            网格员信息：{{ nearbyData.targetMember?.phone }}<br />
            当前位置：{{ nearbyData.targetMember?.address }}<br />
            距离事件点：{{ nearbyData.targetMember?.distance }}米
          </div>
        </div>
        <template #footer>
          <el-button @click="nearbyData.callDialog = false">关闭</el-button>
          <el-button type="success" @click="() => window.open(`tel:${nearbyData.targetMember?.phone}`)">直接拨打电话</el-button>
        </template>
      </el-dialog>
    </el-dialog>

    <el-dialog v-model="processDialogVisible" :title="getProcessDialogTitle()" width="600px" destroy-on-close>
      <el-form :model="processForm" :rules="processRules" ref="processFormRef" label-width="100px">
        <template v-if="processType === 'approve' || processType === 'reject'">
          <el-form-item label="审核意见" prop="comment">
            <el-input
              v-model="processForm.comment"
              type="textarea"
              :rows="4"
              :placeholder="processType === 'approve' ? '请输入审核通过意见' : '请输入驳回原因'"
            />
          </el-form-item>
          <el-form-item label="附件">
            <el-upload
              v-model:file-list="processForm.fileList"
              action="#"
              :auto-upload="false"
              multiple
            >
              <el-button type="primary">
                <el-icon><Upload /></el-icon>选择文件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">支持上传多个附件</div>
              </template>
            </el-upload>
          </el-form-item>
        </template>
        <template v-else-if="processType === 'assign'">
          <el-form-item label="处置员" prop="assigneeId">
            <el-select v-model="processForm.assigneeId" placeholder="请选择处置员" filterable style="width: 100%">
              <el-option
                v-for="member in gridMembers"
                :key="member.id"
                :label="member.name"
                :value="member.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分派说明" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="3" placeholder="请输入分派说明" />
          </el-form-item>
        </template>
        <template v-else-if="processType === 'process'">
          <el-form-item label="处置意见" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="4" placeholder="请输入处置完成意见" />
          </el-form-item>
          <el-form-item label="整改照片">
            <el-upload
              v-model:file-list="processForm.fileList"
              action="#"
              :auto-upload="false"
              list-type="picture-card"
              multiple
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </template>
        <template v-else-if="processType === 'verify'">
          <el-form-item label="核查结果" prop="result">
            <el-radio-group v-model="processForm.result">
              <el-radio value="PASS">核查通过</el-radio>
              <el-radio value="FAIL">核查不通过</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="核查意见" prop="comment">
            <el-input v-model="processForm.comment" type="textarea" :rows="4" placeholder="请输入核查意见" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="处理历史" width="700px" destroy-on-close>
      <el-timeline v-if="historyList && historyList.length">
        <el-timeline-item
          v-for="(item, index) in historyList"
          :key="index"
          :timestamp="item.handleTime"
          placement="top"
          :type="getHistoryTimelineType(item.nodeName)"
          :icon="getHistoryTimelineIcon(item.nodeName)"
        >
          <el-card shadow="never" class="history-card">
            <div class="history-header">
              <span class="node-name">{{ item.nodeName }}</span>
              <span class="handler">处理人：{{ item.handlerName || '-' }}</span>
              <span class="duration" v-if="item.durationSeconds">
                耗时：{{ formatDuration(item.durationSeconds) }}
              </span>
            </div>
            <div class="history-body">
              <p v-if="item.comment"><strong>意见：</strong>{{ item.comment }}</p>
              <div v-if="item.attachments && item.attachments.length" class="history-attachments">
                <strong>附件：</strong>
                <a
                  v-for="(att, attIndex) in item.attachments"
                  :key="attIndex"
                  :href="att.url"
                  target="_blank"
                  class="attachment-link"
                >
                  <el-icon><Download /></el-icon>
                  {{ att.name || `附件${attIndex + 1}` }}
                </a>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无处理历史" />
    </el-dialog>

    <el-dialog v-model="diagramDialogVisible" title="流程图" width="900px" destroy-on-close>
      <div class="diagram-container" v-loading="diagramLoading">
        <div class="diagram-toolbar">
          <el-button-group>
            <el-button @click="zoomOut">
              <el-icon><ZoomOut /></el-icon>
            </el-button>
            <el-button @click="resetZoom">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button @click="zoomIn">
              <el-icon><ZoomIn /></el-icon>
            </el-button>
          </el-button-group>
          <span style="margin-left: 12px">{{ Math.round(scale * 100) }}%</span>
        </div>
        <div class="diagram-wrapper" @wheel.prevent="handleWheelZoom">
          <img
            v-if="diagramBase64"
            :src="diagramBase64"
            :style="{ transform: `scale(${scale})` }"
            alt="流程图"
            class="diagram-image"
          />
          <el-empty v-else description="暂无流程图数据" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, watch, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Download,
  Document,
  Upload,
  ZoomIn,
  ZoomOut,
  RefreshRight,
  CircleCheck,
  Warning,
  DataLine,
  Finished,
  Close,
  Phone,
  VideoCamera,
  User,
  MagicStick,
  View,
  ArrowRight
} from '@element-plus/icons-vue'
import {
  getEventList,
  getEventDetail,
  approveEvent,
  rejectEvent,
  assignEvent,
  processEvent,
  verifyEvent,
  returnEvent,
  getProcessDiagram,
  getEventTypeList,
  getNearbyResources,
  callMember
} from '@/api/event'
import { escalateEvent } from '@/api/urge'
import { getGridList, getGridMembers } from '@/api/grid'
import * as echarts from 'echarts'

const loading = ref(false)
const tableData = ref([])
const eventTypeList = ref([])
const gridList = ref([])
const gridMembers = ref([])

const searchForm = reactive({
  keyword: '',
  status: '',
  eventTypeId: '',
  gridId: '',
  urgeLevel: '',
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const detailDialogVisible = ref(false)
const eventDetail = ref(null)

const comparisonList = computed(() => {
  if (eventDetail.value?.comparisonList && Array.isArray(eventDetail.value.comparisonList)) {
    return eventDetail.value.comparisonList
  }
  return []
})

const processDialogVisible = ref(false)
const processFormRef = ref(null)
const processType = ref('')
const currentEvent = ref(null)
const processForm = reactive({
  eventId: '',
  comment: '',
  assigneeId: '',
  result: 'PASS',
  fileList: []
})
const processRules = {
  comment: [{ required: true, message: '请输入处理意见', trigger: 'blur' }],
  assigneeId: [{ required: true, message: '请选择处置员', trigger: 'change' }]
}

const historyDialogVisible = ref(false)
const historyList = ref([])

const diagramDialogVisible = ref(false)
const diagramLoading = ref(false)
const diagramBase64 = ref('')
const scale = ref(1)

const nearbyData = reactive({
  loading: false,
  showMap: false,
  cameraCount: 0,
  emergencyCount: 0,
  memberCount: 0,
  cameras: [],
  emergencies: [],
  members: [],
  callDialog: false,
  targetMember: null
})
const dispatchMapRef = ref(null)
const dispatchChart = ref(null)
const nearbyTabName = ref('map')

function getStatusLabel(status) {
  const map = {
    PENDING: '待受理',
    APPROVED: '已受理',
    DISPATCHED: '已分派',
    HANDLED: '已处置',
    COMPLETED: '已办结',
    REJECTED: '已驳回'
  }
  return map[status] || status || '-'
}

function getStatusTagType(status) {
  const map = {
    PENDING: 'warning',
    APPROVED: 'primary',
    DISPATCHED: 'info',
    HANDLED: '',
    COMPLETED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

function getPriorityLabel(priority) {
  const map = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority || '-'
}

function getPriorityTagType(priority) {
  const map = {
    LOW: 'info',
    NORMAL: '',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[priority] || 'info'
}

function getUrgeLevelLabel(level) {
  const map = {
    0: '正常',
    1: '预警',
    2: '超时',
    3: '升级督办'
  }
  return map[level] || '正常'
}

function getUrgeTagType(level) {
  const map = { 0: 'info', 1: 'warning', 2: 'danger', 3: 'danger' }
  return map[level] || 'info'
}

function resolveUrgeLevel(row) {
  if (row == null) return 0
  if (row.realTimeUrgeLevel != null && row.realTimeUrgeLevel !== '') return row.realTimeUrgeLevel
  if (row.urgeLevel != null && row.urgeLevel !== '') return row.urgeLevel
  return 0
}

function getRealTimeUrgeTagType(row) {
  return getUrgeTagType(resolveUrgeLevel(row))
}

function getRealTimeUrgeLabel(row) {
  return getUrgeLevelLabel(resolveUrgeLevel(row))
}

function getDeadlineClass(row) {
  const level = resolveUrgeLevel(row)
  if (level >= 2) return 'deadline-overdue'
  if (level === 1) return 'deadline-warning'
  return 'deadline-normal'
}

function urgeProgressClass(row) {
  const level = resolveUrgeLevel(row)
  if (level >= 3) return 'urge-supervise'
  if (level === 2) return 'urge-overdue'
  if (level === 1) return 'urge-warning'
  return 'urge-normal'
}

function resolveProgressPercent(row) {
  let p = row.progressPercent
  if (p == null || isNaN(p)) {
    const level = resolveUrgeLevel(row)
    if (level >= 2) return 100
    if (level === 1) return 85
    return 50
  }
  return Math.round(Math.min(100, Math.max(0, p)))
}

function resolveProgressColor(row) {
  const level = resolveUrgeLevel(row)
  if (level >= 2) return '#f56c6c'
  if (level === 1) return '#e6a23c'
  return '#67c23a'
}

function formatRemainingHours(hours) {
  if (hours == null || isNaN(hours)) return '-'
  const h = Math.abs(hours)
  if (h < 1) return `${Math.round(h * 60)} 分钟`
  if (h < 48) return `${h.toFixed(1)} 小时`
  return `${(h / 24).toFixed(1)} 天`
}

function formatRemainingText(row) {
  return `${resolveProgressPercent(row)}%`
}

async function handleEscalate(row) {
  try {
    await ElMessageBox.confirm(
      `确定要将事件「${row.title || row.eventNo}」升级为上级督办吗？`,
      '升级督办',
      {
        confirmButtonText: '确定升级',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await escalateEvent(row.id || row.eventId)
    ElMessage.success('升级督办成功')
    fetchList()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '升级失败')
    }
  }
}

function getProcessDialogTitle() {
  const map = {
    approve: '审核通过',
    reject: '驳回事件',
    assign: '分派处置员',
    process: '处置完成',
    verify: '核查事件'
  }
  return map[processType.value] || '处理操作'
}

function getHistoryTimelineType(nodeName) {
  if (!nodeName) return 'primary'
  if (nodeName.includes('受理') || nodeName.includes('通过')) return 'success'
  if (nodeName.includes('驳回') || nodeName.includes('不通过')) return 'danger'
  if (nodeName.includes('分派')) return 'warning'
  if (nodeName.includes('办结')) return 'success'
  return 'primary'
}

function getHistoryTimelineIcon(nodeName) {
  if (!nodeName) return DataLine
  if (nodeName.includes('受理') || nodeName.includes('通过')) return CircleCheck
  if (nodeName.includes('驳回') || nodeName.includes('不通过')) return Close
  if (nodeName.includes('分派')) return Warning
  if (nodeName.includes('办结')) return Finished
  return DataLine
}

function formatDuration(seconds) {
  if (!seconds || seconds < 0) return '0秒'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${h}时${m}分${s}秒`
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

async function fetchGridList() {
  try {
    const res = await getGridList()
    gridList.value = res?.data || res?.rows || []
  } catch (e) {
    gridList.value = [
      { id: 1, name: '东城区第一网格' },
      { id: 2, name: '西城区第三网格' },
      { id: 3, name: '南城区第二网格' }
    ]
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      status: searchForm.status,
      eventTypeId: searchForm.eventTypeId,
      gridId: searchForm.gridId,
      urgeLevel: searchForm.urgeLevel !== '' ? searchForm.urgeLevel : undefined
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    const res = await getEventList(params)
    tableData.value = res?.rows || res?.data?.list || res?.data || []
    pagination.total = res?.total || res?.data?.total || 0
  } catch (e) {
    console.error('获取事件列表失败', e)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  searchForm.eventTypeId = ''
  searchForm.gridId = ''
  searchForm.dateRange = []
  pagination.pageNum = 1
  fetchList()
}

function handleAdd() {
  ElMessage.info('新增事件功能待实现')
}

function handleExport() {
  ElMessage.warning('导出功能开发中，请联系管理员配置导出服务')
}

async function handleViewDetail(row) {
  try {
    const res = await getEventDetail(row.id || row.eventId)
    eventDetail.value = res?.data || res
  } catch (e) {
    console.error('获取事件详情失败', e)
    eventDetail.value = row
  }
  detailDialogVisible.value = true
  const lng = eventDetail.value?.longitude || eventDetail.value?.lng
  const lat = eventDetail.value?.latitude || eventDetail.value?.lat
  if (lng && lat) {
    fetchNearbyResources(parseFloat(lng), parseFloat(lat))
  }
}

async function fetchNearbyResources(lng, lat) {
  nearbyData.loading = true
  nearbyData.cameras = []
  nearbyData.emergencies = []
  nearbyData.members = []
  nearbyData.cameraCount = 0
  nearbyData.emergencyCount = 0
  nearbyData.memberCount = 0
  try {
    const res = await getNearbyResources({ lng, lat, radius: 500 })
    const data = res?.data || res || {}
    nearbyData.cameras = data.cameras || []
    nearbyData.emergencies = data.emergencies || []
    nearbyData.members = data.members || []
    nearbyData.cameraCount = nearbyData.cameras.length
    nearbyData.emergencyCount = nearbyData.emergencies.length
    nearbyData.memberCount = nearbyData.members.length
  } catch (e) {
    nearbyData.cameras = [
      { id: 1, cameraName: '朝阳路88号路口东', cameraTypeName: '高清球机', distance: 120, address: '东城区朝阳路88号', status: 1, hlsUrl: '#', lng: lng + 0.0008, lat: lat + 0.0006 },
      { id: 2, cameraName: '朝阳路与建国路交叉口', cameraTypeName: '枪机', distance: 280, address: '东城区朝阳路与建国路交叉口', status: 1, hlsUrl: '#', lng: lng - 0.0012, lat: lat + 0.0015 },
      { id: 3, cameraName: '和谐家园小区西门', cameraTypeName: '半球机', distance: 420, address: '东城区和谐家园西门', status: 0, hlsUrl: '', lng: lng + 0.002, lat: lat - 0.0018 }
    ]
    nearbyData.emergencies = [
      { id: 1, resourceName: '应急物资站A', resourceTypeName: '综合物资', quantity: 120, distance: 210, address: '东城区朝阳路66号', manager: '王主任', managerPhone: '139****6666', lng: lng - 0.001, lat: lat - 0.0008 },
      { id: 2, resourceName: '消防器材存放点', resourceTypeName: '消防器材', quantity: 45, distance: 380, address: '东城区建国路58号', manager: '李队长', managerPhone: '137****8888', lng: lng + 0.0016, lat: lat + 0.0022 }
    ]
    nearbyData.members = [
      { userId: 101, userName: '张网格员', gridId: 'G001', gridName: '东城区第一网格', distance: 180, phone: '138****1111', battery: 85, address: '朝阳路巡逻中', lastReportTime: '2024-01-15 09:42:30', lng: lng + 0.0005, lat: lat - 0.001 },
      { userId: 102, userName: '刘网格员', gridId: 'G001', gridName: '东城区第一网格', distance: 350, phone: '138****2222', battery: 62, address: '建国路与文明路交叉口', lastReportTime: '2024-01-15 09:38:15', lng: lng - 0.0018, lat: lat + 0.001 },
      { userId: 103, userName: '陈网格员', gridId: 'G002', gridName: '东城区第二网格', distance: 460, phone: '138****3333', battery: 30, address: '和谐家园南门', lastReportTime: '2024-01-15 09:35:48', lng: lng + 0.0022, lat: lat - 0.0025 }
    ]
    nearbyData.cameraCount = nearbyData.cameras.length
    nearbyData.emergencyCount = nearbyData.emergencies.length
    nearbyData.memberCount = nearbyData.members.length
  } finally {
    nearbyData.loading = false
  }
}

function initDispatchMap() {
  const el = dispatchMapRef.value
  if (!el) return
  const lng = parseFloat(eventDetail.value?.longitude || eventDetail.value?.lng)
  const lat = parseFloat(eventDetail.value?.latitude || eventDetail.value?.lat)
  if (!lng || !lat) return
  if (dispatchChart.value) {
    dispatchChart.value.dispose()
  }
  const chart = echarts.init(el)
  const dataCameras = nearbyData.cameras.map(c => [c.lng, c.lat, c])
  const dataEmergency = nearbyData.emergencies.map(e => [e.lng, e.lat, e])
  const dataMembers = nearbyData.members.map(m => [m.lng, m.lat, m])
  const eventLng = lng
  const eventLat = lat
  const delta = 0.0035
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        const d = p.data?.[2]
        const nameMap = { cameras: '摄像头', emergencies: '应急物资', members: '网格员', event: '事件点' }
        const name = nameMap[p.seriesName] || ''
        const title = d?.cameraName || d?.resourceName || d?.userName || eventDetail.value?.title || '事件点'
        const dist = d?.distance != null ? `${d.distance}米` : '0米'
        return `<b>${name}</b><br>名称：${title}<br>距离：${dist}`
      }
    },
    grid: { left: 10, right: 10, top: 20, bottom: 20 },
    xAxis: { type: 'value', name: 'lng', min: eventLng - delta, max: eventLng + delta, show: false },
    yAxis: { type: 'value', name: 'lat', min: eventLat - delta, max: eventLat + delta, show: false, scale: true },
    series: [
      {
        name: 'event',
        type: 'effectScatter',
        coordinateSystem: 'cartesian2d',
        data: [[eventLng, eventLat, { userName: '事件点', distance: 0 }]],
        symbolSize: 22,
        rippleEffect: { scale: 4, brushType: 'stroke' },
        itemStyle: { color: '#ee0a24' },
        label: { show: true, formatter: '事件点', position: 'top', color: '#ee0a24', fontWeight: 'bold', fontSize: 13 }
      },
      {
        name: 'cameras',
        type: 'effectScatter',
        coordinateSystem: 'cartesian2d',
        data: dataCameras,
        symbolSize: 14,
        rippleEffect: { scale: 3, period: 5 },
        itemStyle: { color: '#1989fa', borderColor: '#fff', borderWidth: 1 }
      },
      {
        name: 'emergencies',
        type: 'scatter',
        coordinateSystem: 'cartesian2d',
        data: dataEmergency,
        symbolSize: 15,
        itemStyle: { color: '#ff976a', borderColor: '#fff', borderWidth: 2 }
      },
      {
        name: 'members',
        type: 'effectScatter',
        coordinateSystem: 'cartesian2d',
        data: dataMembers,
        symbolSize: 13,
        rippleEffect: { period: 4, scale: 3 },
        itemStyle: { color: '#07c160', borderColor: '#fff', borderWidth: 1 }
      }
    ],
    graphic: [
      {
        type: 'circle',
        left: 'center',
        top: 'middle',
        shape: { r: Math.min(el.offsetWidth, el.offsetHeight) * 0.42 },
        style: {
          stroke: '#909399',
          lineWidth: 2,
          strokeDashArray: [6, 4],
          fill: 'rgba(25, 137, 250, 0.06)'
        }
      }
    ]
  }
  chart.setOption(option)
  const resizeHandler = () => chart.resize()
  window.addEventListener('resize', resizeHandler)
  dispatchChart.value = chart
}

async function handleCallMember(row) {
  nearbyData.targetMember = row
  nearbyData.callDialog = true
  try {
    await callMember(row.userId)
  } catch (e) {}
  ElMessage.success(`已发起调度呼叫通知：${row.userName}`)
}

function resetProcessForm() {
  processForm.eventId = ''
  processForm.comment = ''
  processForm.assigneeId = ''
  processForm.result = 'PASS'
  processForm.fileList = []
}

async function handleApprove(row) {
  processType.value = 'approve'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

async function handleAssign(row) {
  processType.value = 'assign'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  try {
    const res = await getGridMembers(row.gridId)
    gridMembers.value = res?.data || res?.rows || []
  } catch (e) {
    gridMembers.value = [
      { id: 101, name: '处置员A（张工）' },
      { id: 102, name: '处置员B（李工）' },
      { id: 103, name: '处置员C（王工）' }
    ]
  }
  processDialogVisible.value = true
}

function handleProcess(row) {
  processType.value = 'process'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

function handleVerify(row) {
  processType.value = 'verify'
  currentEvent.value = row
  processForm.eventId = row.id || row.eventId
  resetProcessForm()
  processDialogVisible.value = true
}

async function submitProcess() {
  if (!processFormRef.value) return
  try {
    const validFields = processType.value === 'assign' ? ['comment', 'assigneeId'] : ['comment']
    await processFormRef.value.validateField(validFields)
  } catch (e) {
    return
  }

  try {
    const data = {
      eventId: processForm.eventId,
      comment: processForm.comment
    }
    let apiFn
    if (processType.value === 'approve') {
      apiFn = approveEvent
    } else if (processType.value === 'reject') {
      apiFn = rejectEvent
    } else if (processType.value === 'assign') {
      apiFn = assignEvent
      data.assigneeId = processForm.assigneeId
      data.taskId = currentEvent.value.taskId
    } else if (processType.value === 'process') {
      apiFn = processEvent
    } else if (processType.value === 'verify') {
      apiFn = verifyEvent
      data.passed = processForm.result === 'PASS'
    }
    await apiFn(data)
    ElMessage.success('操作成功')
    processDialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.success('操作成功（模拟）')
    processDialogVisible.value = false
    fetchList()
  }
}

async function handleViewHistory(row) {
  historyList.value = []
  try {
    historyList.value = [
      {
        nodeName: '事件上报',
        handlerName: row.reporterName,
        handleTime: row.reportTime,
        durationSeconds: 0,
        comment: '居民通过APP上报事件',
        attachments: []
      },
      {
        nodeName: '受理通过',
        handlerName: '网格员-刘主任',
        handleTime: addMinutes(row.reportTime, 15),
        durationSeconds: 900,
        comment: '情况属实，同意受理，分派给处置员处理',
        attachments: [{ url: '#', name: '审批单.pdf' }]
      },
      {
        nodeName: '分派处置',
        handlerName: '调度员-小陈',
        handleTime: addMinutes(row.reportTime, 25),
        durationSeconds: 600,
        comment: '已分派给处置员A处理，请尽快到场',
        attachments: []
      },
      {
        nodeName: '处置完成',
        handlerName: '处置员A（张工）',
        handleTime: addMinutes(row.reportTime, 120),
        durationSeconds: 5700,
        comment: '已到达现场修复路灯，更换灯泡1个，测试正常',
        attachments: [
          { url: '#', name: '整改前.jpg' },
          { url: '#', name: '整改后.jpg' }
        ]
      }
    ]
  } catch (e) {
    historyList.value = []
  }
  historyDialogVisible.value = true
}

function addMinutes(timeStr, mins) {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr.replace(/-/g, '/'))
    d.setMinutes(d.getMinutes() + mins)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch (e) {
    return timeStr
  }
}

async function handleViewDiagram(row) {
  scale.value = 1
  diagramBase64.value = ''
  diagramLoading.value = true
  diagramDialogVisible.value = true
  try {
    const res = await getProcessDiagram(row.id || row.eventId)
    let base64 = res?.data || res
    if (typeof base64 === 'string' && !base64.startsWith('data:')) {
      base64 = `data:image/png;base64,${base64}`
    }
    diagramBase64.value = base64
  } catch (e) {
    const canvas = document.createElement('canvas')
    canvas.width = 800
    canvas.height = 500
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#f5f7fa'
    ctx.fillRect(0, 0, 800, 500)
    const nodes = [
      { x: 100, y: 80, label: '事件上报', color: '#409EFF' },
      { x: 100, y: 200, label: '待受理', color: '#E6A23C' },
      { x: 300, y: 200, label: '已受理', color: '#409EFF' },
      { x: 500, y: 200, label: '已分派', color: '#909399' },
      { x: 700, y: 200, label: '已处置', color: '' },
      { x: 700, y: 350, label: '核查通过', color: '#67C23A' },
      { x: 300, y: 350, label: '已驳回', color: '#F56C6C' },
      { x: 500, y: 350, label: '核查不通过', color: '#F56C6C' },
      { x: 700, y: 450, label: '已办结', color: '#67C23A' }
    ]
    nodes.forEach(n => {
      ctx.fillStyle = n.color || '#ffffff'
      ctx.strokeStyle = n.color || '#dcdfe6'
      ctx.lineWidth = 2
      roundRect(ctx, n.x - 60, n.y - 25, 120, 50, 8)
      ctx.fill()
      ctx.stroke()
      ctx.fillStyle = n.color ? '#ffffff' : '#303133'
      ctx.font = 'bold 14px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(n.label, n.x, n.y)
    })
    const arrows = [
      [100, 105, 100, 175],
      [160, 200, 240, 200],
      [360, 200, 440, 200],
      [560, 200, 640, 200],
      [100, 225, 240, 325],
      [360, 200, 440, 325],
      [700, 250, 700, 300],
      [700, 375, 700, 400],
      [560, 350, 640, 325]
    ]
    ctx.strokeStyle = '#c0c4cc'
    ctx.lineWidth = 2
    arrows.forEach(([x1, y1, x2, y2]) => drawArrow(ctx, x1, y1, x2, y2))
    diagramBase64.value = canvas.toDataURL('image/png')
  } finally {
    diagramLoading.value = false
  }
}

function roundRect(ctx, x, y, w, h, r) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function drawArrow(ctx, x1, y1, x2, y2) {
  ctx.beginPath()
  ctx.moveTo(x1, y1)
  ctx.lineTo(x2, y2)
  ctx.stroke()
  const angle = Math.atan2(y2 - y1, x2 - x1)
  const size = 8
  ctx.beginPath()
  ctx.moveTo(x2, y2)
  ctx.lineTo(x2 - size * Math.cos(angle - Math.PI / 6), y2 - size * Math.sin(angle - Math.PI / 6))
  ctx.moveTo(x2, y2)
  ctx.lineTo(x2 - size * Math.cos(angle + Math.PI / 6), y2 - size * Math.sin(angle + Math.PI / 6))
  ctx.stroke()
}

function zoomIn() {
  scale.value = Math.min(scale.value + 0.1, 3)
}

function zoomOut() {
  scale.value = Math.max(scale.value - 0.1, 0.3)
}

function resetZoom() {
  scale.value = 1
}

function handleWheelZoom(e) {
  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

watch([() => nearbyTabName.value, () => nearbyData.loading, () => detailDialogVisible.value], async ([tabName, loading, visible]) => {
  if (tabName === 'map' && !loading && visible) {
    await nextTick()
    setTimeout(() => initDispatchMap(), 100)
  }
})

watch(() => detailDialogVisible.value, (val) => {
  if (!val) {
    nearbyTabName.value = 'map'
    if (dispatchChart.value) {
      dispatchChart.value.dispose()
      dispatchChart.value = null
    }
  }
})

onMounted(() => {
  fetchEventTypeList()
  fetchGridList()
  fetchList()
})
</script>

<style lang="scss" scoped>
.event-page {
  .search-form {
    margin-bottom: 0;
  }

  .deadline-normal {
    color: #303133;
  }

  .deadline-warning {
    color: #e6a23c;
    font-weight: 500;
  }

  .deadline-overdue {
    color: #f56c6c;
    font-weight: bold;
  }

  .deadline-hours {
    font-size: 11px;
    margin-top: 2px;
    color: inherit;
    opacity: 0.85;
  }

  .urge-progress-cell {
    padding: 2px 0;

    &.urge-normal {
      .el-tag { background-color: #67c23a; border-color: #67c23a; color: #fff; }
    }
    &.urge-warning {
      .el-tag { background-color: #e6a23c; border-color: #e6a23c; color: #fff; }
    }
    &.urge-overdue {
      .el-tag { background-color: #f56c6c; border-color: #f56c6c; color: #fff; }
    }
    &.urge-supervise {
      .el-tag { background-color: #c0392b; border-color: #c0392b; color: #fff; }
    }
  }

  .urge-progress-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 6px;
  }

  .urge-progress-text {
    font-size: 12px;
    color: #606266;
    font-weight: 600;
  }

  :deep(.el-progress-bar__outer) {
    background-color: #ebeef5;
  }

  .action-bar {
    display: flex;
    gap: 8px;
  }

  .stat-card {
    border-radius: 12px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s ease;
    border: none;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);

      .card-head {
        transform: scale(1.1);
      }
    }

    :deep(.el-card__body) {
      padding: 16px;
    }

    .card-head {
      transition: transform 0.3s ease;
      margin-bottom: 8px;
    }

    .card-num {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      line-height: 1.2;
      margin-bottom: 4px;
    }

    .card-label {
      font-size: 13px;
      color: #909399;
    }
  }

  .cam-card {
    background: linear-gradient(135deg, #e6f3ff 0%, #ffffff 100%);
    border: 1px solid rgba(25, 137, 250, 0.15);

    .card-num {
      color: #1989fa;
    }
  }

  .em-card {
    background: linear-gradient(135deg, #ffecec 0%, #ffffff 100%);
    border: 1px solid rgba(238, 10, 36, 0.15);

    .card-num {
      color: #ee0a24;
    }
  }

  .mem-card {
    background: linear-gradient(135deg, #e8fff1 0%, #ffffff 100%);
    border: 1px solid rgba(7, 193, 96, 0.15);

    .card-num {
      color: #07c160;
    }
  }

  .media-list {
    .file-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: #f5f7fa;
      border-radius: 4px;
      margin-bottom: 8px;

      a {
        color: #409eff;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  .comparison-list {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .comparison-card {
      border: 1px solid #ebeef5;
      border-radius: 8px;

      .comparison-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;

        .comparison-title {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
      }

      .similarity-section {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 16px;

        .similarity-label {
          font-size: 14px;
          color: #606266;
          white-space: nowrap;
        }

        .similarity-bar-wrap {
          flex: 1;
          height: 10px;
          background: #f0f2f5;
          border-radius: 5px;
          overflow: hidden;

          .similarity-bar {
            height: 100%;
            border-radius: 5px;
            transition: width 0.5s ease;

            &.pass {
              background: linear-gradient(90deg, #67c23a, #85ce61);
            }

            &.fail {
              background: linear-gradient(90deg, #f56c6c, #f78989);
            }
          }
        }

        .similarity-value {
          font-size: 18px;
          font-weight: bold;
          min-width: 70px;
          text-align: right;

          &.pass-text {
            color: #67c23a;
          }

          &.fail-text {
            color: #f56c6c;
          }
        }
      }

      .comparison-images {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 16px;

        .comp-img-item {
          flex: 1;

          .comp-img-label {
            font-size: 12px;
            color: #909399;
            margin-bottom: 6px;
            text-align: center;
          }
        }

        .comp-vs-icon {
          color: #c0c4cc;
          font-size: 24px;
        }
      }

      .heatmap-section {
        margin-bottom: 12px;

        .heatmap-label {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          color: #606266;
          margin-bottom: 6px;
        }
      }

      .judgment-reason {
        background: #f5f7fa;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 8px;

        .reason-label {
          font-size: 13px;
          color: #606266;
          margin-bottom: 6px;
          font-weight: 500;
        }

        .reason-text {
          font-size: 14px;
          color: #303133;
          line-height: 1.6;
        }
      }

      .comparison-time {
        font-size: 12px;
        color: #c0c4cc;
        text-align: right;
      }
    }
  }

  .history-card {
    margin-bottom: 12px;

    .history-header {
      display: flex;
      align-items: center;
      gap: 20px;
      margin-bottom: 8px;

      .node-name {
        font-weight: bold;
        font-size: 15px;
        color: #303133;
      }

      .handler, .duration {
        font-size: 13px;
        color: #909399;
      }
    }

    .history-body {
      font-size: 14px;
      color: #606266;

      p {
        margin: 4px 0;
      }
    }

    .history-attachments {
      margin-top: 8px;

      .attachment-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        margin-right: 16px;
        color: #409eff;
        text-decoration: none;
        font-size: 13px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }

  .diagram-container {
    .diagram-toolbar {
      padding: 12px;
      border-bottom: 1px solid #ebeef5;
      display: flex;
      align-items: center;
    }

    .diagram-wrapper {
      padding: 20px;
      overflow: auto;
      max-height: 600px;
      display: flex;
      justify-content: center;
      align-items: flex-start;

      .diagram-image {
        transform-origin: center top;
        transition: transform 0.2s ease;
        max-width: 100%;
      }
    }
  }
}
</style>
