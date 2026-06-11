package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysDept;
import com.gov.grid.mapper.EventEvaluationMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventProcessMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.SysDeptMapper;
import com.gov.grid.service.StatisticsService;
import com.gov.grid.vo.DeptStatsVO;
import com.gov.grid.vo.EventTrendVO;
import com.gov.grid.vo.EventTypeStatsVO;
import com.gov.grid.vo.GridStatsVO;
import com.gov.grid.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final EventInfoMapper eventInfoMapper;
    private final EventEvaluationMapper evaluationMapper;
    private final EventProcessMapper eventProcessMapper;
    private final SysDeptMapper sysDeptMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public StatisticsVO getOverviewStats() {
        StatisticsVO vo = new StatisticsVO();

        LambdaQueryWrapper<EventInfo> totalWrapper = new LambdaQueryWrapper<>();
        vo.setTotalEvents(eventInfoMapper.selectCount(totalWrapper));

        LambdaQueryWrapper<EventInfo> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(EventInfo::getStatus, "PENDING");
        vo.setPendingCount(eventInfoMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<EventInfo> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.and(w -> w
                .eq(EventInfo::getStatus, "PROCESSING")
                .or().eq(EventInfo::getStatus, "APPROVED")
                .or().eq(EventInfo::getStatus, "DISPATCHED")
                .or().eq(EventInfo::getStatus, "HANDLED"));
        vo.setProcessingCount(eventInfoMapper.selectCount(processingWrapper));

        LambdaQueryWrapper<EventInfo> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(EventInfo::getStatus, "COMPLETED");
        Long completedCount = eventInfoMapper.selectCount(completedWrapper);
        vo.setCompletedCount(completedCount);

        vo.setAvgHandleTime(calculateAvgHandleTime());
        vo.setAvgScore(calculateAvgScore());
        vo.setReworkRate(calculateReworkRate());

        return vo;
    }

    @Override
    public List<EventTrendVO> getEventTrend(Integer days) {
        int dayCount = days != null ? days : 7;
        List<EventTrendVO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDateTime now = LocalDateTime.now();
        for (int i = dayCount - 1; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            String dateStr = dayStart.format(formatter);

            LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(EventInfo::getCreatedAt, dayStart)
                    .lt(EventInfo::getCreatedAt, dayEnd);
            Long count = eventInfoMapper.selectCount(wrapper);

            LambdaQueryWrapper<EventInfo> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.ge(EventInfo::getUpdatedAt, dayStart)
                    .lt(EventInfo::getUpdatedAt, dayEnd)
                    .eq(EventInfo::getStatus, "COMPLETED");
            Long completedCount = eventInfoMapper.selectCount(completedWrapper);

            EventTrendVO vo = new EventTrendVO();
            vo.setDate(dateStr);
            vo.setCount(count);
            vo.setCompletedCount(completedCount);
            result.add(vo);
        }

        return result;
    }

    @Override
    public List<EventTypeStatsVO> getEventTypeStats() {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(EventInfo::getEventType);
        List<EventInfo> allEvents = eventInfoMapper.selectList(wrapper);

        Long total = (long) allEvents.size();

        Map<String, Long> typeCountMap = allEvents.stream()
                .filter(e -> e.getEventType() != null)
                .collect(Collectors.groupingBy(EventInfo::getEventType, Collectors.counting()));

        List<EventTypeStatsVO> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCountMap.entrySet()) {
            EventTypeStatsVO vo = new EventTypeStatsVO();
            vo.setEventType(entry.getKey());
            vo.setEventTypeName(resolveEventTypeName(entry.getKey()));
            vo.setCount(entry.getValue());
            if (total > 0) {
                double percentage = (entry.getValue() * 100.0) / total;
                vo.setPercentage(BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).doubleValue());
            } else {
                vo.setPercentage(0.0);
            }
            result.add(vo);
        }

        result.sort(Comparator.comparingLong(EventTypeStatsVO::getCount).reversed());
        return result;
    }

    @Override
    public List<DeptStatsVO> getDeptStats() {
        List<SysDept> deptList = sysDeptMapper.selectList(null);
        List<DeptStatsVO> result = new ArrayList<>();

        List<EventInfo> allCompletedEvents = eventInfoMapper.selectList(
                new LambdaQueryWrapper<EventInfo>().eq(EventInfo::getStatus, "COMPLETED"));

        List<EventEvaluation> allEvaluations = evaluationMapper.selectList(null);
        Map<Long, EventEvaluation> evalMap = allEvaluations.stream()
                .collect(Collectors.toMap(EventEvaluation::getEventId, e -> e, (a, b) -> a));

        Map<Long, List<Long>> deptEventMap = buildDeptEventMap();

        for (SysDept dept : deptList) {
            DeptStatsVO vo = new DeptStatsVO();
            vo.setDeptId(dept.getId());
            vo.setDeptName(dept.getName());

            List<Long> deptEventIds = deptEventMap.getOrDefault(dept.getId(), new ArrayList<>());
            vo.setHandleCount((long) deptEventIds.size());

            double totalDuration = 0.0;
            int durationCount = 0;
            int reworkCount = 0;
            double totalScore = 0.0;
            int scoreCount = 0;

            for (Long eventId : deptEventIds) {
                LambdaQueryWrapper<EventProcess> processWrapper = new LambdaQueryWrapper<>();
                processWrapper.eq(EventProcess::getEventId, eventId)
                        .orderByAsc(EventProcess::getCreatedAt);
                List<EventProcess> processes = eventProcessMapper.selectList(processWrapper);

                if (!processes.isEmpty()) {
                    LocalDateTime first = processes.get(0).getHandleTime();
                    LocalDateTime last = processes.get(processes.size() - 1).getHandleTime();
                    if (first != null && last != null) {
                        long seconds = java.time.Duration.between(first, last).getSeconds();
                        totalDuration += seconds / 3600.0;
                        durationCount++;
                    }
                }

                long rejectCount = processes.stream()
                        .filter(p -> "REJECT".equals(p.getAction()) || "RETURN".equals(p.getAction()))
                        .count();
                if (rejectCount > 0) {
                    reworkCount++;
                }

                EventEvaluation eval = evalMap.get(eventId);
                if (eval != null) {
                    Integer speed = eval.getSpeedScore();
                    Integer effect = eval.getEffectScore();
                    if (speed != null && effect != null) {
                        totalScore += (speed + effect) / 2.0;
                        scoreCount++;
                    }
                }
            }

            vo.setAvgDuration(durationCount > 0 ?
                    BigDecimal.valueOf(totalDuration / durationCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

            long handleCount = vo.getHandleCount();
            vo.setReworkRate(handleCount > 0 ?
                    BigDecimal.valueOf((reworkCount * 100.0) / handleCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

            vo.setAvgScore(scoreCount > 0 ?
                    BigDecimal.valueOf(totalScore / scoreCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

            result.add(vo);
        }

        result.sort(Comparator.comparingLong(DeptStatsVO::getHandleCount).reversed());
        return result;
    }

    @Override
    public List<GridStatsVO> getGridStats() {
        List<GridInfo> gridList = gridInfoMapper.selectList(null);
        List<GridStatsVO> result = new ArrayList<>();

        List<EventInfo> allEvents = eventInfoMapper.selectList(null);
        Map<Long, List<EventInfo>> gridEventMap = allEvents.stream()
                .filter(e -> e.getGridId() != null)
                .collect(Collectors.groupingBy(EventInfo::getGridId));

        List<EventEvaluation> allEvaluations = evaluationMapper.selectList(null);
        Map<Long, EventEvaluation> evalMap = allEvaluations.stream()
                .collect(Collectors.toMap(EventEvaluation::getEventId, e -> e, (a, b) -> a));

        for (GridInfo grid : gridList) {
            GridStatsVO vo = new GridStatsVO();
            vo.setGridId(grid.getId());
            vo.setGridName(grid.getGridName());
            vo.setGridCode(grid.getGridCode());

            List<EventInfo> gridEvents = gridEventMap.getOrDefault(grid.getId(), new ArrayList<>());
            vo.setTotalCount((long) gridEvents.size());

            long pendingCount = gridEvents.stream()
                    .filter(e -> "PENDING".equals(e.getStatus()))
                    .count();
            vo.setPendingCount(pendingCount);

            long processingCount = gridEvents.stream()
                    .filter(e -> !"PENDING".equals(e.getStatus()) && !"COMPLETED".equals(e.getStatus()) && !"REJECTED".equals(e.getStatus()))
                    .count();
            vo.setProcessingCount(processingCount);

            long completedCount = gridEvents.stream()
                    .filter(e -> "COMPLETED".equals(e.getStatus()))
                    .count();
            vo.setCompletedCount(completedCount);

            double totalScore = 0.0;
            int scoreCount = 0;
            for (EventInfo event : gridEvents) {
                EventEvaluation eval = evalMap.get(event.getId());
                if (eval != null && eval.getSpeedScore() != null && eval.getEffectScore() != null) {
                    totalScore += (eval.getSpeedScore() + eval.getEffectScore()) / 2.0;
                    scoreCount++;
                }
            }
            vo.setAvgScore(scoreCount > 0 ?
                    BigDecimal.valueOf(totalScore / scoreCount).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);

            result.add(vo);
        }

        result.sort(Comparator.comparingLong(GridStatsVO::getTotalCount).reversed());
        return result;
    }

    private Double calculateAvgHandleTime() {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getStatus, "COMPLETED");
        List<EventInfo> completedEvents = eventInfoMapper.selectList(wrapper);

        if (completedEvents.isEmpty()) {
            return 0.0;
        }

        double totalHours = 0.0;
        int count = 0;

        for (EventInfo event : completedEvents) {
            LocalDateTime createdAt = event.getCreatedAt();
            LocalDateTime updatedAt = event.getUpdatedAt();
            if (createdAt != null && updatedAt != null) {
                long seconds = java.time.Duration.between(createdAt, updatedAt).getSeconds();
                totalHours += seconds / 3600.0;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(totalHours / count).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double calculateAvgScore() {
        List<EventEvaluation> allEvaluations = evaluationMapper.selectList(null);
        if (allEvaluations.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        int count = 0;
        for (EventEvaluation eval : allEvaluations) {
            if (eval.getSpeedScore() != null && eval.getEffectScore() != null) {
                totalScore += (eval.getSpeedScore() + eval.getEffectScore()) / 2.0;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(totalScore / count).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Double calculateReworkRate() {
        LambdaQueryWrapper<EventInfo> eventWrapper = new LambdaQueryWrapper<>();
        eventWrapper.eq(EventInfo::getStatus, "COMPLETED");
        Long totalCompleted = eventInfoMapper.selectCount(eventWrapper);

        if (totalCompleted == 0) {
            return 0.0;
        }

        LambdaQueryWrapper<EventProcess> processWrapper = new LambdaQueryWrapper<>();
        processWrapper.eq(EventProcess::getAction, "REJECT")
                .or().eq(EventProcess::getAction, "RETURN");
        List<EventProcess> rejectProcesses = eventProcessMapper.selectList(processWrapper);
        long reworkEventCount = rejectProcesses.stream()
                .map(EventProcess::getEventId)
                .distinct()
                .count();

        return BigDecimal.valueOf((reworkEventCount * 100.0) / totalCompleted)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Map<Long, List<Long>> buildDeptEventMap() {
        Map<Long, List<Long>> deptEventMap = new HashMap<>();

        LambdaQueryWrapper<EventProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventProcess::getAction, "DISPATCH")
                .or().eq(EventProcess::getAction, "ASSIGN")
                .orderByAsc(EventProcess::getCreatedAt);
        List<EventProcess> dispatchProcesses = eventProcessMapper.selectList(wrapper);

        for (EventProcess p : dispatchProcesses) {
            if (p.getHandlerId() != null) {
                SysUser user = sysUserMapper.selectById(p.getHandlerId());
                if (user != null && user.getDeptId() != null) {
                    deptEventMap.computeIfAbsent(user.getDeptId(), k -> new ArrayList<>())
                            .add(p.getEventId());
                }
            }
        }

        return deptEventMap;
    }

    private String resolveEventTypeName(String eventType) {
        if (eventType == null) {
            return "未知";
        }
        switch (eventType) {
            case "environment":
                return "环境卫生";
            case "facility":
                return "市政设施";
            case "security":
                return "治安问题";
            case "service":
                return "民生服务";
            case "traffic":
                return "交通出行";
            case "public_facility":
                return "公共设施";
            case "dispute":
                return "矛盾纠纷";
            case "safety_hazard":
                return "安全隐患";
            case "other":
                return "其他问题";
            default:
                return eventType;
        }
    }
}
