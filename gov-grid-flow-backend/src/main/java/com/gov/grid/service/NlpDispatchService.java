package com.gov.grid.service;

import com.gov.grid.vo.NlpDispatchResultVO;

import java.util.List;
import java.util.Map;

public interface NlpDispatchService {

    NlpDispatchResultVO classify(String title, String description, String eventType);

    NlpDispatchResultVO classifyWithEventId(Long eventId, String title, String description, String eventType);

    boolean autoDispatchIfPossible(Long eventId, NlpDispatchResultVO result);

    boolean adoptDispatch(Long eventId, Long dispatchRecordId, String actualDeptCode, String actualDeptName);

    boolean rejectDispatch(Long eventId, Long dispatchRecordId, String feedback);

    List<Map<String, Object>> getDispatchHistory(Long eventId);

    List<Map<String, Object>> getTrainingData(int limit);

    Map<String, Object> triggerModelTraining(List<Map<String, Object>> records, int epochs, int batchSize, double learningRate);

    boolean isNlpServiceAvailable();
}
