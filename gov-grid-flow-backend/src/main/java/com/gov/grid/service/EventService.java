package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.dto.BatchSyncResponseDTO;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.vo.EventDetailVO;

import java.util.List;

public interface EventService {

    EventInfo reportEvent(EventReportDTO dto, Long userId);

    EventInfo reportEvent(EventReportDTO dto, Long userId, boolean async);

    EventInfo findByClientId(String clientId);

    BatchSyncResponseDTO processBatch(List<EventReportDTO> events, Long userId);

    BatchSyncResponseDTO processBatchAsync(List<EventReportDTO> events, Long userId, String deviceId);

    PageResult<EventInfo> getEventList(EventQueryDTO dto);

    EventDetailVO getEventDetail(Long eventId);

    void updateEventStatus(Long eventId, String status);

    String generateEventNo();
}
