package com.gov.grid.service;

import com.gov.grid.dto.EventProcessDTO;
import com.gov.grid.entity.EventProcess;

import java.util.List;

public interface EventProcessService {

    void processEvent(EventProcessDTO dto, Long userId);

    void assignTask(Long eventId, String taskId, String assigneeId, Long userId);

    List<EventProcess> getProcessHistory(Long eventId);

    String getProcessDiagram(Long eventId);
}
