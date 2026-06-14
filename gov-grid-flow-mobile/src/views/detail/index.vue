<template>
  <div class="detail-container">
    <van-nav-bar title="事件详情" left-arrow fixed placeholder @click-left="onBack" />

    <div v-if="loading" class="loading-wrap">
      <van-loading type="spinner" size="32px" color="#1989fa">加载中...</van-loading>
    </div>

    <div v-else-if="detail" class="detail-content">
      <div v-if="voiceStore.enabled && isReadingDetail" class="voice-reading-bar">
        <div class="broadcast-wave">
          <span></span><span></span><span></span><span></span>
        </div>
        <span class="broadcast-text">正在朗读详情...</span>
        <van-button size="mini" type="danger" plain @click="onStopDetailRead">停止</van-button>
      </div>

      <van-card class="header-card">
        <template #title>
          <div class="header-top">
            <span class="event-no">{{ detail.eventNo || detail.id }}</span>
            <div class="header-actions">
              <van-icon
                v-if="voiceStore.enabled"
                :name="isReadingDetail ? 'volume-o' : 'volume-o'"
                size="18"
                :color="isReadingDetail ? '#ee0a24' : '#1989fa'"
                class="voice-icon"
                @click="onToggleDetailRead"
              />
              <van-tag :type="statusTagType" size="medium">{{ statusText }}</van-tag>
            </div>
          </div>
        </template>
        <template #desc>
          <div class="header-bottom">
            <div class="priority-item">
              <span class="label">优先级：</span>
              <van-tag :type="priorityTagType" round>{{ priorityText }}</van-tag>
            </div>
            <div class="time-item">
              <van-icon name="clock-o" size="14" />
              <span>{{ detail.reportTime || detail.createTime }}</span>
            </div>
          </div>
        </template>
      </van-card>

      <div class="section">
        <div class="section-title">基本信息</div>
        <van-cell-group inset>
          <van-cell title="事件标题" :value="detail.title" class="multi-line-value" />
          <van-cell title="事件类型" :value="detail.eventTypeText || detail.eventType" />
          <van-cell title="事件描述">
            <template #value>
              <div class="description-text">{{ detail.description }}</div>
            </template>
          </van-cell>
          <van-cell title="上报时间" :value="detail.reportTime || detail.createTime" />
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">位置信息</div>
        <van-cell-group inset>
          <van-cell title="详细地址" is-link @click="openMap">
            <template #value>
              <span class="address-text">{{ detail.address }}</span>
            </template>
            <template #right-icon>
              <van-icon name="location-o" size="18" color="#1989fa" />
            </template>
          </van-cell>
          <van-cell v-if="detail.lng && detail.lat" title="经纬度">
            <template #value>
              <span class="coord-text">
                经度 {{ detail.lng }} / 纬度 {{ detail.lat }}
              </span>
            </template>
          </van-cell>
        </van-cell-group>
        <div v-if="detail.address" class="map-preview" @click="openMap">
          <van-icon name="location" size="40" color="#07c160" />
          <span class="map-text">点击查看地图位置</span>
        </div>
      </div>

      <div v-if="imageList.length > 0 || videoList.length > 0 || voiceUrl" class="section">
        <div class="section-title">媒体资料</div>
        <van-cell-group inset>
          <div v-if="imageList.length > 0" class="media-item">
            <div class="media-label">现场照片</div>
            <div class="image-grid">
              <van-image
                v-for="(img, idx) in imageList"
                :key="'img-' + idx"
                :src="img"
                width="100"
                height="100"
                fit="cover"
                radius="6"
                @click="previewImage(idx)"
              />
            </div>
          </div>
          <div v-if="videoList.length > 0" class="media-item">
            <div class="media-label">现场视频</div>
            <div class="video-list">
              <div
                v-for="(video, idx) in videoList"
                :key="'video-' + idx"
                class="video-item"
              >
                <video
                  :src="video"
                  controls
                  class="video-player"
                  playsinline
                  webkit-playsinline
                />
              </div>
            </div>
          </div>
          <div v-if="voiceUrl" class="media-item">
            <div class="media-label">
              <van-icon name="voice" size="14" />
              <span>语音描述</span>
            </div>
            <div class="voice-player-wrap">
              <audio :src="voiceUrl" controls class="voice-player" />
              <div class="voice-hint">语音转写内容供核验参考</div>
            </div>
          </div>
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">
          区块链存证
          <van-tag v-if="blockchainEvidence" type="success" size="mini" round>已存证</van-tag>
          <van-tag v-else type="default" size="mini" plain>未存证</van-tag>
        </div>
        <van-cell-group inset>
          <template v-if="blockchainEvidence">
            <van-cell title="存证编号" :value="blockchainEvidence.evidenceNo" is-link @click="openEvidenceDetail" />
            <van-cell title="存证链" :value="blockchainEvidence.chainType" />
            <van-cell title="存证状态">
              <template #value>
                <van-tag :type="blockchainEvidence.status === 'SUCCESS' ? 'success' : 'warning'" size="small">
                  {{ blockchainEvidence.status === 'SUCCESS' ? '上链成功' : '处理中' }}
                </van-tag>
              </template>
            </van-cell>
            <van-cell title="核验状态">
              <template #value>
                <van-tag :type="blockchainEvidence.verified === 1 ? 'success' : 'default'" size="small">
                  {{ blockchainEvidence.verified === 1 ? '已核验' : '待核验' }}
                </van-tag>
              </template>
            </van-cell>
            <div class="evidence-actions">
              <van-button size="small" type="primary" plain icon="scan" @click="onVerifyEvidence" :loading="verifyingEvidence">
                链上核验
              </van-button>
              <van-button size="small" type="success" icon="description" @click="openEvidenceDetail">
                查看证书
              </van-button>
            </div>
          </template>
          <template v-else>
            <van-cell title="存证说明">
              <template #value>
                <span class="evidence-empty-text">暂无存证记录</span>
              </template>
            </van-cell>
            <van-cell title="存证作用" label="图片、视频、GPS哈希上链，司法联盟链存证防篡改" />
            <div class="evidence-create-btn">
              <van-button type="primary" icon="shield" :loading="blockchainLoading" @click="onCreateEvidence">
                立即存证
              </van-button>
            </div>
          </template>
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">上报人信息</div>
        <van-cell-group inset>
          <van-cell title="上报方式">
            <template #value>
              <van-tag v-if="detail.anonymous === 1 || detail.anonymous" type="warning" plain>匿名上报</van-tag>
              <van-tag v-else type="primary" plain>实名上报</van-tag>
            </template>
          </van-cell>
          <van-cell v-if="!(detail.anonymous === 1 || detail.anonymous)" title="姓名" :value="detail.reporterName || detail.reporter || '-' " />
          <van-cell v-if="!(detail.anonymous === 1 || detail.anonymous)" title="联系电话" :value="detail.reporterPhone || detail.phone || '-' " />
        </van-cell-group>
      </div>

      <div class="section">
        <div class="section-title">处理流程</div>
        <van-cell-group inset>
          <div class="timeline-wrap">
            <div
              v-for="(node, index) in processList"
              :key="index"
              class="timeline-node"
              :class="{ 'is-last': index === processList.length - 1 }"
            >
              <div class="timeline-dot" :class="getNodeClass(node)"></div>
              <div v-if="index !== processList.length - 1" class="timeline-line" :class="getNodeClass(node)"></div>
              <div class="timeline-content">
                <div class="timeline-header">
                  <span class="node-name">{{ node.nodeName }}</span>
                  <van-tag v-if="node.status" :type="getNodeTagType(node)" size="small">{{ getNodeStatusText(node) }}</van-tag>
                </div>
                <div v-if="node.handlerName" class="timeline-handler">
                  <van-icon name="user-o" size="12" />
                  <span>{{ node.handlerName }}</span>
                  <span v-if="node.handleTime" class="handle-time">{{ node.handleTime }}</span>
                </div>
                <div v-if="node.comment" class="timeline-comment">
                  <div class="comment-label">处理意见：</div>
                  <div class="comment-text">{{ node.comment }}</div>
                </div>
                <div v-if="node.attachments && node.attachments.length > 0" class="timeline-attachments">
                  <div class="attach-label">附件：</div>
                  <div class="attach-list">
                    <template v-for="(att, attIdx) in parseAttachments(node.attachments)" :key="attIdx">
                      <van-image
                        v-if="isImageFile(att)"
                        :src="att"
                        width="60"
                        height="60"
                        fit="cover"
                        radius="4"
                        @click="previewAttachment(parseAttachments(node.attachments), attIdx)"
                      />
                      <a
                        v-else
                        :href="att"
                        target="_blank"
                        class="attach-link"
                      >
                        <van-icon name="description" size="16" />
                        <span>附件{{ attIdx + 1 }}</span>
                      </a>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </van-cell-group>
      </div>

      <!-- AI图像比对结果 -->
      <div v-if="comparisonList && comparisonList.length > 0" class="section">
        <div class="section-title">
          <van-icon name="magic" size="14" color="#1989fa" />
          <span>AI图像比对结果</span>
        </div>
        <van-cell-group inset>
          <div
            v-for="(item, idx) in comparisonList"
            :key="item.id || idx"
            class="comparison-card"
          >
            <div class="comparison-header">
              <div class="comparison-index">比对记录 {{ idx + 1 }}</div>
              <van-tag
                :type="item.judgment === 'PASS' ? 'success' : 'danger'"
                size="medium"
                round
              >
                {{ item.judgmentText || (item.judgment === 'PASS' ? '合格' : item.judgment === 'FAIL' ? '不合格' : '待判定') }}
              </van-tag>
            </div>

            <div class="similarity-wrap">
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
              <div class="image-item">
                <div class="image-label">处置前</div>
                <van-image
                  :src="getFullFileUrl(item.beforeImage)"
                  width="100%"
                  height="100"
                  fit="cover"
                  radius="4"
                  @click="previewImageByUrl(getFullFileUrl(item.beforeImage))"
                />
              </div>
              <div class="vs-icon">
                <van-icon name="arrow" size="20" />
              </div>
              <div class="image-item">
                <div class="image-label">处置后</div>
                <van-image
                  :src="getFullFileUrl(item.afterImage)"
                  width="100%"
                  height="100"
                  fit="cover"
                  radius="4"
                  @click="previewImageByUrl(getFullFileUrl(item.afterImage))"
                />
              </div>
            </div>

            <div v-if="item.heatmapImage" class="heatmap-wrap">
              <div class="heatmap-label">
                <van-icon name="eye-o" size="12" />
                <span>差异热力图</span>
              </div>
              <van-image
                :src="item.heatmapImage"
                width="100%"
                height="150"
                fit="cover"
                radius="4"
                @click="previewImageByUrl(item.heatmapImage)"
              />
            </div>

            <div v-if="item.judgmentReason" class="judgment-reason">
              <div class="reason-label">AI判定说明</div>
              <div class="reason-text">{{ item.judgmentReason }}</div>
            </div>

            <div v-if="item.createdAt" class="comparison-time">
              比对时间：{{ item.createdAt }}
            </div>
          </div>
        </van-cell-group>
      </div>

      <!-- 周边资源调度 -->
      <van-cell-group v-if="detail.lng" inset title="周边资源调度（500米）" style="margin-top:12px">
        <van-cell title="周边摄像头" :label="`${nearbyData.cameraCount}个`">
          <template #icon><van-icon name="eye-o" color="#1989fa" size="20"/></template>
          <template #right-icon>
            <van-tag color="#e6f7ff" text-color="#1989fa" size="medium">查看</van-tag>
          </template>
        </van-cell>
        <div v-if="nearbyData.cameras.length" class="nearby-grid">
          <div v-for="cam in nearbyData.cameras.slice(0,4)" :key="cam.id" class="nearby-item cam-item">
            <div class="item-head">
              <van-icon name="eye-o" color="#1989fa"/>
              <span class="item-name">{{ cam.cameraName }}</span>
            </div>
            <div class="item-meta">
              <span>{{ cam.distance }}米</span>
              <span :class="cam.status===1?'tag-green':'tag-gray'">
                {{ cam.status===1?'在线':'离线' }}
              </span>
            </div>
            <a v-if="cam.hlsUrl" :href="cam.hlsUrl" target="_blank" class="play-link">
              <van-icon name="play-circle-o"/> 查看直播
            </a>
          </div>
        </div>

        <van-cell title="应急物资点" :label="`${nearbyData.emergencyCount}处`">
          <template #icon><van-icon name="fire-o" color="#ee0a24" size="20"/></template>
        </van-cell>
        <div v-if="nearbyData.emergencies.length" class="nearby-grid">
          <div v-for="item in nearbyData.emergencies.slice(0,4)" :key="item.id" class="nearby-item em-item">
            <div class="item-head">
              <van-icon name="fire-o" color="#ee0a24"/>
              <span class="item-name">{{ item.resourceName }}</span>
              <van-tag size="mini" type="warning">x{{ item.quantity }}</van-tag>
            </div>
            <div class="item-meta">
              <span>{{ item.distance }}米</span>
              <span class="tag-type">{{ item.resourceTypeName }}</span>
            </div>
            <div v-if="item.managerPhone" class="call-manager">
              <a :href="`tel:${item.managerPhone}`" @click.stop>
                <van-icon name="phone-o"/> 联系管理员 {{ item.manager }}
              </a>
            </div>
          </div>
        </div>

        <van-cell title="附近网格员" :label="`${nearbyData.memberCount}人在岗`" @click="showCallSheet=true">
          <template #icon><van-icon name="friends-o" color="#07c160" size="20"/></template>
          <template #right-icon>
            <van-button size="small" type="success" round plain @click.stop="showCallSheet=true">
              <van-icon name="phone-o"/> 一键呼叫
            </van-button>
          </template>
        </van-cell>
        <div v-if="nearbyData.members.length" class="nearby-grid">
          <div v-for="mem in nearbyData.members.slice(0,4)" :key="mem.userId" class="nearby-item mem-item">
            <van-avatar :size="48" color="#07c160">{{ mem.userName.charAt(0) }}</van-avatar>
            <div class="mem-info">
              <div class="mem-name">{{ mem.userName }}</div>
              <div class="mem-meta">
                <span>{{ mem.distance }}米</span>
                <span>⚡{{ mem.battery }}%</span>
                <span class="tag-green">在岗</span>
              </div>
            </div>
            <a :href="`tel:${mem.phone}`" class="call-btn" @click.stop>
              <van-icon name="phone-o" size="20"/>
            </a>
          </div>
        </div>
      </van-cell-group>

      <div v-if="showActions" class="action-section">
        <template v-if="canVerify">
          <van-button block round type="success" size="large" @click="handleVerify">
            核查通过
          </van-button>
          <van-button block round plain type="danger" size="large" style="margin-top: 12px" @click="openReturnDialog('verify')">
            不合格退回
          </van-button>
        </template>

        <template v-else-if="canApprove">
          <van-button block round type="success" size="large" @click="handleApprove">
            审核通过
          </van-button>
          <van-button block round plain type="danger" size="large" style="margin-top: 12px" @click="openRejectDialog">
            驳回
          </van-button>
        </template>

        <template v-else-if="canProcess">
          <van-button block round type="success" size="large" @click="handleProcess">
            处置完成
          </van-button>
          <van-button block round plain type="warning" size="large" style="margin-top: 12px" @click="openReturnDialog('process')">
            退回重办
          </van-button>
        </template>

        <template v-else-if="canAssign">
          <div v-if="nlpDispatch" class="nlp-dispatch-section">
            <div class="nlp-dispatch-header">
              <van-icon name="guide-o" size="18" color="#1989fa" />
              <span class="nlp-dispatch-title">AI智能推荐分派</span>
              <van-tag :type="nlpDispatch.autoDispatch ? 'success' : 'warning'" size="small" round>
                {{ nlpDispatch.autoDispatch ? '置信度≥80%' : '需人工确认' }}
              </van-tag>
            </div>
            <div class="nlp-dispatch-body">
              <div class="nlp-dispatch-dept">
                <span class="dept-label">推荐部门：</span>
                <span class="dept-value">{{ nlpDispatch.departmentName }}</span>
              </div>
              <div class="nlp-dispatch-confidence">
                <span>置信度：{{ (nlpDispatch.confidence * 100).toFixed(1) }}%</span>
                <van-tag size="mini" type="primary" plain>
                  {{ nlpDispatch.method === 'rule' ? '规则匹配' : nlpDispatch.method === 'model' ? 'BERT模型' : nlpDispatch.method }}
                </van-tag>
              </div>
              <div v-if="nlpDispatch.allScores && nlpDispatch.allScores.length > 0" class="nlp-dispatch-scores">
                <div v-for="score in nlpDispatch.allScores.slice(0,3)" :key="score.departmentCode" class="score-item">
                  <span class="score-name">{{ score.departmentName }}</span>
                  <div class="score-bar-wrap">
                    <div class="score-bar" :style="{ width: (score.score * 100) + '%' }"></div>
                  </div>
                  <span class="score-value">{{ (score.score * 100).toFixed(1) }}%</span>
                </div>
              </div>
            </div>
            <div class="nlp-dispatch-actions">
              <van-button size="small" type="primary" round @click="adoptNlpDispatch">
                一键采纳推荐
              </van-button>
              <van-button size="small" plain type="default" round @click="openAssignPopup">
                手动选择
              </van-button>
            </div>
          </div>
          <van-button v-else block round type="primary" size="large" @click="openAssignPopup">
            分派任务
          </van-button>
        </template>

        <template v-else-if="canEvaluate">
          <van-button block round type="warning" size="large" @click="openEvaluatePopup">
            事件评价
          </van-button>
        </template>
      </div>

      <div class="bottom-placeholder"></div>
    </div>

    <van-popup v-model:show="showAssign" round position="bottom" :style="{ height: '60%' }">
      <div class="popup-header">
        <div class="popup-title">选择处置人员</div>
        <van-icon name="cross" size="22" @click="showAssign = false" />
      </div>
      <div class="popup-content">
        <van-cell-group v-if="memberList.length > 0">
          <van-cell
            v-for="member in memberList"
            :key="member.id"
            :title="member.realName || member.name || member.username"
            :label="member.phone || member.roleText || ''"
            is-link
            @click="confirmAssign(member)"
          >
            <template #icon>
              <van-icon name="user-circle-o" size="32" color="#1989fa" />
            </template>
          </van-cell>
        </van-cell-group>
        <van-empty v-else description="暂无可用人员" />
      </div>
    </van-popup>

    <!-- 处置完成弹窗 -->
    <van-popup v-model:show="showProcess" round position="bottom" :style="{ height: '80%' }">
      <div class="popup-header">
        <div class="popup-title">处置完成</div>
        <van-icon name="cross" size="22" @click="showProcess = false" />
      </div>
      <div class="process-content">
        <van-cell-group inset>
          <van-field
            v-model="processForm.comment"
            label="处置意见"
            type="textarea"
            rows="3"
            autosize
            maxlength="500"
            placeholder="请输入处置意见（选填）"
            show-word-limit
          />
        </van-cell-group>

        <div class="process-section">
          <div class="section-label">
            <span>处置后照片</span>
            <span class="label-hint">上传后将自动进行AI比对</span>
          </div>
          <van-uploader
            v-model="afterImageFiles"
            :max-count="9"
            :max-size="10 * 1024 * 1024"
            multiple
            :deletable="true"
            :before-read="beforeProcessImageRead"
            :after-read="afterProcessImageRead"
            @delete="onProcessImageDelete"
            accept="image/*"
          />
          <div class="upload-hint">支持 jpg、png 格式，单张不超过10MB</div>
        </div>

        <!-- AI比对结果 -->
        <div v-if="processComparisonResult" class="comparison-result-wrap">
          <div class="result-header">
            <div class="result-title">
              <van-icon name="magic" size="16" color="#1989fa" />
              <span>AI图像比对结果</span>
            </div>
            <van-tag
              :type="processComparisonResult.judgment === 'PASS' ? 'success' : 'danger'"
              size="medium"
              round
            >
              {{ processComparisonResult.judgmentText || (processComparisonResult.judgment === 'PASS' ? '合格' : '不合格') }}
            </van-tag>
          </div>

          <div class="similarity-wrap">
            <div class="similarity-label">图像相似度</div>
            <div class="similarity-bar-wrap">
              <div
                class="similarity-bar"
                :style="{ width: processComparisonResult.similarity + '%' }"
                :class="processComparisonResult.judgment === 'PASS' ? 'pass' : 'fail'"
              ></div>
            </div>
            <div class="similarity-value" :class="processComparisonResult.judgment === 'PASS' ? 'pass-text' : 'fail-text'">
              {{ processComparisonResult.similarity }}%
            </div>
          </div>

          <div class="comparison-images">
            <div class="image-item">
              <div class="image-label">处置前</div>
              <van-image
                :src="processComparisonResult.beforeImage ? getFullFileUrl(processComparisonResult.beforeImage) : ''"
                width="100%"
                height="80"
                fit="cover"
                radius="4"
              />
            </div>
            <div class="vs-icon">
              <van-icon name="arrow" size="18" />
            </div>
            <div class="image-item">
              <div class="image-label">处置后</div>
              <van-image
                :src="processComparisonResult.afterImage ? getFullFileUrl(processComparisonResult.afterImage) : ''"
                width="100%"
                height="80"
                fit="cover"
                radius="4"
              />
            </div>
          </div>

          <div v-if="processComparisonResult.heatmapImage" class="heatmap-wrap">
            <div class="heatmap-label">
              <van-icon name="eye-o" size="12" />
              <span>差异热力图</span>
            </div>
            <van-image
              :src="processComparisonResult.heatmapImage"
              width="100%"
              height="120"
              fit="cover"
              radius="4"
            />
          </div>

          <div v-if="processComparisonResult.judgmentReason" class="judgment-reason">
            <div class="reason-label">AI判定说明</div>
            <div class="reason-text">{{ processComparisonResult.judgmentReason }}</div>
          </div>
        </div>

        <div v-if="comparing" class="comparing-hint">
          <van-loading size="20px" color="#1989fa" />
          <span>AI比对中，请稍候...</span>
        </div>
      </div>

      <div class="process-actions">
        <van-button block round type="success" size="large" :loading="processLoading" @click="submitProcessForm">
          确认处置完成
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showEvaluate" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">事件评价</div>
        <van-icon name="cross" size="22" @click="showEvaluate = false" />
      </div>
      <div class="evaluate-content">
        <van-cell-group inset>
          <van-cell title="处理速度">
            <template #value>
              <van-rate v-model="evaluation.speed" count="5" size="24" />
            </template>
          </van-cell>
          <van-cell title="处理效果">
            <template #value>
              <van-rate v-model="evaluation.effect" count="5" size="24" />
            </template>
          </van-cell>
          <van-field
            v-model="evaluation.comment"
            label="评价内容"
            type="textarea"
            rows="3"
            autosize
            maxlength="200"
            placeholder="请输入评价内容（选填）"
            show-word-limit
          />
        </van-cell-group>
        <div class="evaluate-actions">
          <van-button block round type="warning" size="large" :loading="evaluating" @click="submitEvaluationForm">
            提交评价
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showReturn" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">{{ returnType === 'verify' ? '不合格退回' : '退回重办' }}</div>
        <van-icon name="cross" size="22" @click="showReturn = false" />
      </div>
      <div class="return-content">
        <van-cell-group inset>
          <van-field
            v-model="returnForm.reason"
            label="退回原因"
            type="textarea"
            rows="4"
            autosize
            maxlength="500"
            placeholder="请输入退回原因"
            show-word-limit
            :rules="[{ required: true, message: '请输入退回原因' }]"
          />
        </van-cell-group>
        <div class="return-actions">
          <van-button block round type="danger" size="large" :loading="returnLoading" @click="submitReturn">
            确认退回
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showReject" round position="bottom" :style="{ height: 'auto' }">
      <div class="popup-header">
        <div class="popup-title">审核驳回</div>
        <van-icon name="cross" size="22" @click="showReject = false" />
      </div>
      <div class="return-content">
        <van-cell-group inset>
          <van-field
            v-model="rejectForm.reason"
            label="驳回原因"
            type="textarea"
            rows="4"
            autosize
            maxlength="500"
            placeholder="请输入驳回原因"
            show-word-limit
            :rules="[{ required: true, message: '请输入驳回原因' }]"
          />
        </van-cell-group>
        <div class="return-actions">
          <van-button block round type="danger" size="large" :loading="rejectLoading" @click="submitReject">
            确认驳回
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 区块链存证证书详情 -->
    <van-popup v-model:show="showEvidenceDialog" round position="bottom" :style="{ height: '90%' }">
      <div class="evidence-cert-container">
        <div class="cert-header">
          <div class="cert-title">
            <van-icon name="shield-o" size="24" color="#07c160" />
            <span>司法联盟链存证证书</span>
          </div>
          <van-icon name="cross" size="22" @click="showEvidenceDialog = false" />
        </div>

        <div class="cert-body" v-if="blockchainEvidence">
          <div class="cert-qr-section">
            <div class="qr-code-wrapper">
              <div class="qr-code-placeholder">
                <van-icon name="scan" size="48" color="#07c160" />
                <div class="qr-text">扫描核验</div>
              </div>
            </div>
            <div class="cert-no">证书编号：{{ blockchainEvidence.evidenceNo }}</div>
            <div class="cert-status">
              <van-tag type="success" size="medium" round>
                {{ blockchainEvidence.status === 'SUCCESS' ? '上链成功' : '处理中' }}
              </van-tag>
            </div>
          </div>

          <van-cell-group inset title="存证基本信息">
            <van-cell title="事件编号" :value="detail?.eventNo || detail?.id" />
            <van-cell title="事件标题" :value="detail?.title" />
            <van-cell title="存证链" :value="blockchainEvidence.chainType" />
            <van-cell title="交易哈希" is-link @click="copyText(blockchainEvidence.txHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.txHash }}</span>
              </template>
            </van-cell>
            <van-cell title="区块高度" :value="blockchainEvidence.blockHeight" />
            <van-cell title="区块时间" :value="blockchainEvidence.blockTime" />
            <van-cell title="存证时间" :value="blockchainEvidence.createdAt" />
          </van-cell-group>

          <van-cell-group inset title="证据哈希清单" style="margin-top: 12px">
            <van-cell title="证据总哈希" is-link @click="copyText(blockchainEvidence.evidenceHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.evidenceHash }}</span>
              </template>
            </van-cell>
            <van-cell v-if="blockchainEvidence.imageCount > 0" title="图片数量" :value="`${blockchainEvidence.imageCount}张`" />
            <van-cell v-if="blockchainEvidence.videoCount > 0" title="视频数量" :value="`${blockchainEvidence.videoCount}个`" />
            <van-cell v-if="blockchainEvidence.voiceHash" title="语音哈希" is-link @click="copyText(blockchainEvidence.voiceHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.voiceHash }}</span>
              </template>
            </van-cell>
            <van-cell title="GPS哈希" is-link @click="copyText(blockchainEvidence.gpsHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.gpsHash }}</span>
              </template>
            </van-cell>
            <van-cell title="标题哈希" is-link @click="copyText(blockchainEvidence.titleHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.titleHash }}</span>
              </template>
            </van-cell>
            <van-cell title="描述哈希" is-link @click="copyText(blockchainEvidence.descHash)">
              <template #value>
                <span class="hash-text">{{ blockchainEvidence.descHash }}</span>
              </template>
            </van-cell>
          </van-cell-group>

          <van-cell-group inset style="margin-top: 12px">
            <van-cell title="核验状态">
              <template #value>
                <van-tag :type="blockchainEvidence.verified === 1 ? 'success' : 'warning'" size="medium">
                  {{ blockchainEvidence.verified === 1 ? '已核验 · 数据真实有效' : '待核验' }}
                </van-tag>
              </template>
            </van-cell>
            <van-cell v-if="blockchainEvidence.verifyTime" title="最近核验时间" :value="blockchainEvidence.verifyTime" />
          </van-cell-group>

          <div class="cert-footer">
            <van-button block round type="primary" icon="scan" :loading="verifyingEvidence" @click="onVerifyEvidence">
              立即核验
            </van-button>
            <div class="cert-disclaimer">
              * 本证书由司法联盟链存证，数据不可篡改，可作为司法证据使用
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 呼叫网格员 ActionSheet -->
    <van-action-sheet v-model:show="showCallSheet" title="选择呼叫的网格员" :actions="nearbyData.members.map(m=>({
      name: `${m.userName}（${m.distance}米）`,
      subname: `${m.phone} · 电量${m.battery}%`,
      callback: () => handleCall(m)
    }))">
    </van-action-sheet>

    <van-image-preview
      v-model:show="previewVisible"
      :images="previewImages"
      :start-position="previewIndex"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog, showImagePreview } from 'vant'
