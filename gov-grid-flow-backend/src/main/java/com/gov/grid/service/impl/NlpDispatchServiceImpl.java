package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.EventDispatchRecord;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.SysDept;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.EventDispatchRecordMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.SysDeptMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.service.NlpDispatchService;
import com.gov.grid.vo.NlpDispatchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NlpDispatchServiceImpl implements NlpDispatchService {

    @Value("${nlp.service.url:http://localhost:8001}")
    private String nlpServiceUrl;

    @Value("${nlp.dispatch.enabled:true}")
    private boolean nlpDispatchEnabled;

    @Value("${nlp.dispatch.auto-dispatch-enabled:true}")
    private boolean autoDispatchEnabled;

    @Value("${nlp.dispatch.confidence-threshold:0.8}")
    private double confidenceThreshold;

    private final EventDispatchRecordMapper dispatchRecordMapper;
    private final EventInfoMapper eventInfoMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final EventProcessService eventProcessService;

    @Override
    public NlpDispatchResultVO classify(String title, String description, String eventType) {
        if (!nlpDispatchEnabled) {
            return buildFallbackResult(eventType);
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("title", title);
            requestBody.set("description", description != null ? description : "");
            if (eventType != null) {
                requestBody.set("event_type", eventType);
            }

            HttpResponse response = HttpRequest.post(nlpServiceUrl + "/classify")
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(5000)
                    .execute();

            if (response.isOk()) {
                JSONObject respJson = JSONUtil.parseObj(response.body());
                if (respJson.getBool("success", false)) {
                    JSONObject data = respJson.getJSONObject("data");
                    return parseClassifyResult(data);
                }
            }
            log.warn("NLP classify API call failed, status: {}, using fallback", response.getStatus());
        } catch (Exception e) {
            log.warn("NLP classify API call exception: {}, using fallback", e.getMessage());
        }

        return buildFallbackResult(eventType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NlpDispatchResultVO classifyWithEventId(Long eventId, String title, String description, String eventType) {
        NlpDispatchResultVO result = classify(title, description, eventType);
        Long dispatchRecordId = saveDispatchRecord(eventId, result);
        result.setDispatchRecordId(dispatchRecordId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean autoDispatchIfPossible(Long eventId, NlpDispatchResultVO result) {
        if (!autoDispatchEnabled || result == null || !Boolean.TRUE.equals(result.getAutoDispatch())) {
            return false;
        }

        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            return false;
        }

        if (!"APPROVED".equals(eventInfo.getStatus())) {
            log.info("Event {} status is not APPROVED, skip auto dispatch", eventId);
            return false;
        }

        SysDept dept = findDeptByCode(result.getDepartmentCode());
        if (dept == null) {
            log.warn("Department not found for code: {}, skip auto dispatch", result.getDepartmentCode());
            return false;
        }

        SysUser handler = findDefaultHandlerForDept(dept.getId());
        if (handler == null) {
            log.warn("No handler found for department: {}, skip auto dispatch", dept.getName());
            return false;
        }

        try {
            eventProcessService.assignTask(eventId, null, String.valueOf(handler.getId()), null);
        } catch (Exception e) {
            log.error("Auto dispatch failed for event {}, error: {}", eventId, e.getMessage());
            return false;
        }

        updateDispatchRecordActual(eventId, result.getDepartmentCode(),
                result.getDepartmentName(), "AUTO_DISPATCHED", 1);

        log.info("Auto dispatched event {} to department {} (handler: {}, confidence: {})",
                eventId, result.getDepartmentName(), handler.getRealName(), result.getConfidence());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean adoptDispatch(Long eventId, Long dispatchRecordId, String actualDeptCode, String actualDeptName) {
        EventDispatchRecord record = dispatchRecordMapper.selectById(dispatchRecordId);
        if (record == null || !record.getEventId().equals(eventId)) {
            throw new BusinessException("分派记录不存在");
        }

        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        String deptCode = actualDeptCode != null ? actualDeptCode : record.getRecommendedDeptCode();
        String deptName = actualDeptName != null ? actualDeptName : record.getRecommendedDeptName();

        SysDept dept = findDeptByCode(deptCode);
        if (dept == null) {
            throw new BusinessException("部门不存在: " + deptCode);
        }

        SysUser handler = findDefaultHandlerForDept(dept.getId());
        if (handler == null) {
            throw new BusinessException("部门 " + deptName + " 没有可用的处置员");
        }

        record.setAdopted(1);
        record.setActualDeptCode(deptCode);
        record.setActualDeptName(deptName);
        record.setStatus("ADOPTED");
        record.setFeedback("一键采纳");
        dispatchRecordMapper.updateById(record);

        try {
            eventProcessService.assignTask(eventId, null, String.valueOf(handler.getId()), null);
        } catch (Exception e) {
            log.error("Adopt dispatch failed for event {}, error: {}", eventId, e.getMessage());
            throw new BusinessException("任务分派失败: " + e.getMessage());
        }

        log.info("Adopted dispatch for event {}, dept: {}, handler: {}", eventId, deptName, handler.getRealName());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectDispatch(Long eventId, Long dispatchRecordId, String feedback) {
        EventDispatchRecord record = dispatchRecordMapper.selectById(dispatchRecordId);
        if (record == null || !record.getEventId().equals(eventId)) {
            throw new BusinessException("分派记录不存在");
        }

        record.setAdopted(0);
        record.setFeedback(feedback);
        record.setStatus("REJECTED");
        dispatchRecordMapper.updateById(record);

        log.info("Rejected dispatch for event {}, feedback: {}", eventId, feedback);
        return true;
    }

    @Override
    public List<Map<String, Object>> getDispatchHistory(Long eventId) {
        LambdaQueryWrapper<EventDispatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDispatchRecord::getEventId, eventId);
        wrapper.orderByDesc(EventDispatchRecord::getCreatedAt);
        List<EventDispatchRecord> records = dispatchRecordMapper.selectList(wrapper);

        return records.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("eventId", r.getEventId());
            map.put("recommendedDeptCode", r.getRecommendedDeptCode());
            map.put("recommendedDeptName", r.getRecommendedDeptName());
            map.put("confidence", r.getConfidence());
            map.put("autoDispatch", r.getAutoDispatch());
            map.put("dispatchMethod", r.getDispatchMethod());
            map.put("actualDeptCode", r.getActualDeptCode());
            map.put("actualDeptName", r.getActualDeptName());
            map.put("status", r.getStatus());
            map.put("adopted", r.getAdopted());
            map.put("feedback", r.getFeedback());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTrainingData(int limit) {
        LambdaQueryWrapper<EventDispatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDispatchRecord::getAdopted, 1);
        wrapper.isNotNull(EventDispatchRecord::getActualDeptCode);
        wrapper.orderByDesc(EventDispatchRecord::getCreatedAt);
        wrapper.last("LIMIT " + Math.min(limit, 10000));
        List<EventDispatchRecord> records = dispatchRecordMapper.selectList(wrapper);

        List<Map<String, Object>> trainingData = new ArrayList<>();
        for (EventDispatchRecord record : records) {
            EventInfo eventInfo = eventInfoMapper.selectById(record.getEventId());
            if (eventInfo == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", eventInfo.getTitle());
            item.put("description", eventInfo.getDescription());
            item.put("event_type", eventInfo.getEventType());
            item.put("dept_code", record.getActualDeptCode());
            trainingData.add(item);
        }

        return trainingData;
    }

    @Override
    public Map<String, Object> triggerModelTraining(List<Map<String, Object>> records, int epochs, int batchSize, double learningRate) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.set("records", records);
            requestBody.set("epochs", epochs);
            requestBody.set("batch_size", batchSize);
            requestBody.set("learning_rate", learningRate);

            HttpResponse response = HttpRequest.post(nlpServiceUrl + "/train")
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(300000)
                    .execute();

            if (response.isOk()) {
                return JSONUtil.parseObj(response.body()).toBean(Map.class);
            }
            throw new BusinessException("NLP训练请求失败: " + response.getStatus());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("NLP训练请求异常: " + e.getMessage());
        }
    }

    @Override
    public boolean isNlpServiceAvailable() {
        if (!nlpDispatchEnabled) return false;
        try {
            HttpResponse response = HttpRequest.get(nlpServiceUrl + "/health")
                    .timeout(3000)
                    .execute();
            return response.isOk();
        } catch (Exception e) {
            return false;
        }
    }

    private NlpDispatchResultVO parseClassifyResult(JSONObject data) {
        NlpDispatchResultVO result = new NlpDispatchResultVO();
        result.setDepartmentCode(data.getStr("department_code"));
        result.setDepartmentName(data.getStr("department_name"));
        result.setConfidence(data.getBigDecimal("confidence"));
        result.setAutoDispatch(data.getBool("auto_dispatch"));
        result.setMethod(data.getStr("method"));

        JSONArray scores = data.getJSONArray("all_scores");
        if (scores != null && !scores.isEmpty()) {
            List<NlpDispatchResultVO.DeptScoreVO> scoreList = new ArrayList<>();
            for (int i = 0; i < scores.size(); i++) {
                JSONObject scoreObj = scores.getJSONObject(i);
                NlpDispatchResultVO.DeptScoreVO scoreVO = new NlpDispatchResultVO.DeptScoreVO();
                scoreVO.setDepartmentCode(scoreObj.getStr("department_code"));
                scoreVO.setDepartmentName(scoreObj.getStr("department_name"));
                scoreVO.setScore(scoreObj.getBigDecimal("score"));
                scoreList.add(scoreVO);
            }
            result.setAllScores(scoreList);
        }
        return result;
    }

    private NlpDispatchResultVO buildFallbackResult(String eventType) {
        NlpDispatchResultVO result = new NlpDispatchResultVO();
        Map<String, String[]> typeDeptMap = new HashMap<>();
        typeDeptMap.put("environment", new String[]{"ENVIRONMENTAL", "环卫科"});
        typeDeptMap.put("public_facility", new String[]{"MUNICIPAL", "市政科"});
        typeDeptMap.put("dispute", new String[]{"CIVIL_AFFAIRS", "民政科"});
        typeDeptMap.put("safety_hazard", new String[]{"SAFETY", "安监科"});
        typeDeptMap.put("security", new String[]{"PUBLIC_SECURITY", "治安科"});
        typeDeptMap.put("service", new String[]{"CIVIL_AFFAIRS", "民政科"});
        typeDeptMap.put("traffic", new String[]{"TRAFFIC", "交通科"});

        String[] deptInfo = typeDeptMap.getOrDefault(eventType, new String[]{"OTHER", "综合协调科"});
        result.setDepartmentCode(deptInfo[0]);
        result.setDepartmentName(deptInfo[1]);
        result.setConfidence(BigDecimal.valueOf(0.3));
        result.setAutoDispatch(false);
        result.setMethod("fallback");
        return result;
    }

    private Long saveDispatchRecord(Long eventId, NlpDispatchResultVO result) {
        EventDispatchRecord record = new EventDispatchRecord();
        record.setEventId(eventId);
        record.setRecommendedDeptCode(result.getDepartmentCode());
        record.setRecommendedDeptName(result.getDepartmentName());
        record.setConfidence(result.getConfidence());
        record.setAutoDispatch(Boolean.TRUE.equals(result.getAutoDispatch()) ? 1 : 0);
        record.setDispatchMethod(result.getMethod());
        record.setStatus("RECOMMENDED");
        record.setAdopted(0);

        if (CollUtil.isNotEmpty(result.getAllScores())) {
            record.setModelScores(JSONUtil.toJsonStr(result.getAllScores()));
        }

        dispatchRecordMapper.insert(record);
        log.info("Saved dispatch record for event {}, recommended: {}, confidence: {}",
                eventId, result.getDepartmentName(), result.getConfidence());
        return record.getId();
    }

    private void updateDispatchRecordActual(Long eventId, String deptCode, String deptName, String status, Integer adopted) {
        LambdaQueryWrapper<EventDispatchRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventDispatchRecord::getEventId, eventId);
        wrapper.orderByDesc(EventDispatchRecord::getCreatedAt);
        wrapper.last("LIMIT 1");
        EventDispatchRecord record = dispatchRecordMapper.selectOne(wrapper);
        if (record != null) {
            record.setActualDeptCode(deptCode);
            record.setActualDeptName(deptName);
            record.setStatus(status);
            record.setAdopted(adopted);
            dispatchRecordMapper.updateById(record);
        }
    }

    private SysDept findDeptByCode(String deptCode) {
        if (StrUtil.isBlank(deptCode)) return null;
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getCode, deptCode);
        wrapper.last("LIMIT 1");
        return sysDeptMapper.selectOne(wrapper);
    }

    private SysUser findDefaultHandlerForDept(Long deptId) {
        if (deptId == null) return null;
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, deptId);
        wrapper.eq(SysUser::getRole, "handler");
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.last("LIMIT 1");
        return sysUserMapper.selectOne(wrapper);
    }
}
