package com.gov.grid.service;

public interface SentimentAnalysisService {

    SentimentResult analyze(String text);

    SentimentResult analyzeWithScores(String text, Integer speedScore, Integer effectScore);

    class SentimentResult {
        private String label;
        private double score;
        private boolean isWarning;
        private String warningLevel;
        private Integer speedScore;
        private Integer effectScore;

        public SentimentResult() {}

        public SentimentResult(String label, double score) {
            this.label = label;
            this.score = score;
            this.speedScore = null;
            this.effectScore = null;
            this.isWarning = false;
            this.warningLevel = calculateWarningLevel(label, score, null, null);
        }

        public SentimentResult(String label, double score, Integer speedScore, Integer effectScore) {
            this.label = label;
            this.score = score;
            this.speedScore = speedScore;
            this.effectScore = effectScore;
            this.isWarning = false;
            this.warningLevel = calculateWarningLevel(label, score, speedScore, effectScore);
        }

        private String calculateWarningLevel(String label, double score, Integer speed, Integer effect) {
            boolean negativeSentiment = "negative".equals(label) || score < 0.35;
            boolean lowSpeed = speed != null && speed <= 2;
            boolean lowEffect = effect != null && effect <= 2;
            boolean veryLowSpeed = speed != null && speed == 1;
            boolean veryLowEffect = effect != null && effect == 1;

            int strikeCount = 0;
            if (negativeSentiment) strikeCount++;
            if (lowSpeed) strikeCount++;
            if (lowEffect) strikeCount++;

            if (negativeSentiment && (veryLowSpeed || veryLowEffect)) {
                return "critical";
            }
            if (strikeCount >= 3) {
                return "critical";
            }
            if (strikeCount >= 2) {
                return "warning";
            }
            if (negativeSentiment || (lowSpeed && lowEffect)) {
                return "attention";
            }
            if (score >= 0.7 && !lowSpeed && !lowEffect) {
                return "excellent";
            }
            if (score >= 0.5) {
                return "normal";
            }
            return "attention";
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public double getScore() { return score; }
        public void setScore(double score) {
            this.score = score;
            this.warningLevel = calculateWarningLevel(label, score, speedScore, effectScore);
            this.isWarning = "warning".equals(this.warningLevel) || "critical".equals(this.warningLevel);
        }
        public boolean isWarning() { return isWarning; }
        public String getWarningLevel() { return warningLevel; }
        public Integer getSpeedScore() { return speedScore; }
        public void setSpeedScore(Integer speedScore) { this.speedScore = speedScore; }
        public Integer getEffectScore() { return effectScore; }
        public void setEffectScore(Integer effectScore) { this.effectScore = effectScore; }
    }
}