import {
  getEventDetail,
  approveEvent,
  verifyEvent,
  processEvent,
  returnEvent,
  rejectEvent,
  assignEvent,
  submitEvaluation as apiSubmitEvaluation,
  getMemberList,
  getNearbyResources,
  callMember,
  uploadFile,
  compareImages,
  nlpRecommend,
  nlpAdoptDispatch,
  getDispatchHistory,
  getBlockchainEvidence,
  createBlockchainEvidence,
  verifyBlockchainEvidence
} from '@/api'
import { useUserStore, useVoiceStore } from '@/store'
import { speak, stop } from '@/utils/tts'
import { getFullFileUrl, getBaseURL } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const voiceStore = useVoiceStore()

const loading = ref(true)
const detail = ref(null)
const isReadingDetail = ref(false)
const detailReadCancelled = ref(false)

const showAssign = ref(false)
const showEvaluate = ref(false)
const showReturn = ref(false)
const showReject = ref(false)
const returnType = ref('verify')

const evaluating = ref(false)
const returnLoading = ref(false)
const rejectLoading = ref(false)

const memberList = ref([])
const previewVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

const evaluation = ref({
  speed: 5,
  effect: 5,
  comment: ''
})

const returnForm = ref({
  reason: ''
})

const showProcess = ref(false)
const processLoading = ref(false)
const comparing = ref(false)
const processForm = reactive({
  comment: ''
})
const afterImageFiles = ref([])
const uploadedAfterImages = ref([])
const processComparisonResult = ref(null)

