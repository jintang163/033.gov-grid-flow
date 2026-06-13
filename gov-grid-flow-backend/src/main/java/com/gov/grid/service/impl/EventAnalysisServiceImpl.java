package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.entity.SysUser;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.RoleEnum;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.EventAnalysisService;
import com.gov.grid.service.NotificationService;
import com.gov.grid.vo.EventAnalysisReportVO;
import com.gov.grid.vo.EventGraphVO;
import com.gov.grid.vo.EventSimpleVO;
import com.gov.grid.vo.RecurrenceGroupVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventAnalysisServiceImpl implements EventAnalysisService {

    private static final int HIGH_RECURRENCE_THRESHOLD = 3;
    private static final double DISTANCE_KM_THRESHOLD = 0.3;
    private static final int DEFAULT_TOP_GROUPS = 10;

    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean analyzeAndMarkRecurrence(Long eventId) {
        EventInfo event = eventInfoMapper.selectById(eventId);
        if (event == null) {
            return false;
        }

        String groupKey = buildGroupKey(event);
        List<EventInfo> sameGroupEvents = findSimilarEvents(event);

        int totalCount = sameGroupEvents.size();
        event.setRecurrenceGroupKey(groupKey);
        event.setRecurrenceCount(totalCount);
        if (totalCount >= HIGH_RECURRENCE_THRESHOLD) {
            boolean changed = event.getIsHighRecurrence() == null || event.getIsHighRecurrence() != 1;
            event.setIsHighRecurrence(1);
            eventInfoMapper.updateById(event);

            sameGroupEvents.forEach(e -> {
                if (!e.getId().equals(eventId)) {
                    e.setRecurrenceGroupKey(groupKey);
                    e.setRecurrenceCount(totalCount);
                    if (e.getIsHighRecurrence() == null || e.getIsHighRecurrence() != 1) {
                        e.setIsHighRecurrence(1);
                    }
                    eventInfoMapper.updateById(e);
                }
            });

            if (changed) {
                try {
                    pushHighRecurrenceAlert(groupKey, event, totalCount);
                } catch (Exception e) {
                    log.error("推送高复发告警失败，事件ID：{}", eventId, e);
                }
            }
            return true;
        } else {
            event.setIsHighRecurrence(0);
            eventInfoMapper.updateById(event);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchScanAndMarkHighRecurrence() {
        List<EventInfo> activeEvents = eventInfoMapper.selectList(
                new LambdaQueryWrapper<EventInfo>()
                        .eq(EventInfo::getDeleted, 0)
                        .isNotNull(EventInfo::getLng)
                        .isNotNull(EventInfo::getLat)
                        .gt(EventInfo::getCreatedAt, LocalDate.now().minusDays(90).atStartOfDay())
                        .orderByDesc(EventInfo::getCreatedAt)
        );

        Map<String, List<EventInfo>> groupMap = new HashMap<>();
        for (EventInfo e : activeEvents) {
            try {
                String key = buildGroupKey(e);
                groupMap.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
            } catch (Exception ignore) {
            }
        }

        int flagged = 0;
        for (Map.Entry<String, List<EventInfo>> entry : groupMap.entrySet()) {
            List<EventInfo> list = entry.getValue();
            int size = list.size();
            if (size >= HIGH_RECURRENCE_THRESHOLD) {
                for (EventInfo e : list) {
                    boolean shouldNotify = e.getIsHighRecurrence() == null || e.getIsHighRecurrence() != 1;
                    e.setRecurrenceGroupKey(entry.getKey());
                    e.setRecurrenceCount(size);
                    e.setIsHighRecurrence(1);
                    eventInfoMapper.updateById(e);
                    if (shouldNotify) {
                        try {
                            pushHighRecurrenceAlert(entry.getKey(), e, size);
                        } catch (Exception ignore) {
                        }
                    }
                }
                flagged++;
            } else {
                for (EventInfo e : list) {
                    e.setRecurrenceGroupKey(entry.getKey());
                    e.setRecurrenceCount(size);
                    if (e.getIsHighRecurrence() == null || e.getIsHighRecurrence() != 0) {
                        e.setIsHighRecurrence(0);
                    }
                    eventInfoMapper.updateById(e);
                }
            }
        }
        log.info("[EventAnalysis] 批量扫描高复发事件完成，标记了{}组高复发事件", flagged);
        return flagged;
    }

    @Override
    public List<RecurrenceGroupVO> listHighRecurrenceGroups(Integer days) {
        LocalDateTime startTime = days != null && days > 0
                ? LocalDate.now().minusDays(days).atStartOfDay()
                : LocalDate.now().minusDays(90).atStartOfDay();

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getDeleted, 0)
                .eq(EventInfo::getIsHighRecurrence, 1)
                .ge(EventInfo::getCreatedAt, startTime)
                .orderByDesc(EventInfo::getCreatedAt);
        List<EventInfo> events = eventInfoMapper.selectList(wrapper);

        Map<String, List<EventInfo>> groups = events.stream()
                .collect(Collectors.groupingBy(
                        e -> StrUtil.isNotBlank(e.getRecurrenceGroupKey()) ? e.getRecurrenceGroupKey() : buildGroupKey(e),
                        Collectors.toList()
                ));

        List<RecurrenceGroupVO> result = new ArrayList<>();
        for (Map.Entry<String, List<EventInfo>> entry : groups.entrySet()) {
            RecurrenceGroupVO vo = toGroupVO(entry.getKey(), entry.getValue());
            result.add(vo);
        }
        result.sort((a, b) -> Integer.compare(b.getTotalCount(), a.getTotalCount()));
        return result.stream().limit(DEFAULT_TOP_GROUPS).collect(Collectors.toList());
    }

    @Override
    public RecurrenceGroupVO getRecurrenceGroup(String groupKey) {
        if (StrUtil.isBlank(groupKey)) {
            return null;
        }
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getDeleted, 0)
                .eq(EventInfo::getRecurrenceGroupKey, groupKey)
                .orderByAsc(EventInfo::getCreatedAt);
        List<EventInfo> events = eventInfoMapper.selectList(wrapper);
        if (CollUtil.isEmpty(events)) {
            return null;
        }
        return toGroupVO(groupKey, events);
    }

    @Override
    public EventGraphVO getEventRelationGraph(Long eventId, Integer depth) {
        EventInfo event = eventInfoMapper.selectById(eventId);
        if (event == null) {
            return null;
        }
        int d = depth == null || depth < 1 ? 2 : Math.min(depth, 4);

        List<EventInfo> sameGroup = findSimilarEvents(event);

        EventGraphVO vo = new EventGraphVO();
        vo.setEventId(event.getId());
        vo.setEventNo(event.getEventNo());
        vo.setTitle(event.getTitle());

        List<EventGraphVO.GraphNode> nodes = new ArrayList<>();
        List<EventGraphVO.GraphEdge> edges = new ArrayList<>();
        Set<String> nodeIds = new HashSet<>();

        String selfId = "event_" + event.getId();
        EventGraphVO.GraphNode selfNode = buildNode(selfId, event, "event", 50, 0);
        nodes.add(selfNode);
        nodeIds.add(selfId);

        if (CollUtil.isNotEmpty(sameGroup)) {
            String groupId = "group_" + buildGroupKey(event);
            if (!nodeIds.contains(groupId)) {
                EventGraphVO.GraphNode groupNode = new EventGraphVO.GraphNode();
                groupNode.setId(groupId);
                groupNode.setLabel("高复发组(" + sameGroup.size() + "次)");
                groupNode.setType("group");
                groupNode.setSymbolSize(40);
                groupNode.setCategory(2);
                groupNode.setDescription("同类型同地点复发事件分组");
                nodes.add(groupNode);
                nodeIds.add(groupId);
            }
            EventGraphVO.GraphEdge selfToGroup = new EventGraphVO.GraphEdge();
            selfToGroup.setSource(selfId);
            selfToGroup.setTarget(groupId);
            selfToGroup.setLabel("属于");
            selfToGroup.setRelationType("GROUP");
            edges.add(selfToGroup);

            for (EventInfo related : sameGroup) {
                if (related.getId().equals(eventId)) {
                    continue;
                }
                String relId = "event_" + related.getId();
                if (!nodeIds.contains(relId)) {
                    nodes.add(buildNode(relId, related, "event", 35, 1));
                    nodeIds.add(relId);
                }
                EventGraphVO.GraphEdge edge = new EventGraphVO.GraphEdge();
                edge.setSource(relId);
                edge.setTarget(groupId);
                edge.setLabel("复发");
                edge.setRelationType("RECURRENCE");
                edges.add(edge);

                if (d >= 2) {
                    EventInfo reporterNode = new EventInfo();
                    reporterNode.setId(-related.getId());
                    reporterNode.setTitle("上报人：" + (related.getReporterName() != null ? related.getReporterName() : "匿名"));
                    String reporterId = "reporter_" + related.getReporterId();
                    if (related.getReporterId() != null && !nodeIds.contains(reporterId)) {
                        EventGraphVO.GraphNode rp = new EventGraphVO.GraphNode();
                        rp.setId(reporterId);
                        rp.setLabel(related.getReporterName() != null ? related.getReporterName() : "匿名");
                        rp.setType("reporter");
                        rp.setCategory(3);
                        rp.setSymbolSize(28);
                        nodes.add(rp);
                        nodeIds.add(reporterId);
                    }
                    if (related.getReporterId() != null) {
                        EventGraphVO.GraphEdge rpEdge = new EventGraphVO.GraphEdge();
                        rpEdge.setSource(reporterId);
                        rpEdge.setTarget(relId);
                        rpEdge.setLabel("上报");
                        rpEdge.setRelationType("REPORT");
                        edges.add(rpEdge);
                    }
                }
            }
        }

        if (d >= 2 && event.getGridId() != null) {
            GridInfo grid = gridInfoMapper.selectById(event.getGridId());
            if (grid != null) {
                String gridIdNode = "grid_" + grid.getId();
                if (!nodeIds.contains(gridIdNode)) {
                    EventGraphVO.GraphNode gn = new EventGraphVO.GraphNode();
                    gn.setId(gridIdNode);
                    gn.setLabel(grid.getGridName());
                    gn.setType("grid");
                    gn.setCategory(4);
                    gn.setSymbolSize(38);
                    gn.setDescription("所属网格：" + grid.getAddress());
                    nodes.add(gn);
                    nodeIds.add(gridIdNode);
                }
                EventGraphVO.GraphEdge gridEdge = new EventGraphVO.GraphEdge();
                gridEdge.setSource(selfId);
                gridEdge.setTarget(gridIdNode);
                gridEdge.setLabel("位于");
                gridEdge.setRelationType("GRID");
                edges.add(gridEdge);
            }
        }

        vo.setNodes(nodes);
        vo.setEdges(edges);
        vo.setRecurrenceGroup(toGroupVO(buildGroupKey(event), sameGroup));

        log.info("[EventAnalysis] 生成关联图谱，事件ID：{}，节点数：{}，边数：{}", eventId, nodes.size(), edges.size());
        return vo;
    }

    @Override
    public EventAnalysisReportVO generateAnalysisReport(Integer days) {
        int d = days == null || days < 1 ? 7 : days;
        LocalDateTime startTime = LocalDate.now().minusDays(d).atStartOfDay();

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getDeleted, 0).ge(EventInfo::getCreatedAt, startTime);
        List<EventInfo> events = eventInfoMapper.selectList(wrapper);

        List<RecurrenceGroupVO> topGroups = listHighRecurrenceGroups(d);
        int highRecurrenceCount = events.stream()
                .filter(e -> e.getIsHighRecurrence() != null && e.getIsHighRecurrence() == 1)
                .toList()
                .size();
        int totalGroups = topGroups.size();

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("近%d天共上报事件%d件，识别出高复发事件%d件，涉及高复发地点%d处。",
                d, events.size(), highRecurrenceCount, totalGroups));
        if (totalGroups > 0) {
            RecurrenceGroupVO top = topGroups.get(0);
            summary.append(String.format("其中最高复发地点：%s（%s），共发生%d次。",
                    top.getAddress() != null ? top.getAddress() : "未知",
                    top.getEventType() != null ? top.getEventType() : "未分类",
                    top.getTotalCount()));
        }

        StringBuilder suggestions = new StringBuilder();
        suggestions.append("建议：");
        if (highRecurrenceCount > 0) {
            suggestions.append("1）对高复发地点进行专项巡查与整改；2）将高复发事件纳入月度考核重点；3）升级督办相关责任部门。");
        } else {
            suggestions.append("当前事件复发率处于正常水平，请保持日常巡查。");
        }

        EventAnalysisReportVO report = new EventAnalysisReportVO();
        report.setReportId(System.currentTimeMillis());
        report.setReportNo("RPT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + IdUtil.nanoId(6).toUpperCase());
        report.setGeneratedAt(LocalDateTime.now());
        report.setPeriod(d + "天");
        report.setTotalEvents(events.size());
        report.setHighRecurrenceCount(highRecurrenceCount);
        report.setTotalRecurrenceGroups(totalGroups);
        report.setTopRecurrenceGroups(topGroups.stream().limit(5).collect(Collectors.toList()));
        report.setSummary(summary.toString());
        report.setSuggestions(suggestions.toString());

        return report;
    }

    @Override
    public boolean pushReportToStreetOffice(EventAnalysisReportVO report) {
        if (report == null) {
            return false;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, RoleEnum.ADMIN.getCode(), RoleEnum.STREET_MANAGER.getCode())
                .eq(SysUser::getStatus, 1);
        List<SysUser> receivers = sysUserMapper.selectList(wrapper);
        if (CollUtil.isEmpty(receivers)) {
            log.warn("[EventAnalysis] 街道办无管理员用户可接收报告");
            return false;
        }

        String title = "事件关联分析报告 - " + report.getPeriod();
        String content = String.format(
                "报告编号：%s\n生成时间：%s\n统计周期：%s\n事件总数：%d件\n高复发事件：%d件\n涉及地点：%d处\n\n%s\n\n%s",
                report.getReportNo(),
                report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                report.getPeriod(),
                report.getTotalEvents(),
                report.getHighRecurrenceCount(),
                report.getTotalRecurrenceGroups(),
                report.getSummary(),
                report.getSuggestions()
        );

        boolean allOk = true;
        for (SysUser user : receivers) {
            try {
                SysNotification notification = new SysNotification();
                notification.setUserId(user.getId());
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType("ANALYSIS_REPORT");
                notification.setBizId(report.getReportId());
                notification.setIsRead(0);
                sysNotificationMapper.insert(notification);

                boolean sendOk = notificationService.sendByChannel(
                        "ALL",
                        user.getId(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getEmail(),
                        title,
                        content,
                        "ANALYSIS_REPORT",
                        report.getReportId()
                );
                if (!sendOk) {
                    allOk = false;
                }
            } catch (Exception e) {
                allOk = false;
                log.error("推送报告给用户失败，userId：{}", user.getId(), e);
            }
        }
        log.info("[EventAnalysis] 报告已推送到街道办，报告编号：{}，接收人数：{}，结果：{}",
                report.getReportNo(), receivers.size(), allOk ? "全部成功" : "部分失败");
        return allOk;
    }

    private List<EventInfo> findSimilarEvents(EventInfo event) {
        if (event.getLng() == null || event.getLat() == null) {
            return Collections.singletonList(event);
        }
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getDeleted, 0)
                .eq(EventInfo::getEventType, event.getEventType())
                .ge(EventInfo::getCreatedAt, LocalDate.now().minusDays(90).atStartOfDay());
        if (event.getGridId() != null) {
            wrapper.eq(EventInfo::getGridId, event.getGridId());
        }
        List<EventInfo> candidates = eventInfoMapper.selectList(wrapper);
        if (CollUtil.isEmpty(candidates)) {
            return Collections.singletonList(event);
        }
        List<EventInfo> result = new ArrayList<>();
        for (EventInfo c : candidates) {
            if (c.getLng() == null || c.getLat() == null) {
                continue;
            }
            double distance = haversineKm(event.getLng(), event.getLat(), c.getLng(), c.getLat());
            if (distance <= DISTANCE_KM_THRESHOLD) {
                result.add(c);
            }
        }
        if (result.isEmpty()) {
            result.add(event);
        }
        result.sort(Comparator.comparing(EventInfo::getCreatedAt));
        return result;
    }

    private String buildGroupKey(EventInfo event) {
        if (event.getLng() == null || event.getLat() == null) {
            return StrUtil.blankToDefault(event.getEventType(), "unknown") + "_noloc_" + (event.getGridId() != null ? event.getGridId() : "0");
        }
        String lng3 = event.getLng().setScale(3, RoundingMode.HALF_UP).toPlainString();
        String lat3 = event.getLat().setScale(3, RoundingMode.HALF_UP).toPlainString();
        return StrUtil.blankToDefault(event.getEventType(), "unknown") + "_" + lng3 + "_" + lat3;
    }

    private EventGraphVO.GraphNode buildNode(String id, EventInfo event, String type, Integer symbolSize, Integer category) {
        EventGraphVO.GraphNode n = new EventGraphVO.GraphNode();
        n.setId(id);
        String titleShort = StrUtil.sub(event.getTitle() != null ? event.getTitle() : "-", 0, 12);
        n.setLabel(titleShort);
        n.setType(type);
        n.setCategory(category);
        n.setSymbolSize(symbolSize);
        n.setDescription(String.format(
                "事件号：%s | 状态：%s | 时间：%s",
                event.getEventNo() != null ? event.getEventNo() : "-",
                event.getStatus() != null ? event.getStatus() : "-",
                event.getCreatedAt() != null ? event.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-"
        ));
        if (event.getLng() != null) n.setX(event.getLng().doubleValue());
        if (event.getLat() != null) n.setY(event.getLat().doubleValue());
        return n;
    }

    private RecurrenceGroupVO toGroupVO(String groupKey, List<EventInfo> events) {
        RecurrenceGroupVO vo = new RecurrenceGroupVO();
        vo.setGroupKey(groupKey);
        if (CollUtil.isEmpty(events)) {
            vo.setTotalCount(0);
            return vo;
        }
        EventInfo first = events.get(0);
        vo.setEventType(first.getEventType());
        vo.setAddress(first.getAddress());
        if (first.getLng() != null) vo.setLng(first.getLng().doubleValue());
        if (first.getLat() != null) vo.setLat(first.getLat().doubleValue());
        vo.setTotalCount(events.size());
        vo.setPendingCount((int) events.stream().filter(e -> Objects.equals(e.getStatus(), EventStatus.PENDING.getCode())
                || Objects.equals(e.getStatus(), EventStatus.APPROVED.getCode())
                || Objects.equals(e.getStatus(), EventStatus.DISPATCHED.getCode())).count());
        vo.setCompletedCount((int) events.stream().filter(e -> Objects.equals(e.getStatus(), EventStatus.COMPLETED.getCode())).count());
        vo.setOverdueCount((int) events.stream()
                .filter(e -> e.getDeadlineAt() != null && e.getDeadlineAt().isBefore(LocalDateTime.now())
                        && !Objects.equals(e.getStatus(), EventStatus.COMPLETED.getCode())).count());
        vo.setFirstOccurAt(events.get(0).getCreatedAt());
        vo.setLastOccurAt(events.get(events.size() - 1).getCreatedAt());
        vo.setEvents(events.stream().map(this::toSimpleVO).collect(Collectors.toList()));
        return vo;
    }

    private EventSimpleVO toSimpleVO(EventInfo e) {
        EventSimpleVO vo = new EventSimpleVO();
        BeanUtils.copyProperties(e, vo);
        vo.setId(e.getId());
        vo.setEventNo(e.getEventNo());
        vo.setTitle(e.getTitle());
        vo.setStatus(e.getStatus());
        vo.setPriority(e.getPriority());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setDeadlineAt(e.getDeadlineAt());
        vo.setUrgeLevel(e.getUrgeLevel());
        vo.setReporterName(e.getReporterName());
        return vo;
    }

    private void pushHighRecurrenceAlert(String groupKey, EventInfo event, int totalCount) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, RoleEnum.ADMIN.getCode(), RoleEnum.STREET_MANAGER.getCode())
                .eq(SysUser::getStatus, 1);
        List<SysUser> receivers = sysUserMapper.selectList(wrapper);

        String title = "【高复发事件告警】" + (event.getAddress() != null ? event.getAddress() : "未知地点");
        String content = String.format(
                "事件：%s（编号：%s）\n地点：%s\n类型：%s\n累计复发次数：%d\n首次发生：%s\n请街道办及时关注并处置。",
                event.getTitle(),
                event.getEventNo(),
                event.getAddress() != null ? event.getAddress() : "-",
                event.getEventType(),
                totalCount,
                event.getCreatedAt() != null ? event.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-"
        );

        for (SysUser user : receivers) {
            try {
                SysNotification notification = new SysNotification();
                notification.setUserId(user.getId());
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType("HIGH_RECURRENCE_ALERT");
                notification.setBizId(event.getId());
                notification.setIsRead(0);
                sysNotificationMapper.insert(notification);

                notificationService.sendByChannel(
                        "ALL",
                        user.getId(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getEmail(),
                        title,
                        content,
                        "HIGH_RECURRENCE_ALERT",
                        event.getId()
                );
            } catch (Exception e) {
                log.error("推送高复发告警给用户失败，userId：{}", user.getId(), e);
            }
        }
    }

    private double haversineKm(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
