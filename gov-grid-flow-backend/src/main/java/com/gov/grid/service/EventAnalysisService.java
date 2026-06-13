package com.gov.grid.service;

import com.gov.grid.vo.EventAnalysisReportVO;
import com.gov.grid.vo.EventGraphVO;
import com.gov.grid.vo.RecurrenceGroupVO;

import java.util.List;

public interface EventAnalysisService {

    boolean analyzeAndMarkRecurrence(Long eventId);

    int batchScanAndMarkHighRecurrence();

    List<RecurrenceGroupVO> listHighRecurrenceGroups(Integer days);

    RecurrenceGroupVO getRecurrenceGroup(String groupKey);

    EventGraphVO getEventRelationGraph(Long eventId, Integer depth);

    EventAnalysisReportVO generateAnalysisReport(Integer days);

    boolean pushReportToStreetOffice(EventAnalysisReportVO report);
}