const rejectForm = ref({
  reason: ''
})

const nearbyLoading = ref(false)
const nearbyData = reactive({
  radius: 500,
  cameraCount: 0,
  emergencyCount: 0,
  memberCount: 0,
  cameras: [],
  emergencies: [],
  members: []
})

const blockchainEvidence = ref(null)
const blockchainLoading = ref(false)
const verifyingEvidence = ref(false)
const showEvidenceDialog = ref(false)
const showCallSheet = ref(false)
const showResourceMap = ref(false)

const nlpDispatch = ref(null)
const nlpDispatchHistory = ref([])

const statusMap = {
  PENDING: { text: '待受理', tag: 'warning' },
  APPROVED: { text: '已受理', tag: 'primary' },
  DISPATCHED: { text: '已分派', tag: 'primary' },
  HANDLED: { text: '已处置', tag: 'success' },
  COMPLETED: { text: '已办结', tag: 'success' },
  REJECTED: { text: '已驳回', tag: 'danger' }
}

const priorityMap = {
  LOW: { text: '低', tag: 'default' },
  NORMAL: { text: '一般', tag: 'primary' },
  HIGH: { text: '紧急', tag: 'warning' },
  URGENT: { text: '特急', tag: 'danger' }
}

const statusText = computed(() => {
  const s = detail.value?.status || ''
  if (statusMap[s]) return statusMap[s].text
  return s || '未知'
})

