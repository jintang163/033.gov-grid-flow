package com.gov.grid.quartz;

import com.gov.grid.service.NlpDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NlpModelTrainJob {

    private final NlpDispatchService nlpDispatchService;

    @Value("${nlp.dispatch.auto-train-enabled:false}")
    private boolean autoTrainEnabled;

    @Value("${nlp.dispatch.auto-train-cron:0 0 2 * * ?}")
    private String autoTrainCron;

    @Scheduled(cron = "${nlp.dispatch.auto-train-cron:0 0 2 * * ?}")
    public void scheduledModelTraining() {
        if (!autoTrainEnabled) {
            return;
        }

        log.info("[NlpModelTrainJob] Starting scheduled model training...");

        try {
            if (!nlpDispatchService.isNlpServiceAvailable()) {
                log.warn("[NlpModelTrainJob] NLP service is not available, skipping training");
                return;
            }

            List<Map<String, Object>> trainingData = nlpDispatchService.getTrainingData(5000);
            if (trainingData.isEmpty()) {
                log.info("[NlpModelTrainJob] No training data available, skipping");
                return;
            }

            if (trainingData.size() < 50) {
                log.info("[NlpModelTrainJob] Training data too few ({}), need at least 50 samples, skipping",
                        trainingData.size());
                return;
            }

            log.info("[NlpModelTrainJob] Training with {} samples", trainingData.size());
            Map<String, Object> result = nlpDispatchService.triggerModelTraining(
                    trainingData, 3, 16, 2e-5
            );
            log.info("[NlpModelTrainJob] Training completed: {}", result);
        } catch (Exception e) {
            log.error("[NlpModelTrainJob] Scheduled model training failed", e);
        }
    }
}
