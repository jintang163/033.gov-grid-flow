package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gov.grid.service.SentimentAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class SentimentAnalysisServiceImpl implements SentimentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisServiceImpl.class);

    @Value("${nlp.service.url:}")
    private String nlpServiceUrl;

    @Value("${nlp.sentiment.enabled:true}")
    private boolean nlpEnabled;

    private Set<String> positiveWords;
    private Set<String> negativeWords;
    private Set<String> degreeWords;
    private Map<String, Double> degreeWeights;
    private Set<String> negativeAdverbs;

    @PostConstruct
    public void init() {
        initPositiveWords();
        initNegativeWords();
        initDegreeWords();
        initNegativeAdverbs();
        log.info("情感词典初始化完成，正向词{}个，负向词{}个", positiveWords.size(), negativeWords.size());
    }

    private void initPositiveWords() {
        positiveWords = new HashSet<>(Arrays.asList(
                "满意", "满意的", "很满意", "非常满意", "特别满意", "太满意了",
                "好", "很好", "非常好", "特别好", "真好", "挺好", "不错", "很不错", "挺好的",
                "赞", "点赞", "给力", "棒", "很棒", "非常棒", "优秀", "出色", "完美",
                "快", "很快", "非常快", "迅速", "及时", "高效", "效率高",
                "负责", "认真", "专业", "耐心", "细心", "周到", "热情", "积极", "主动",
                "解决", "处理好了", "处理得好", "办好了", "搞定",
                "感谢", "谢谢", "辛苦了", "辛苦", "表扬", "肯定", "认可",
                "到位", "规范", "标准", "满意而归", "交口称赞",
                "服务好", "态度好", "质量好", "效果好", "结果好",
                "便民", "贴心", "暖心", "安心", "放心",
                "五星好评", "五星", "好评", "推荐",
                "高兴", "开心", "欣慰", "惊喜",
                "改善", "提升", "进步", "变化大",
                "公开透明", "公正", "公平", "合理"
        ));
    }

    private void initNegativeWords() {
        negativeWords = new HashSet<>(Arrays.asList(
                "不满意", "不满", "失望", "非常失望", "太失望了",
                "差", "很差", "非常差", "特别差", "太差了", "糟糕", "糟透了",
                "不好", "不行", "垃圾", "烂", "废物", "没用", "差劲",
                "慢", "很慢", "非常慢", "太慢了", "拖拉", "拖延", "效率低", "低效",
                "不负责", "敷衍", "马虎", "不认真", "不专业", "不耐心",
                "推诿", "扯皮", "踢皮球", "不作为", "懒政", "怠政",
                "没解决", "没处理", "没办好", "搞不定", "办不了",
                "投诉", "举报", "上访", "曝光", "举报电话",
                "态度差", "态度恶劣", "语气差", "服务差",
                "等太久", "等了好久", "等不及", "遥遥无期",
                "不合理", "不公平", "不公正", "不公开", "不透明",
                "乱收费", "收费高", "贵", "太贵",
                "扰民", "噪音大", "脏", "乱", "差",
                "推卸责任", "不负责任", "不闻不问", "置之不理",
                "麻烦", "烦心", "闹心", "生气", "气愤", "愤怒",
                "差评", "一星", "一星差评",
                "反复", "重复", "屡教不改", "老样子", "没变化",
                "形式主义", "走过场", "表面功夫",
                "危险", "不安全", "隐患", "问题严重"
        ));
    }

    private void initDegreeWords() {
        degreeWeights = new HashMap<>();
        degreeWeights.put("非常", 2.0);
        degreeWeights.put("特别", 2.0);
        degreeWeights.put("极其", 2.2);
        degreeWeights.put("超级", 2.0);
        degreeWeights.put("极为", 2.2);
        degreeWeights.put("太", 1.8);
        degreeWeights.put("真的", 1.5);
        degreeWeights.put("真是", 1.5);
        degreeWeights.put("很", 1.5);
        degreeWeights.put("挺", 1.2);
        degreeWeights.put("比较", 1.2);
        degreeWeights.put("相当", 1.3);
        degreeWeights.put("有点", 0.8);
        degreeWeights.put("稍微", 0.7);
        degreeWeights.put("略", 0.7);
        degreeWeights.put("不太", 0.6);
        degreeWeights.put("不怎么", 0.5);
        degreeWeights.put("最", 2.0);
        degreeWeights.put("极", 2.2);
        degreeWeights.put("巨", 2.0);
        degreeWeights.put("超", 1.8);
        degreeWords = degreeWeights.keySet();
    }

    private void initNegativeAdverbs() {
        negativeAdverbs = new HashSet<>(Arrays.asList(
                "不", "没", "没有", "未", "别", "勿", "莫", "非", "无",
                "不是", "不会", "不能", "不该", "不可",
                "没有", "没了", "没人", "没事",
                "不怎么样", "不咋地", "不好说"
        ));
    }

    @Override
    public SentimentResult analyze(String text) {
        if (StrUtil.isBlank(text)) {
            return new SentimentResult("neutral", 0.5);
        }

        if (nlpEnabled && StrUtil.isNotBlank(nlpServiceUrl)) {
            try {
                SentimentResult result = callNlpService(text);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("调用NLP情感分析服务失败，降级使用词典法: {}", e.getMessage());
            }
        }

        return analyzeByDictionary(text);
    }

    private SentimentResult callNlpService(String text) {
        try {
            String url = nlpServiceUrl + "/api/sentiment/analyze";
            JSONObject body = JSONUtil.createObj();
            body.set("text", text);

            HttpResponse response = HttpRequest.post(url)
                    .body(body.toString())
                    .timeout(5000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                String label = result.getStr("label", "neutral");
                double score = result.getDouble("score", 0.5);
                return new SentimentResult(label, score);
            }
        } catch (Exception e) {
            log.debug("NLP服务调用异常: {}", e.getMessage());
        }
        return null;
    }

    private SentimentResult analyzeByDictionary(String text) {
        double positiveScore = 0;
        double negativeScore = 0;

        String processedText = text.toLowerCase();
        processedText = processedText.replaceAll("[，。！？、；：,.!?;:\"]", " ");
        processedText = processedText.replaceAll("\\s+", " ");

        int textLen = processedText.length();

        for (String word : positiveWords) {
            int count = countOccurrences(processedText, word);
            if (count > 0) {
                double weight = calculateDegreeWeight(processedText, word);
                double negationFactor = calculateNegationFactor(processedText, word);
                positiveScore += count * weight * negationFactor;
            }
        }

        for (String word : negativeWords) {
            int count = countOccurrences(processedText, word);
            if (count > 0) {
                double weight = calculateDegreeWeight(processedText, word);
                double negationFactor = calculateNegationFactor(processedText, word);
                negativeScore += count * weight * negationFactor;
            }
        }

        double total = positiveScore + negativeScore;
        double sentimentScore;
        String label;

        if (total == 0) {
            sentimentScore = 0.5;
            label = "neutral";
        } else {
            sentimentScore = positiveScore / total;
            sentimentScore = Math.max(0.0, Math.min(1.0, sentimentScore));

            if (sentimentScore >= 0.65) {
                label = "positive";
            } else if (sentimentScore <= 0.35) {
                label = "negative";
            } else {
                label = "neutral";
            }
        }

        double intensityFactor = Math.min(total / 3.0, 1.0);
        if (label.equals("positive")) {
            sentimentScore = 0.5 + (sentimentScore - 0.5) * (0.5 + intensityFactor * 0.5);
        } else if (label.equals("negative")) {
            sentimentScore = 0.5 - (0.5 - sentimentScore) * (0.5 + intensityFactor * 0.5);
        }

        sentimentScore = Math.max(0.0, Math.min(1.0, sentimentScore));
        return new SentimentResult(label, sentimentScore);
    }

    private int countOccurrences(String text, String word) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(word, idx)) != -1) {
            count++;
            idx += word.length();
        }
        return count;
    }

    private double calculateDegreeWeight(String text, String word) {
        double weight = 1.0;
        int wordIndex = text.indexOf(word);
        if (wordIndex <= 0) return weight;

        String before = text.substring(Math.max(0, wordIndex - 6), wordIndex);

        for (Map.Entry<String, Double> entry : degreeWeights.entrySet()) {
            if (before.contains(entry.getKey())) {
                weight *= entry.getValue();
                break;
            }
        }
        return weight;
    }

    private double calculateNegationFactor(String text, String word) {
        int wordIndex = text.indexOf(word);
        if (wordIndex <= 0) return 1.0;

        String before = text.substring(Math.max(0, wordIndex - 6), wordIndex);

        int negationCount = 0;
        for (String neg : negativeAdverbs) {
            if (before.contains(neg)) {
                negationCount++;
            }
        }

        if (negationCount % 2 == 1) {
            return 0.2;
        }
        return 1.0;
    }
}