const statusTagType = computed(() => {
  const s = detail.value?.status || ''
  if (statusMap[s]) return statusMap[s].tag
  return 'default'
})

const priorityText = computed(() => {
  const p = detail.value?.priority || ''
  if (priorityMap[p]) return priorityMap[p].text
  return p || '一般'
})

const priorityTagType = computed(() => {
  const p = detail.value?.priority || ''
  if (priorityMap[p]) return priorityMap[p].tag
  return 'primary'
})

const imageList = computed(() => {
  if (!detail.value?.images) return []
  let list = []
  if (Array.isArray(detail.value.images)) {
    list = detail.value.images
  } else {
    list = String(detail.value.images).split(',').filter(Boolean)
  }
  return list.map(url => getFullFileUrl(url))
})

const videoList = computed(() => {
  if (!detail.value?.videos) return []
  let list = []
  if (Array.isArray(detail.value.videos)) {
    list = detail.value.videos
  } else {
    list = String(detail.value.videos).split(',').filter(Boolean)
  }
  return list.map(url => getFullFileUrl(url))
})

const voiceUrl = computed(() => {
  const url = detail.value?.voiceUrl || ''
  return getFullFileUrl(url)
})

const processList = computed(() => {
  if (detail.value?.processList && Array.isArray(detail.value.processList) && detail.value.processList.length > 0) {
    return detail.value.processList
  }
  return buildDefaultProcessList()
})

