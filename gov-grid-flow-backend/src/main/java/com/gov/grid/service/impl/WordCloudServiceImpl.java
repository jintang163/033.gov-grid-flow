package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gov.grid.service.WordCloudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WordCloudServiceImpl implements WordCloudService {

    private static final Logger log = LoggerFactory.getLogger(WordCloudServiceImpl.class);

    private Set<String> stopWords;
    private Set<String> domainWords;

    @PostConstruct
    public void init() {
        initStopWords();
        initDomainWords();
        log.info("词云服务初始化完成，停用词{}个，领域词{}个", stopWords.size(), domainWords.size());
    }

    private void initStopWords() {
        stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "是", "在", "我", "有", "和", "就",
                "不", "人", "都", "一", "一个", "上", "也", "很",
                "到", "说", "要", "去", "你", "会", "着", "没有",
                "看", "好", "自己", "这", "那", "他", "她", "它",
                "们", "什么", "怎么", "为什么", "吗", "呢", "吧",
                "啊", "哦", "嗯", "哈", "呀", "哇", "哦", "哎",
                "被", "把", "给", "让", "从", "向", "对", "为",
                "以", "及", "其", "之", "所", "而", "然", "则",
                "可以", "可能", "应该", "必须", "需要", "能够",
                "但是", "可是", "不过", "所以", "因此", "因为",
                "如果", "假如", "虽然", "尽管", "不仅", "而且",
                "还", "又", "再", "也", "都", "全", "只", "就",
                "这个", "那个", "这些", "那些", "这样", "那样",
                "真的", "非常", "特别", "十分", "相当", "比较",
                "有点", "稍微", "几乎", "差不多", "大概", "大约",
                "现在", "已经", "以后", "之前", "后来", "然后",
                "一直", "从来", "总是", "经常", "偶尔", "有时候",
                "还是", "或者", "要么", "不是", "就是", "只是",
                "知道", "觉得", "感觉", "认为", "以为", "想",
                "做", "干", "搞", "弄", "办", "处理", "解决",
                "问题", "事情", "东西", "情况", "方式", "方法",
                "时候", "时间", "地方", "地点", "位置", "地方",
                "一下", "一些", "一点", "不少", "很多", "许多",
                "太", "最", "更", "越", "比", "跟", "和", "与",
                "能", "会", "可", "该", "得", "过", "来", "去",
                "这里", "那里", "哪儿", "哪里", "谁", "哪个", "什么",
                "怎么样", "如何", "为何", "多少", "几", "多"
        ));
    }

    private void initDomainWords() {
        domainWords = new HashSet<>(Arrays.asList(
                "噪音", "噪声", "扰民", "油烟", "垃圾", "污水", "废气",
                "占道", "违停", "乱停", "乱摆", "乱搭", "违建", "违章",
                "卫生", "环境", "保洁", "清洁", "脏乱差", "整治",
                "路灯", "井盖", "管道", "水管", "电线", "电缆", "设施",
                "绿化", "树木", "花草", "修剪", "养护",
                "物业", "小区", "社区", "街道", "网格", "网格员",
                "投诉", "举报", "反映", "反馈", "建议", "意见",
                "速度", "效率", "态度", "服务", "质量", "效果", "结果",
                "满意", "不满意", "好评", "差评",
                "及时", "拖拉", "拖延", "推诿", "扯皮",
                "负责", "敷衍", "专业", "认真", "耐心", "细心",
                "整改", "改善", "改进", "修复", "维修", "维护",
                "安全", "隐患", "危险", "事故",
                "收费", "费用", "价格", "贵", "便宜",
                "道路", "路面", "坑洼", "破损", "塌陷",
                "消防", "防火", "逃生",
                "噪音扰民", "环境污染", "市容市貌", "市政设施",
                "公共设施", "居住环境", "物业服务", "社区治理",
                "网格员", "网格长", "街道办", "居委会", "城管",
                "执法", "管理", "监管", "监督",
                "五星", "一星", "评分", "打分",
                "点赞", "表扬", "感谢", "辛苦了",
                "投诉无门", "踢皮球", "形式主义", "走过场"
        ));
    }

    @Override
    public List<Map.Entry<String, Integer>> extractKeywords(List<String> texts, int topN) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String text : texts) {
            if (StrUtil.isBlank(text)) continue;

            List<String> words = segment(text);
            for (String word : words) {
                if (isValidWord(word)) {
                    frequencyMap.merge(word, 1, Integer::sum);
                }
            }
        }

        return frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<String, Integer>> extractKeywords(String text, int topN) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        return extractKeywords(Collections.singletonList(text), topN);
    }

    @Override
    public List<String> segment(String text) {
        if (StrUtil.isBlank(text)) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();

        String cleanText = text.replaceAll("[，。！？、；：,.!?;:\"'()（）【】\\[\\]\\s+]", " ");
        cleanText = cleanText.toLowerCase();

        Set<String> foundDomainWords = new TreeSet<>((a, b) -> b.length() - a.length());
        for (String dw : domainWords) {
            if (cleanText.contains(dw)) {
                foundDomainWords.add(dw);
            }
        }

        for (String dw : foundDomainWords) {
            int count = countOccurrences(cleanText, dw);
            for (int i = 0; i < count; i++) {
                result.add(dw);
            }
            cleanText = cleanText.replace(dw, " ");
        }

        String[] parts = cleanText.split("\\s+");
        for (String part : parts) {
            if (part.isEmpty()) continue;

            if (part.length() >= 2) {
                for (int len = 4; len >= 2; len--) {
                    for (int i = 0; i <= part.length() - len; i++) {
                        String sub = part.substring(i, i + len);
                        if (isValidWord(sub)) {
                            result.add(sub);
                        }
                    }
                }
            }

            if (part.length() == 1 && isValidSingleChar(part)) {
                result.add(part);
            }
        }

        return result;
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

    private boolean isValidWord(String word) {
        if (StrUtil.isBlank(word)) return false;
        if (word.length() < 2) return false;
        if (stopWords.contains(word)) return false;
        if (word.matches("\\d+")) return false;
        if (word.matches("[a-zA-Z]+") && word.length() < 3) return false;
        return true;
    }

    private boolean isValidSingleChar(String c) {
        if (c == null || c.length() != 1) return false;
        char ch = c.charAt(0);
        return ch >= '\u4e00' && ch <= '\u9fa5' && !stopWords.contains(c);
    }
}
