package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.GridMemberLocation;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.GridMemberLocationMapper;
import com.gov.grid.service.DashboardService;
import com.gov.grid.vo.CommunityRankVO;
import com.gov.grid.vo.DashboardOverviewVO;
import com.gov.grid.vo.EventMarkerVO;
import com.gov.grid.vo.GridMemberStatusVO;
import com.gov.grid.vo.HeatmapPointVO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final GridMemberLocationMapper gridMemberLocationMapper;

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public DashboardOverviewVO getDashboardOverview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        LambdaQueryWrapper<EventInfo> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(EventInfo::getCreatedAt, todayStart);
        vo.setTodayReported(eventInfoMapper.selectCount(todayWrapper));

        LambdaQueryWrapper<EventInfo> todayCompletedWrapper = new LambdaQueryWrapper<>();
        todayCompletedWrapper.ge(EventInfo::getUpdatedAt, todayStart)
                .eq(EventInfo::getStatus, EventStatus.COMPLETED.getCode());
        vo.setTodayCompleted(eventInfoMapper.selectCount(todayCompletedWrapper));

        LambdaQueryWrapper<EventInfo> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(EventInfo::getStatus, EventStatus.PENDING.getCode());
        vo.setPendingCount(eventInfoMapper.selectCount(pendingWrapper));

        LambdaQueryWrapper<EventInfo> processingWrapper = new LambdaQueryWrapper<>();
        processingWrapper.and(w -> w
                .eq(EventInfo::getStatus, EventStatus.APPROVED.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.DISPATCHED.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.HANDLED.getCode()));
        vo.setProcessingCount(eventInfoMapper.selectCount(processingWrapper));

        LambdaQueryWrapper<EventInfo> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(EventInfo::getStatus, EventStatus.COMPLETED.getCode());
        vo.setCompletedCount(eventInfoMapper.selectCount(completedWrapper));

        vo.setAvgHandleTime(calculateAvgHandleTime());
        vo.setOnlineRate(calculateOnlineRate());

        vo.setHourlyTrend(buildHourlyTrend(todayStart));

        return vo;
    }

    @Override
    public List<EventMarkerVO> getPendingEventMarkers() {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(EventInfo::getStatus, EventStatus.PENDING.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.APPROVED.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.DISPATCHED.getCode()));
        wrapper.isNotNull(EventInfo::getLng).isNotNull(EventInfo::getLat);

        List<EventInfo> events = eventInfoMapper.selectList(wrapper);
        return events.stream().map(this::toEventMarkerVO).collect(Collectors.toList());
    }

    @Override
    public List<HeatmapPointVO> getEventHeatmap() {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .eq(EventInfo::getStatus, EventStatus.PENDING.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.APPROVED.getCode())
                .or().eq(EventInfo::getStatus, EventStatus.DISPATCHED.getCode()));
        wrapper.isNotNull(EventInfo::getLng).isNotNull(EventInfo::getLat);
        List<EventInfo> events = eventInfoMapper.selectList(wrapper);

        Map<String, HeatmapPointVO> pointMap = new HashMap<>();
        for (EventInfo event : events) {
            if (event.getLng() == null || event.getLat() == null) continue;
            String key = event.getLng().setScale(3, RoundingMode.DOWN) + "_" + event.getLat().setScale(3, RoundingMode.DOWN);
            HeatmapPointVO point = pointMap.get(key);
            if (point == null) {
                point = new HeatmapPointVO();
                point.setLng(event.getLng().doubleValue());
                point.setLat(event.getLat().doubleValue());
                point.setCount(1);
                pointMap.put(key, point);
            } else {
                point.setCount(point.getCount() + 1);
            }
        }

        return new ArrayList<>(pointMap.values());
    }

    @Override
    public List<CommunityRankVO> getCommunityRank() {
        List<GridInfo> allGrids = gridInfoMapper.selectList(null);
        List<EventInfo> allEvents = eventInfoMapper.selectList(null);

        Map<Long, Long> gridParentMap = new HashMap<>();
        Map<Long, String> gridNameMap = new HashMap<>();
        for (GridInfo grid : allGrids) {
            gridNameMap.put(grid.getId(), grid.getGridName());
            if (grid.getParentId() != null) {
                gridParentMap.put(grid.getId(), grid.getParentId());
            }
        }

        Map<Long, Long> gridToCommunity = new HashMap<>();
        for (GridInfo grid : allGrids) {
            if (grid.getGridLevel() != null && grid.getGridLevel() == 2) {
                gridToCommunity.put(grid.getId(), grid.getId());
            }
        }

        for (GridInfo grid : allGrids) {
            if (gridToCommunity.containsKey(grid.getId())) continue;
            Long communityId = findAncestorCommunity(grid.getId(), gridParentMap, gridToCommunity);
            gridToCommunity.put(grid.getId(), communityId);
        }

        Map<Long, List<EventInfo>> communityEventMap = new HashMap<>();
        for (EventInfo event : allEvents) {
            if (event.getGridId() == null) continue;
            Long communityId = gridToCommunity.get(event.getGridId());
            if (communityId == null) continue;
            communityEventMap.computeIfAbsent(communityId, k -> new ArrayList<>()).add(event);
        }

        List<CommunityRankVO> result = new ArrayList<>();
        for (GridInfo grid : allGrids) {
            if (grid.getGridLevel() != null && grid.getGridLevel() == 2) {
                CommunityRankVO vo = new CommunityRankVO();
                vo.setGridId(grid.getId());
                vo.setGridName(grid.getGridName());

                List<EventInfo> communityEvents = communityEventMap.getOrDefault(grid.getId(), new ArrayList<>());
                vo.setTotalCount((long) communityEvents.size());
                long completedCount = communityEvents.stream()
                        .filter(e -> EventStatus.COMPLETED.getCode().equals(e.getStatus()))
                        .count();
                vo.setCompletedCount(completedCount);
                vo.setCompletionRate(vo.getTotalCount() > 0 ?
                        BigDecimal.valueOf((completedCount * 100.0) / vo.getTotalCount())
                                .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
                result.add(vo);
            }
        }

        result.sort(Comparator.comparingDouble(CommunityRankVO::getCompletionRate).reversed());
        return result;
    }

    private Long findAncestorCommunity(Long gridId, Map<Long, Long> gridParentMap, Map<Long, Long> gridToCommunity) {
        Long current = gridId;
        Set<Long> visited = new HashSet<>();
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (gridToCommunity.containsKey(current)) {
                return gridToCommunity.get(current);
            }
            current = gridParentMap.get(current);
        }
        return gridId;
    }

    @Override
    public List<GridMemberStatusVO> getGridMemberStatus() {
        List<GridMemberLocation> members = gridMemberLocationMapper.selectList(null);
        Map<Long, String> gridNameMap = new HashMap<>();
        List<GridInfo> grids = gridInfoMapper.selectList(null);
        for (GridInfo grid : grids) {
            gridNameMap.put(grid.getId(), grid.getGridName());
        }

        return members.stream().map(m -> {
            GridMemberStatusVO vo = new GridMemberStatusVO();
            vo.setUserId(m.getUserId());
            vo.setUserName(m.getUserName());
            vo.setPhone(m.getPhone());
            vo.setGridId(m.getGridId());
            vo.setGridName(gridNameMap.getOrDefault(m.getGridId(), "未知网格"));
            vo.setLng(m.getLng());
            vo.setLat(m.getLat());
            vo.setOnDuty(m.getOnDuty());
            vo.setBattery(m.getBattery());
            if (m.getLastReportTime() != null) {
                vo.setLastReportTime(m.getLastReportTime().format(DATETIME_FORMATTER));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public EventMarkerVO getEventDetail(Long eventId) {
        EventInfo event = eventInfoMapper.selectById(eventId);
        if (event == null) return null;
        return toEventMarkerVO(event);
    }

    @Override
    public Map<String, Object> getDashboardAllData() {
        Map<String, Object> data = new HashMap<>();
        data.put("overview", getDashboardOverview());
        data.put("eventMarkers", getPendingEventMarkers());
        data.put("heatmap", getEventHeatmap());
        data.put("communityRank", getCommunityRank());
        data.put("memberStatus", getGridMemberStatus());
        return data;
    }

    private EventMarkerVO toEventMarkerVO(EventInfo event) {
        EventMarkerVO vo = new EventMarkerVO();
        vo.setEventId(event.getId());
        vo.setEventNo(event.getEventNo());
        vo.setTitle(event.getTitle());
        vo.setEventType(event.getEventType());
        vo.setStatus(event.getStatus());
        vo.setPriority(event.getPriority());
        vo.setLng(event.getLng());
        vo.setLat(event.getLat());
        vo.setAddress(event.getAddress());
        vo.setReporterName(event.getReporterName());
        if (event.getCreatedAt() != null) {
            vo.setReportTime(event.getCreatedAt().format(DATETIME_FORMATTER));
        }
        return vo;
    }

    private List<DashboardOverviewVO.HourlyTrendItem> buildHourlyTrend(LocalDateTime todayStart) {
        List<DashboardOverviewVO.HourlyTrendItem> items = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        for (int h = 0; h <= currentHour; h++) {
            LocalDateTime hourStart = todayStart.plusHours(h);
            LocalDateTime hourEnd = hourStart.plusHours(1);

            LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(EventInfo::getCreatedAt, hourStart).lt(EventInfo::getCreatedAt, hourEnd);
            Long count = eventInfoMapper.selectCount(wrapper);

            DashboardOverviewVO.HourlyTrendItem item = new DashboardOverviewVO.HourlyTrendItem();
            item.setHour(h);
            item.setCount(count);
            items.add(item);
        }
        return items;
    }

    private Double calculateAvgHandleTime() {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getStatus, EventStatus.COMPLETED.getCode());
        List<EventInfo> completedEvents = eventInfoMapper.selectList(wrapper);

        if (completedEvents.isEmpty()) return 0.0;

        double totalHours = 0.0;
        int count = 0;
        for (EventInfo event : completedEvents) {
            if (event.getCreatedAt() != null && event.getUpdatedAt() != null) {
                long seconds = java.time.Duration.between(event.getCreatedAt(), event.getUpdatedAt()).getSeconds();
                totalHours += seconds / 3600.0;
                count++;
            }
        }
        return count > 0 ? BigDecimal.valueOf(totalHours / count).setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0;
    }

    private Double calculateOnlineRate() {
        List<GridMemberLocation> allMembers = gridMemberLocationMapper.selectList(null);
        if (allMembers.isEmpty()) return 0.0;
        long onlineCount = allMembers.stream().filter(m -> m.getOnDuty() != null && m.getOnDuty() == 1).count();
        return BigDecimal.valueOf((onlineCount * 100.0) / allMembers.size())
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