const currentUserRole = computed(() => userStore.userRole || '')
const currentUserId = computed(() => userStore.userInfo?.id || '')

const canVerify = computed(() => {
  return currentUserRole.value === 'worker' && detail.value?.status === 'HANDLED'
})

const canApprove = computed(() => {
  return currentUserRole.value === 'grid_leader' && detail.value?.status === 'PENDING'
})

const canProcess = computed(() => {
  return currentUserRole.value === 'handler' && detail.value?.status === 'DISPATCHED'
})

const canAssign = computed(() => {
  const isAdmin = currentUserRole.value === 'admin' || currentUserRole.value === 'grid_leader'
  return isAdmin && detail.value?.status === 'APPROVED'
})

const canEvaluate = computed(() => {
  const isReporter = detail.value?.reporterId === currentUserId.value ||
    detail.value?.reporterPhone === userStore.userPhone
  return detail.value?.status === 'COMPLETED' && isReporter
})

const showActions = computed(() => {
  return canVerify.value || canApprove.value || canProcess.value || canAssign.value || canEvaluate.value
})

const buildDefaultProcessList = () => {
  const d = detail.value
  const list = []
  const statusOrder = ['PENDING', 'APPROVED', 'DISPATCHED', 'HANDLED', 'COMPLETED']
  const currentIdx = statusOrder.indexOf(d?.status)
  list.push({
    nodeName: '事件上报',
    handlerName: d?.reporterName || d?.reporter || '匿名用户',
    handleTime: d?.reportTime || d?.createTime,
    status: 'COMPLETED',
    comment: d?.description ? '事件已提交' : '',
    attachments: null
  })
  if (d?.approveTime || currentIdx >= 1) {
    list.push({
      nodeName: '网格长受理',
      handlerName: d?.approverName || '-',
      handleTime: d?.approveTime || '待受理',
      status: d?.approveTime ? 'COMPLETED' : 'PENDING',
      comment: d?.approveComment || '',
      attachments: null
    })
  }
  if (d?.assignTime || currentIdx >= 2) {
    list.push({
      nodeName: '任务分派',
      handlerName: d?.assignerName || '-',
      handleTime: d?.assignTime || '待分派',
      status: d?.assignTime ? 'COMPLETED' : 'PENDING',
      comment: d?.assignComment || (d?.handlerName ? `分派给：${d.handlerName}` : ''),
      attachments: null
    })
  }
  if (d?.processTime || currentIdx >= 3) {
    list.push({
      nodeName: '事件处置',
      handlerName: d?.handlerName || '-',
      handleTime: d?.processTime || '待处置',
      status: d?.processTime ? 'COMPLETED' : 'PENDING',
      comment: d?.processComment || '',
      attachments: d?.processAttachments || null
    })
  }
  if (d?.status === 'COMPLETED') {
    list.push({
      nodeName: '事件办结',
      handlerName: '系统',
      handleTime: d?.finishTime || d?.completeTime,
      status: 'COMPLETED',
      comment: d?.evaluationScore ? `已评价：${d.evaluationScore}分` : '事件已完成',
      attachments: null
    })
  }
  if (d?.status === 'REJECTED') {
    list.push({
      nodeName: '已驳回',
      handlerName: d?.approverName || '-',
      handleTime: d?.rejectTime || d?.approveTime,
      status: 'REJECTED',
      comment: d?.rejectComment || d?.approveComment || '',
      attachments: null
    })
  }
  return list
}

const buildDetailBroadcastText = () => {
  const d = detail.value
  if (!d) return ''
  const eventType = d.eventTypeText || d.eventType || '未分类'
  const priority = priorityText.value || '一般'
  const title = d.title || '无标题'
  const address = d.address || '暂无位置信息'
  const description = d.description || '暂无描述'
  const reportTime = d.reportTime || d.createTime || ''
  const reporter = d.anonymous ? '匿名用户' : (d.reporterName || d.reporter || d.createBy || '匿名')

  let text = `${priority}事件详情。`
  text += `事件类型：${eventType}。`
  text += `标题：${title}。`
  text += `地点：${address}。`
  text += `描述：${description}。`
  if (reportTime) {
    text += `上报时间：${reportTime.replace('T', ' ').slice(0, 16)}。`
  }
  if (!d.anonymous) {
    text += `上报人：${reporter}。`
  }
  return text
}

const onToggleDetailRead = async () => {
  if (isReadingDetail.value) {
    onStopDetailRead()
    return
  }
  if (!voiceStore.enabled) {
    showToast('请先在设置中开启语音播报')
    return
  }
  if (!detail.value) {
    showToast('详情未加载完成')
    return
  }

  detailReadCancelled.value = false
  isReadingDetail.value = true
  voiceStore.isBroadcasting = true

  try {
    const text = buildDetailBroadcastText()
    await speak(text, voiceStore.getVoiceOptions())
    if (!detailReadCancelled.value) {
      showToast('详情朗读完成')
    }
  } catch (e) {
    console.error(e)
    if (e.message !== 'cancel' && e.message !== 'interrupted' && !detailReadCancelled.value) {
      showToast('语音朗读失败')
    }
  } finally {
    isReadingDetail.value = false
    if (voiceStore.broadcastQueue.length === 0) {
      voiceStore.isBroadcasting = false
    }
  }
}

const onStopDetailRead = () => {
  detailReadCancelled.value = true
  stop()
  isReadingDetail.value = false
  if (voiceStore.broadcastQueue.length === 0) {
    voiceStore.isBroadcasting = false
  }
  showToast('已停止朗读')
}

const onBack = () => router.back()

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getEventDetail(route.params.id)
    detail.value = res.data
    if (detail.value.lng && detail.value.lat) {
      await fetchNearbyResources(detail.value.lng, detail.value.lat)
    }
    if (detail.value.dispatchHistory && detail.value.dispatchHistory.length > 0) {
      nlpDispatchHistory.value = detail.value.dispatchHistory
      const latestDispatch = detail.value.dispatchHistory[0]
      if (latestDispatch && (latestDispatch.status === 'RECOMMENDED' || latestDispatch.status === 'AUTO_DISPATCHED')) {
        nlpDispatch.value = {
          departmentCode: latestDispatch.recommendedDeptCode,
          departmentName: latestDispatch.recommendedDeptName,
          confidence: latestDispatch.confidence,
          autoDispatch: latestDispatch.autoDispatch === 1,
          method: latestDispatch.dispatchMethod,
          dispatchRecordId: latestDispatch.id,
          allScores: []
        }
      }
    } else {
      fetchNlpRecommendation()
    }
    fetchBlockchainEvidence()
  } catch (e) {
    console.warn('Load event detail failed, using mock data', e)
    detail.value = getMockDetail()
    if (detail.value.lng && detail.value.lat) {
      await fetchNearbyResources(detail.value.lng, detail.value.lat)
    }
  } finally {
    loading.value = false
    if (voiceStore.enabled && voiceStore.autoPlayOnDetail && detail.value) {
      setTimeout(() => onToggleDetailRead(), 500)
    }
  }
}

const fetchBlockchainEvidence = async () => {
  try {
    const res = await getBlockchainEvidence(route.params.id)
    blockchainEvidence.value = res.data
  } catch (e) {
    blockchainEvidence.value = null
  }
}

const onCreateEvidence = async () => {
  try {
    blockchainLoading.value = true
    const res = await createBlockchainEvidence(route.params.id)
    blockchainEvidence.value = res.data
    showToast({ type: 'success', message: '存证成功' })
  } catch (e) {
    showToast(e.message || '存证失败')
  } finally {
    blockchainLoading.value = false
  }
}

