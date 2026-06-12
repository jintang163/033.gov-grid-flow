package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.ImageComparisonDTO;
import com.gov.grid.dto.ImageComparisonResultVO;
import com.gov.grid.entity.EventImageComparison;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.enums.ComparisonJudgment;
import com.gov.grid.mapper.EventImageComparisonMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.service.ImageComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cv.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockImageComparisonServiceImpl implements ImageComparisonService {

    private static final double SIMILARITY_THRESHOLD = 60.0;

    private final EventImageComparisonMapper comparisonMapper;
    private final EventInfoMapper eventInfoMapper;
    private final com.gov.grid.service.FileService fileService;

    @Override
    public ImageComparisonResultVO compareImages(ImageComparisonDTO dto) {
        log.info("Mock图像比对开始，beforeImage={}, afterImage={}", dto.getBeforeImage(), dto.getAfterImage());

        BigDecimal similarity = calculateMockSimilarity(dto.getBeforeImage(), dto.getAfterImage());
        ComparisonJudgment judgment = judgeBySimilarity(similarity);
        String heatmapImage = generateMockHeatmap(dto.getBeforeImage(), dto.getAfterImage());

        ImageComparisonResultVO vo = new ImageComparisonResultVO();
        vo.setEventId(dto.getEventId());
        vo.setProcessId(dto.getProcessId());
        vo.setBeforeImage(dto.getBeforeImage());
        vo.setAfterImage(dto.getAfterImage());
        vo.setSimilarity(similarity);
        vo.setHeatmapImage(heatmapImage);
        vo.setJudgment(judgment.getCode());
        vo.setJudgmentText(judgment.getName());
        vo.setJudgmentReason(generateJudgmentReason(judgment, similarity));
        vo.setCreatedAt(LocalDateTime.now());

        log.info("Mock图像比对完成，相似度={}，判定={}", similarity, judgment.getName());
        return vo;
    }

    @Override
    public ImageComparisonResultVO compareAndSave(ImageComparisonDTO dto, Long userId) {
        EventInfo eventInfo = eventInfoMapper.selectById(dto.getEventId());
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        ImageComparisonResultVO result = compareImages(dto);

        EventImageComparison comparison = new EventImageComparison();
        comparison.setEventId(dto.getEventId());
        comparison.setProcessId(dto.getProcessId());
        comparison.setBeforeImage(dto.getBeforeImage());
        comparison.setAfterImage(dto.getAfterImage());
        comparison.setSimilarity(result.getSimilarity());
        comparison.setHeatmapImage(result.getHeatmapImage());
        comparison.setJudgment(result.getJudgment());
        comparison.setJudgmentReason(result.getJudgmentReason());

        comparisonMapper.insert(comparison);
        result.setId(comparison.getId());

        return result;
    }

    @Override
    public List<ImageComparisonResultVO> getComparisonHistory(Long eventId) {
        LambdaQueryWrapper<EventImageComparison> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventImageComparison::getEventId, eventId)
                .orderByDesc(EventImageComparison::getCreatedAt);

        List<EventImageComparison> list = comparisonMapper.selectList(wrapper);
        List<ImageComparisonResultVO> voList = new ArrayList<>();
        for (EventImageComparison item : list) {
            ImageComparisonResultVO vo = new ImageComparisonResultVO();
            BeanUtils.copyProperties(item, vo);
            ComparisonJudgment judgment = ComparisonJudgment.fromCode(item.getJudgment());
            vo.setJudgmentText(judgment.getName());
            voList.add(vo);
        }
        return voList;
    }

    private BigDecimal calculateMockSimilarity(String beforeUrl, String afterUrl) {
        Random random = new Random();
        int hash = (beforeUrl + afterUrl).hashCode();
        random.setSeed(Math.abs(hash));

        double baseSimilarity = 30 + random.nextDouble() * 50;
        BigDecimal similarity = BigDecimal.valueOf(baseSimilarity)
                .setScale(2, RoundingMode.HALF_UP);

        return similarity;
    }

    private ComparisonJudgment judgeBySimilarity(BigDecimal similarity) {
        if (similarity.compareTo(BigDecimal.valueOf(SIMILARITY_THRESHOLD)) < 0) {
            return ComparisonJudgment.PASS;
        } else {
            return ComparisonJudgment.FAIL;
        }
    }

    private String generateJudgmentReason(ComparisonJudgment judgment, BigDecimal similarity) {
        if (judgment == ComparisonJudgment.PASS) {
            return String.format("处置前后图像相似度为%.1f%%，低于阈值%.0f%%，判定为处置合格。图像差异明显，问题已得到有效处理。",
                    similarity.doubleValue(), SIMILARITY_THRESHOLD);
        } else {
            return String.format("处置前后图像相似度为%.1f%%，高于阈值%.0f%%，判定为处置不合格。图像差异较小，问题可能未得到有效处理，建议重新处置。",
                    similarity.doubleValue(), SIMILARITY_THRESHOLD);
        }
    }

    private String generateMockHeatmap(String beforeUrl, String afterUrl) {
        try {
            int width = 400;
            int height = 300;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            Random random = new Random(Math.abs((beforeUrl + afterUrl).hashCode()));
            int numCircles = 8 + random.nextInt(12);

            for (int i = 0; i < numCircles; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                int radius = 20 + random.nextInt(60);

                float redness = random.nextFloat();
                Color color = new Color(1.0f, 1.0f - redness, 0.0f, 0.6f);

                g2d.setColor(color);
                for (int r = radius; r > 0; r -= 2) {
                    g2d.drawOval(x - r, y - r, r * 2, r * 2);
                }
            }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString("差异热力图 (Mock)", 10, 25);

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.error("生成Mock热力图失败", e);
            return null;
        }
    }
}
