package com.gov.grid.service;

public interface SentimentAnalysisService {

    SentimentResult analyze(String text);

    class SentimentResult {
        private String label;
        private double score;
        private boolean isWarning;
        private String warningLevel;

        public SentimentResult() {}

        public SentimentResult(String label, double score) {
            this.label = label;
            this.score = score;
            this.isWarning = score < 0.3;
            this.warningLevel = calculateWarningLevel(score);
        }

        private String calculateWarningLevel(double score) {
            if (score >= 0.7) return "normal";
            if (score >= 0.5) return "attention";
            if (score >= 0.3) return "warning";
            return "critical";
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public double getScore() { return score; }
        public void setScore(double score) {
            this.score = score;
            this.isWarning = score < 0.3;
            this.warningLevel = calculateWarningLevel(score);
        }
        public boolean isWarning() { return isWarning; }
        public String getWarningLevel() { return warningLevel; }
    }
}