const onVerifyEvidence = async () => {
  if (!blockchainEvidence.value?.id) return
  try {
    verifyingEvidence.value = true
    const res = await verifyBlockchainEvidence(blockchainEvidence.value.id)
    if (res.data?.valid) {
      blockchainEvidence.value.verified = 1
      showToast({ type: 'success', message: '存证核验通过' })
    } else {
      showToast({ type: 'fail', message: '存证核验失败' })
    }
  } catch (e) {
    showToast(e.message || '核验失败')
  } finally {
    verifyingEvidence.value = false
  }
}

const openEvidenceDetail = () => {
  showEvidenceDialog.value = true
}

const copyText = (text) => {
  if (navigator.clipboard) {
    navigator.clipboard.writeText(text)
    showToast('已复制')
  } else {
    showToast('复制失败，请手动复制')
  }
}

async function fetchNearbyResources(lng, lat) {
  nearbyLoading.value = true
  try {
    const res = await getNearbyResources({ lng, lat, radius: 500 })
    if (res.data) {
      Object.assign(nearbyData, res.data)
    }
  } catch (e) {
    console.warn('周边资源查询失败', e)
  } finally {
    nearbyLoading.value = false
  }
}

async function fetchNlpRecommendation() {
  if (!detail.value || !canAssign.value) return
  try {
    const res = await nlpRecommend(route.params.id)
    if (res.data) {
      nlpDispatch.value = res.data
    }
  } catch (e) {
    console.warn('NLP推荐分派失败', e)
  }
}

