package com.gov.grid.service;

import com.gov.grid.vo.CommunityRankVO;
import com.gov.grid.vo.DashboardOverviewVO;
import com.gov.grid.vo.EventMarkerVO;
import com.gov.grid.vo.GridMemberStatusVO;
import com.gov.grid.vo.HeatmapPointVO;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    DashboardOverviewVO getDashboardOverview();

    List<EventMarkerVO> getPendingEventMarkers();

    List<HeatmapPointVO> getEventHeatmap();

    List<CommunityRankVO> getCommunityRank();

    List<GridMemberStatusVO> getGridMemberStatus();

    EventMarkerVO getEventDetail(Long eventId);

    Map<String, Object> getDashboardAllData();
}
