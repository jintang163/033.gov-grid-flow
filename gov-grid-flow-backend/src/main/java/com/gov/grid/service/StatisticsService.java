package com.gov.grid.service;

import com.gov.grid.vo.DeptStatsVO;
import com.gov.grid.vo.EventTrendVO;
import com.gov.grid.vo.EventTypeStatsVO;
import com.gov.grid.vo.GridStatsVO;
import com.gov.grid.vo.StatisticsVO;

import java.util.List;

public interface StatisticsService {

    StatisticsVO getOverviewStats();

    List<EventTrendVO> getEventTrend(Integer days);

    List<EventTypeStatsVO> getEventTypeStats();

    List<DeptStatsVO> getDeptStats();

    List<GridStatsVO> getGridStats();
}