async function adoptNlpDispatch() {
  if (!nlpDispatch.value || !detail.value) return
  try {
    await showConfirmDialog({
      title: '确认采纳',
      message: `确认将事件分派给「${nlpDispatch.value.departmentName}」？`
    })
    const dispatchRecordId = nlpDispatch.value.dispatchRecordId
    if (dispatchRecordId) {
      await nlpAdoptDispatch(detail.value.id || route.params.id, dispatchRecordId, {
        deptCode: nlpDispatch.value.departmentCode,
        deptName: nlpDispatch.value.departmentName
      })
    } else {
      await assignEvent({
        eventId: detail.value.id || route.params.id,
        handlerId: nlpDispatch.value.departmentCode,
        handlerName: nlpDispatch.value.departmentName
      })
    }
    showToast({ type: 'success', message: '采纳推荐分派成功' })
    nlpDispatch.value = null
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

async function handleCall(member) {
  try {
    await callMember(member.userId)
  } catch(e) {}
  if (member.phone) {
    window.location.href = `tel:${member.phone}`
  } else {
    showToast('该网格员暂无手机号')
  }
}

const getMockDetail = () => ({
  id: route.params.id || 'GD20240115001',
  eventNo: 'GD20240115001',
  title: '小区门口垃圾堆积未清理',
  eventType: 'environment',
  eventTypeText: '环境卫生',
  priority: 'HIGH',
  status: 'HANDLED',
  description: 'XX小区东门门口垃圾堆积未及时清理，已有3天时间，天气炎热产生异味，严重影响居民出行和生活环境，请相关部门尽快处理。',
  voiceUrl: '',
  createTime: '2024-01-15 10:30:25',
  reportTime: '2024-01-15 10:30:25',
  address: '浙江省杭州市西湖区XX街道XX小区东门',
  lng: '120.123456',
  lat: '30.256789',
  images: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg,https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg',
  videos: '',
  anonymous: 0,
  reporterName: '张三',
  reporterPhone: '138****8888',
  reporterId: userStore.userInfo?.id,
  processList: [
    {
      nodeName: '事件上报',
      handlerName: '张三',
      handleTime: '2024-01-15 10:30:25',
      status: 'COMPLETED',
      comment: '小区门口垃圾堆积，已提交事件上报。',
      attachments: null
    },
    {
      nodeName: '网格员核查',
      handlerName: '李网格员',
      handleTime: '2024-01-15 10:45:12',
      status: 'COMPLETED',
      comment: '现场核查情况属实，垃圾数量约3袋，建议环卫部门尽快处理。',
      attachments: 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
    },
    {
      nodeName: '网格长审核',
      handlerName: '-',
      handleTime: '',
      status: 'PENDING',
      comment: '',
      attachments: null
    }
  ]
})

const openMap = () => {
  if (!detail.value?.address && !detail.value?.lng) {
    showToast('位置信息不完整')
    return
  }
  const { lng, lat, address } = detail.value
  if (lng && lat) {
    window.open(`https://uri.amap.com/marker?position=${lng},${lat}&name=${encodeURIComponent(address || '事件位置')}`, '_blank')
  } else {
    window.open(`https://uri.amap.com/search?keyword=${encodeURIComponent(address)}`, '_blank')
  }
}

const previewImage = (index) => {
  previewImages.value = imageList.value
  previewIndex.value = index
  previewVisible.value = true
}

const previewImageByUrl = (url) => {
  showImagePreview({
    images: [url],
    startPosition: 0
  })
}

const previewAttachment = (attachments, index) => {
  const images = attachments.filter(a => isImageFile(a))
  if (images.length > 0) {
    showImagePreview({
      images,
      startPosition: images.indexOf(attachments[index]) >= 0 ? images.indexOf(attachments[index]) : 0
    })
  }
}

const isImageFile = (url) => {
  if (!url) return false
  return /\.(jpg|jpeg|png|gif|bmp|webp|svg)(\?.*)?$/i.test(url)
}

const parseAttachments = (attachments) => {
  if (!attachments) return []
  if (Array.isArray(attachments)) return attachments
  return String(attachments).split(',').filter(Boolean)
}

const getNodeClass = (node) => {
  const s = node?.status || ''
  if (s === 'COMPLETED') return 'node-completed'
  if (s === 'PENDING') return 'node-processing'
  if (s === 'REJECTED') return 'node-rejected'
  return 'node-pending'
}

const getNodeTagType = (node) => {
  const s = node?.status || ''
  if (s === 'COMPLETED') return 'success'
  if (s === 'PENDING') return 'primary'
  if (s === 'REJECTED') return 'danger'
  return 'default'
}

const getNodeStatusText = (node) => {
  const map = {
    COMPLETED: '已完成',
    PENDING: '进行中',
    REJECTED: '已驳回'
  }
  return map[node?.status] || '待处理'
}

const handleVerify = async () => {
  try {
    await showConfirmDialog({
      title: '确认核查通过',
      message: '确认该事件核查无误，提交审核？'
    })
    const res = await verifyEvent({
      eventId: detail.value.id,
      passed: true
    })
    showToast({ type: 'success', message: '核查通过' })
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleApprove = async () => {
  try {
    await showConfirmDialog({
      title: '确认审核通过',
      message: '确认该事件审核通过，进入分派流程？'
    })
    const res = await approveEvent({
      eventId: detail.value.id,
      passed: true
    })
    showToast({ type: 'success', message: '审核通过' })
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleProcess = () => {
  processForm.comment = ''
  afterImageFiles.value = []
  uploadedAfterImages.value = []
  processComparisonResult.value = null
  showProcess.value = true
}

const beforeProcessImageRead = (file) => {
  if (file.type && !file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return false
  }
  if (file.file && file.file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过10MB')
    return false
  }
  return true
}

const afterProcessImageRead = async (file) => {
  const files = Array.isArray(file) ? file : [file]
  for (const f of files) {
    try {
      f.status = 'uploading'
      f.message = '上传中...'
      const res = await uploadFile([f.file])
      const urls = res.data || []
      if (urls.length > 0) {
        uploadedAfterImages.value.push(urls[0])
        f.status = 'done'
        f.message = ''
        if (uploadedAfterImages.value.length === 1 && imageList.value.length > 0) {
          triggerComparison(urls[0])
        }
      } else {
        f.status = 'failed'
        f.message = '上传失败'
      }
    } catch (e) {
      f.status = 'failed'
      f.message = '上传失败'
    }
  }
}

const onProcessImageDelete = (file, detail) => {
  uploadedAfterImages.value.splice(detail.index, 1)
  processComparisonResult.value = null
}

const triggerComparison = async (afterImageUrl) => {
  if (!imageList.value || imageList.value.length === 0) {
    return
  }
  const beforeImageUrl = imageList.value[0]
  comparing.value = true
  try {
    const res = await compareImages({
      eventId: detail.value.id,
      beforeImage: beforeImageUrl.replace(getBaseURL(), ''),
      afterImage: afterImageUrl.replace(getBaseURL(), '')
    })
    if (res.data) {
      processComparisonResult.value = res.data
    }
  } catch (e) {
    console.error('AI比对失败', e)
    showToast('AI比对失败，可继续提交处置')
  } finally {
    comparing.value = false
  }
}

const submitProcessForm = async () => {
  processLoading.value = true
  try {
    const submitData = {
      eventId: detail.value.id,
      action: 'HANDLE',
      comment: processForm.comment,
      afterImages: uploadedAfterImages.value,
      attachments: uploadedAfterImages.value
    }
    await processEvent(submitData)
    showToast({ type: 'success', message: '处置完成' })
    showProcess.value = false
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') {
      showToast(e.message || '处置失败，请重试')
    }
  } finally {
    processLoading.value = false
  }
}

const openReturnDialog = (type) => {
  returnType.value = type
  returnForm.value.reason = ''
  showReturn.value = true
}

const submitReturn = async () => {
  if (!returnForm.value.reason.trim()) {
    showToast('请输入退回原因')
    return
  }
  returnLoading.value = true
  try {
    const api = returnType.value === 'verify' ? verifyEvent : processEvent
    const res = await api({
      eventId: detail.value.id,
      passed: false,
      completed: false,
      reason: returnForm.value.reason,
      comment: returnForm.value.reason
    })
    showToast({ type: 'success', message: '已退回' })
    showReturn.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '操作失败，请重试')
  } finally {
    returnLoading.value = false
  }
}

const openRejectDialog = () => {
  rejectForm.value.reason = ''
  showReject.value = true
}

const submitReject = async () => {
  if (!rejectForm.value.reason.trim()) {
    showToast('请输入驳回原因')
    return
  }
  rejectLoading.value = true
  try {
    const res = await rejectEvent({
      eventId: detail.value.id,
      reason: rejectForm.value.reason,
      comment: rejectForm.value.reason
    })
    showToast({ type: 'success', message: '已驳回' })
    showReject.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '操作失败，请重试')
  } finally {
    rejectLoading.value = false
  }
}

const openAssignPopup = async () => {
  showAssign.value = true
  memberList.value = []
  try {
    const gridId = detail.value?.gridId
    const res = await getMemberList(gridId)
    if (res.data && Array.isArray(res.data)) {
      memberList.value = res.data
    } else {
      memberList.value = [
        { id: 1, realName: '王处置', phone: '139****1111', roleText: '处置员' },
        { id: 2, realName: '赵工', phone: '139****2222', roleText: '处置员' }
      ]
    }
  } catch (e) {
    console.warn('Load member list failed, using mock', e)
    memberList.value = [
      { id: 1, realName: '王处置', phone: '139****1111', roleText: '处置员' },
      { id: 2, realName: '赵工', phone: '139****2222', roleText: '处置员' }
    ]
  }
}

const confirmAssign = async (member) => {
  try {
    await showConfirmDialog({
      title: '确认分派',
      message: `确定将该事件分派给「${member.realName || member.name || member.username}」？`
    })
    const res = await assignEvent({
      eventId: detail.value.id,
      handlerId: member.id,
      handlerName: member.realName || member.name || member.username
    })
    showToast({ type: 'success', message: '分派成功' })
    showAssign.value = false
    await fetchDetail()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const openEvaluatePopup = () => {
  evaluation.value = {
    speed: 5,
    effect: 5,
    comment: ''
  }
  showEvaluate.value = true
}

const submitEvaluationForm = async () => {
  evaluating.value = true
  try {
    const res = await apiSubmitEvaluation({
      eventId: detail.value.id,
      speedScore: evaluation.value.speed,
      effectScore: evaluation.value.effect,
      comment: evaluation.value.comment
    })
    showToast({ type: 'success', message: '评价提交成功' })
    showEvaluate.value = false
    await fetchDetail()
  } catch (e) {
    showToast(e.message || '评价提交失败，请重试')
  } finally {
    evaluating.value = false
  }
}

onMounted(() => {
  fetchDetail()
})

onUnmounted(() => {
  detailReadCancelled.value = true
  stop()
  isReadingDetail.value = false
})
</script>

<style lang="scss" scoped>
.detail-container {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.loading-wrap {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

.detail-content {
  padding: 12px 0;
}

.header-card {
  margin: 0 16px 12px;
  border-radius: 12px;

  :deep(.van-card__header) {
    padding: 20px 16px;
  }

  :deep(.van-card__footer) {
    display: none;
  }
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 16px;
  font-weight: bold;
  color: #323233;
}

.event-no {
  font-size: 15px;
  font-weight: 600;
}

.header-bottom {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.priority-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #646566;

  .label {
    margin-right: 4px;
  }
}

.time-item {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: #969799;
  gap: 4px;
}

.section {
  margin-top: 12px;
}

.section-title {
  padding: 12px 16px;
  font-size: 14px;
  color: #646566;
  font-weight: 600;
}

.multi-line-value {
  :deep(.van-cell__value) {
    max-width: 60%;
    word-break: break-all;
    line-height: 1.5;
  }
}

.description-text {
  font-size: 14px;
  color: #323233;
  line-height: 1.6;
  padding: 4px 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.address-text {
  color: #323233;
}

.coord-text {
  font-size: 13px;
  color: #969799;
}

.map-preview {
  margin: 12px 16px;
  height: 140px;
  background: linear-gradient(135deg, #e8f7ef 0%, #d4f0e3 100%);
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;

  .map-text {
    font-size: 14px;
    color: #07c160;
    font-weight: 500;
  }
}

.media-item {
  padding: 12px 16px;

  & + .media-item {
    border-top: 1px solid #f2f3f5;
  }
}

.media-label {
  font-size: 13px;
  color: #646566;
  margin-bottom: 10px;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.video-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.video-item {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}

.video-player {
  width: 100%;
  max-height: 220px;
  display: block;
}

.voice-player-wrap {
  margin-top: 8px;

  .voice-player {
    width: 100%;
    height: 40px;
    margin-bottom: 6px;
  }

  .voice-hint {
    font-size: 12px;
    color: #969799;
    text-align: center;
  }
}

.comparison-card {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }

  .comparison-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .comparison-index {
      font-size: 14px;
      font-weight: 600;
      color: #323233;
    }
  }

  .similarity-wrap {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .similarity-label {
      font-size: 13px;
      color: #646566;
      white-space: nowrap;
    }

    .similarity-bar-wrap {
      flex: 1;
      height: 8px;
      background: #f2f3f5;
      border-radius: 4px;
      overflow: hidden;

      .similarity-bar {
        height: 100%;
        border-radius: 4px;
        transition: width 0.3s ease;

        &.pass {
          background: linear-gradient(90deg, #07c160, #00b060);
        }

        &.fail {
          background: linear-gradient(90deg, #ee0a24, #ff6034);
        }
      }
    }

    .similarity-value {
      font-size: 14px;
      font-weight: bold;
      min-width: 50px;
      text-align: right;

      &.pass-text {
        color: #07c160;
      }

      &.fail-text {
        color: #ee0a24;
      }
    }
  }

  .comparison-images {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    .image-item {
      flex: 1;

      .image-label {
        font-size: 12px;
        color: #969799;
        margin-bottom: 4px;
        text-align: center;
      }
    }

    .vs-icon {
      color: #c8c9cc;
      font-size: 20px;
    }
  }

  .heatmap-wrap {
    margin-bottom: 12px;

    .heatmap-label {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #646566;
      margin-bottom: 6px;
    }
  }

  .judgment-reason {
    background: #f7f8fa;
    border-radius: 6px;
    padding: 10px 12px;
    margin-bottom: 8px;

    .reason-label {
      font-size: 12px;
      color: #646566;
      margin-bottom: 4px;
    }

    .reason-text {
      font-size: 13px;
      color: #323233;
      line-height: 1.5;
    }
  }

  .comparison-time {
    font-size: 12px;
    color: #c8c9cc;
    text-align: right;
  }
}

.timeline-wrap {
  padding: 8px 0;
}

.timeline-node {
  position: relative;
  display: flex;
  padding: 12px 16px 12px 36px;

  &.is-last {
    .timeline-line {
      display: none;
    }
  }
}

.timeline-dot {
  position: absolute;
  left: 16px;
  top: 16px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  z-index: 2;

  &.node-completed {
    background-color: #07c160;
    box-shadow: 0 0 0 2px #07c160;
  }

  &.node-processing {
    background-color: #1989fa;
    box-shadow: 0 0 0 2px #1989fa;
    animation: pulse 2s infinite;
  }

  &.node-pending {
    background-color: #fff;
    box-shadow: 0 0 0 2px #dcdee0;
  }

  &.node-rejected {
    background-color: #ee0a24;
    box-shadow: 0 0 0 2px #ee0a24;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.timeline-line {
  position: absolute;
  left: 21px;
  top: 28px;
  bottom: 0;
  width: 2px;

  &.node-completed {
    background: #07c160;
  }

  &.node-processing {
    background: linear-gradient(to bottom, #1989fa 0%, #dcdee0 100%);
  }

  &.node-pending,
  &.node-rejected {
    background: #dcdee0;
  }
}

.timeline-content {
  flex: 1;
  padding-bottom: 8px;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
}

.timeline-handler {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #646566;
  margin-bottom: 8px;
  flex-wrap: wrap;

  .handle-time {
    color: #969799;
  }
}

.timeline-comment {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 8px;

  .comment-label {
    font-size: 12px;
    color: #969799;
    margin-bottom: 4px;
  }

  .comment-text {
    font-size: 13px;
    color: #323233;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.timeline-attachments {
  .attach-label {
    font-size: 12px;
    color: #969799;
    margin-bottom: 6px;
  }

  .attach-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .attach-link {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 10px;
    background: #f0f9ff;
    border-radius: 4px;
    font-size: 12px;
    color: #1989fa;
    text-decoration: none;
  }
}

.action-section {
  padding: 24px 16px 8px;
  position: sticky;
  bottom: 0;
  background: rgba(247, 248, 250, 0.95);
  backdrop-filter: blur(8px);
}

.nlp-dispatch-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(25, 137, 250, 0.1);
  border: 1px solid #e6f7ff;

  .nlp-dispatch-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 12px;

    .nlp-dispatch-title {
      font-size: 15px;
      font-weight: 600;
      color: #323233;
      flex: 1;
    }
  }

  .nlp-dispatch-body {
    background: #f0f9ff;
    border-radius: 8px;
    padding: 12px;
    margin-bottom: 12px;

    .nlp-dispatch-dept {
      margin-bottom: 8px;

      .dept-label {
        font-size: 13px;
        color: #969799;
      }

      .dept-value {
        font-size: 18px;
        font-weight: bold;
        color: #1989fa;
      }
    }

    .nlp-dispatch-confidence {
      font-size: 12px;
      color: #646566;
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .nlp-dispatch-scores {
      margin-top: 8px;

      .score-item {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .score-name {
          font-size: 12px;
          color: #646566;
          width: 70px;
          flex-shrink: 0;
        }

        .score-bar-wrap {
          flex: 1;
          height: 6px;
          background: #e8f7ef;
          border-radius: 3px;
          overflow: hidden;

          .score-bar {
            height: 100%;
            background: linear-gradient(90deg, #1989fa, #64b5f6);
            border-radius: 3px;
            transition: width 0.3s;
          }
        }

        .score-value {
          font-size: 12px;
          color: #323233;
          width: 44px;
          text-align: right;
        }
      }
    }
  }

  .nlp-dispatch-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}

.bottom-placeholder {
  height: env(safe-area-inset-bottom);
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #f2f3f5;

  .popup-title {
    font-size: 16px;
    font-weight: 600;
    color: #323233;
  }
}

.popup-content {
  max-height: calc(60vh - 60px);
  overflow-y: auto;
}

.evaluate-content {
  padding-bottom: 20px;
}

.evaluate-actions {
  padding: 20px 16px 0;
}

.process-content {
  padding-bottom: 20px;
  max-height: calc(80vh - 120px);
  overflow-y: auto;
}

.process-section {
  margin-top: 12px;
  padding: 0 16px;

  .section-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 14px;
    color: #646566;
    margin-bottom: 8px;
    font-weight: 500;

    .label-hint {
      font-size: 12px;
      color: #1989fa;
      font-weight: normal;
    }
  }

  .upload-hint {
    font-size: 12px;
    color: #969799;
    margin-top: 4px;
  }
}

.comparison-result-wrap {
  margin: 12px 16px 0;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 12px;

  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .result-title {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 600;
      color: #323233;
    }
  }
}

.comparing-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  font-size: 14px;
  color: #646566;
}

.process-actions {
  padding: 12px 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.return-content {
  padding-bottom: 20px;
}

.return-actions {
  padding: 20px 16px 0;
}

.nearby-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 10px 16px;
  background: #fafbfc;
}
.nearby-item {
  background: #fff;
  border-radius: 8px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  .item-head {
    display: flex; align-items: center; gap: 6px;
    margin-bottom: 6px; font-weight: 500;
    .item-name { font-size: 13px; color: #333; }
  }
  .item-meta {
    display: flex; gap: 8px; font-size: 12px; color: #969799;
    margin-bottom: 4px;
  }
}
.tag-green { color:#07c160; background:#eefbf3; padding:1px 6px; border-radius:4px; font-size:11px; }
.tag-gray { color:#969799; background:#f2f3f5; padding:1px 6px; border-radius:4px; font-size:11px; }
.tag-type { color:#7232dd; background:#f5efff; padding:1px 6px; border-radius:4px; font-size:11px; }
.play-link { font-size: 12px; color: #1989fa; text-decoration: none; }
.call-manager a { font-size: 12px; color: #1989fa; text-decoration: none; }
.mem-item {
  display: flex; align-items: center; gap: 10px;
  .mem-info { flex:1; .mem-name {font-weight:500;font-size:13px;} .mem-meta{font-size:11px;color:#969799;display:flex;gap:6px;margin-top:2px;}}
  .call-btn { width:36px; height:36px; border-radius:50%; background:#07c160; display:flex; align-items:center; justify-content:center; color:#fff; }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.voice-icon {
  cursor: pointer;
  transition: opacity 0.2s;
  padding: 2px;

  &:active {
    opacity: 0.6;
  }
}

.voice-reading-bar {
  position: sticky;
  top: 46px;
  left: 0;
  right: 0;
  background: rgba(25, 137, 250, 0.95);
  color: #fff;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 90;
  backdrop-filter: blur(4px);
  margin: 0 0 12px;
}

.voice-reading-bar .broadcast-wave {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 16px;
  flex-shrink: 0;
}

.voice-reading-bar .broadcast-wave span {
  width: 3px;
  background: #fff;
  animation: detail-broadcast-wave 1s ease-in-out infinite;
}

.voice-reading-bar .broadcast-wave span:nth-child(1) {
  height: 40%;
  animation-delay: 0s;
}

.voice-reading-bar .broadcast-wave span:nth-child(2) {
  height: 100%;
  animation-delay: 0.1s;
}

.voice-reading-bar .broadcast-wave span:nth-child(3) {
  height: 60%;
  animation-delay: 0.2s;
}

.voice-reading-bar .broadcast-wave span:nth-child(4) {
  height: 80%;
  animation-delay: 0.3s;
}

@keyframes detail-broadcast-wave {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.voice-reading-bar .broadcast-text {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-empty-text {
  color: #c8c9cc;
  font-size: 13px;
}

.evidence-actions {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  justify-content: center;
}

.evidence-create-btn {
  padding: 16px;
  display: flex;
  justify-content: center;
}

.hash-text {
  font-size: 11px;
  color: #969799;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  font-family: monospace;
}

.evidence-cert-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

.cert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;

  .cert-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #323233;
  }
}

.cert-body {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 20px;
}

.cert-qr-section {
  background: #fff;
  padding: 24px 16px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;

  .qr-code-wrapper {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;

    .qr-code-placeholder {
      width: 140px;
      height: 140px;
      border: 2px solid #07c160;
      border-radius: 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 8px;
      background: #f0fff4;

      .qr-text {
        font-size: 12px;
        color: #07c160;
        font-weight: 500;
      }
    }
  }

  .cert-no {
    font-size: 13px;
    color: #646566;
    margin-bottom: 8px;
    font-family: monospace;
  }
}

.cert-footer {
  padding: 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;

  .cert-disclaimer {
    margin-top: 12px;
    font-size: 11px;
    color: #c8c9cc;
    text-align: center;
  }
}
</style>
