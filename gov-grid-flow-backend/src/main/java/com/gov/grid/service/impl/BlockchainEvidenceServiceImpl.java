package com.gov.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.EventBlockchainEvidence;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.mapper.EventBlockchainEvidenceMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.service.BlockchainEvidenceService;
import com.gov.grid.vo.BlockchainEvidenceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainEvidenceServiceImpl implements BlockchainEvidenceService {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.access.prefix:/file}")
    private String fileAccessPrefix;

    @Value("${blockchain.mock:true}")
    private boolean mockMode;

    @Value("${blockchain.chain-name:司法联盟链}")
    private String chainName;

    private final EventBlockchainEvidenceMapper evidenceMapper;
    private final EventInfoMapper eventInfoMapper;

    private static final Set<String> HIGH_RISK_TYPES = new HashSet<>(Arrays.asList(
            "security", "dispute", "safety_hazard", "public_security"
    ));

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlockchainEvidenceVO createEvidence(Long eventId) {
        EventInfo event = eventInfoMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException("事件不存在");
        }

        LambdaQueryWrapper<EventBlockchainEvidence> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(EventBlockchainEvidence::getEventId, eventId)
                .eq(EventBlockchainEvidence::getStatus, "SUCCESS")
                .last("LIMIT 1");
        EventBlockchainEvidence existing = evidenceMapper.selectOne(existingWrapper);
        if (existing != null) {
            log.info("事件 {} 已有存证记录，直接返回", eventId);
            return convertToVO(existing, event);
        }

        EventBlockchainEvidence evidence = new EventBlockchainEvidence();
        evidence.setEventId(eventId);
        evidence.setEvidenceNo(generateEvidenceNo());
        evidence.setChainType(chainName);
        evidence.setStatus("PENDING");

        StringBuilder hashBuilder = new StringBuilder();

        String titleHash = DigestUtil.sha256Hex(event.getTitle() == null ? "" : event.getTitle());
        evidence.setTitleHash(titleHash);
        hashBuilder.append("title:").append(titleHash).append(";");

        String descHash = DigestUtil.sha256Hex(event.getDescription() == null ? "" : event.getDescription());
        evidence.setDescHash(descHash);
        hashBuilder.append("desc:").append(descHash).append(";");

        String gpsContent = (event.getLng() != null ? event.getLng().toPlainString() : "")
                + "|" + (event.getLat() != null ? event.getLat().toPlainString() : "")
                + "|" + (event.getAddress() == null ? "" : event.getAddress());
        String gpsHash = DigestUtil.sha256Hex(gpsContent);
        evidence.setGpsHash(gpsHash);
        hashBuilder.append("gps:").append(gpsHash).append(";");

        List<String> imageHashList = new ArrayList<>();
        if (StrUtil.isNotBlank(event.getImages())) {
            String[] images = event.getImages().split(",");
            for (String img : images) {
                String imgHash = calculateFileHash(img.trim());
                imageHashList.add(imgHash);
                hashBuilder.append("img:").append(imgHash).append(";");
            }
            evidence.setImageCount(images.length);
            evidence.setImageHashes(String.join(",", imageHashList));
        } else {
            evidence.setImageCount(0);
        }

        List<String> videoHashList = new ArrayList<>();
        if (StrUtil.isNotBlank(event.getVideos())) {
            String[] videos = event.getVideos().split(",");
            for (String video : videos) {
                String videoHash = calculateFileHash(video.trim());
                videoHashList.add(videoHash);
                hashBuilder.append("video:").append(videoHash).append(";");
            }
            evidence.setVideoCount(videos.length);
            evidence.setVideoHashes(String.join(",", videoHashList));
        } else {
            evidence.setVideoCount(0);
        }

        if (StrUtil.isNotBlank(event.getVoiceUrl())) {
            String voiceHash = calculateFileHash(event.getVoiceUrl());
            evidence.setVoiceHash(voiceHash);
            hashBuilder.append("voice:").append(voiceHash).append(";");
        }

        String reporterInfo = event.getReporterId() != null
                ? "reporter:" + event.getReporterId()
                : "reporter:" + (event.getReporterName() == null ? "anonymous" : event.getReporterName());
        evidence.setReporterInfo(reporterInfo);
        hashBuilder.append(reporterInfo).append(";");

        hashBuilder.append("time:").append(event.getCreatedAt() != null ? event.getCreatedAt().toString() : LocalDateTime.now().toString());

        String evidenceHash = DigestUtil.sha256Hex(hashBuilder.toString());
        evidence.setEvidenceHash(evidenceHash);

        if (mockMode) {
            mockChainUpload(evidence);
        } else {
            evidence.setStatus("PENDING");
        }

        evidenceMapper.insert(evidence);
        log.info("创建区块链存证成功，事件ID：{}，存证编号：{}，证据哈希：{}",
                eventId, evidence.getEvidenceNo(), evidenceHash);

        return convertToVO(evidence, event);
    }

    @Override
    public BlockchainEvidenceVO getEvidenceByEventId(Long eventId) {
        LambdaQueryWrapper<EventBlockchainEvidence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventBlockchainEvidence::getEventId, eventId)
                .eq(EventBlockchainEvidence::getStatus, "SUCCESS")
                .orderByDesc(EventBlockchainEvidence::getCreatedAt)
                .last("LIMIT 1");
        EventBlockchainEvidence evidence = evidenceMapper.selectOne(wrapper);
        if (evidence == null) {
            return null;
        }
        EventInfo event = eventInfoMapper.selectById(eventId);
        return convertToVO(evidence, event);
    }

    @Override
    public List<BlockchainEvidenceVO> getEvidenceList(Long eventId) {
        LambdaQueryWrapper<EventBlockchainEvidence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventBlockchainEvidence::getEventId, eventId)
                .orderByDesc(EventBlockchainEvidence::getCreatedAt);
        List<EventBlockchainEvidence> list = evidenceMapper.selectList(wrapper);
        EventInfo event = eventInfoMapper.selectById(eventId);
        return list.stream()
                .map(e -> convertToVO(e, event))
                .collect(Collectors.toList());
    }

    @Override
    public BlockchainEvidenceVO getEvidenceById(Long evidenceId) {
        EventBlockchainEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BusinessException("存证记录不存在");
        }
        EventInfo event = eventInfoMapper.selectById(evidence.getEventId());
        return convertToVO(evidence, event);
    }

    @Override
    public PageResult<BlockchainEvidenceVO> getEvidencePage(int pageNum, int pageSize, String evidenceNo,
                                                             String keyword, String status, Integer verified,
                                                             String startTime, String endTime) {
        Page<EventBlockchainEvidence> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EventBlockchainEvidence> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(evidenceNo)) {
            wrapper.like(EventBlockchainEvidence::getEvidenceNo, evidenceNo);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(EventBlockchainEvidence::getStatus, status);
        }
        if (verified != null) {
            wrapper.eq(EventBlockchainEvidence::getVerified, verified);
        }
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(EventBlockchainEvidence::getCreatedAt, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(EventBlockchainEvidence::getCreatedAt, endTime + " 23:59:59");
        }

        wrapper.orderByDesc(EventBlockchainEvidence::getCreatedAt);

        Page<EventBlockchainEvidence> resultPage = evidenceMapper.selectPage(page, wrapper);

        List<Long> eventIds = resultPage.getRecords().stream()
                .map(EventBlockchainEvidence::getEventId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, EventInfo> eventMap = new HashMap<>();
        if (CollUtil.isNotEmpty(eventIds)) {
            List<EventInfo> events = eventInfoMapper.selectBatchIds(eventIds);
            for (EventInfo event : events) {
                eventMap.put(event.getId(), event);
            }
        }

        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matchedEventIds = eventMap.values().stream()
                    .filter(e -> (e.getTitle() != null && e.getTitle().contains(keyword))
                            || (e.getEventNo() != null && e.getEventNo().contains(keyword)))
                    .map(EventInfo::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(matchedEventIds)) {
                resultPage.setRecords(new ArrayList<>());
                resultPage.setTotal(0);
            } else {
                wrapper.in(EventBlockchainEvidence::getEventId, matchedEventIds);
                resultPage = evidenceMapper.selectPage(page, wrapper);
            }
        }

        List<BlockchainEvidenceVO> voList = resultPage.getRecords().stream()
                .map(e -> convertToVO(e, eventMap.get(e.getEventId())))
                .collect(Collectors.toList());

        return PageResult.of(voList, resultPage.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> verifyEvidence(Long evidenceId) {
        EventBlockchainEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BusinessException("存证记录不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evidenceId", evidenceId);
        result.put("evidenceNo", evidence.getEvidenceNo());
        result.put("chainType", evidence.getChainType());
        result.put("txHash", evidence.getTxHash());
        result.put("blockHeight", evidence.getBlockHeight());
        result.put("blockTime", evidence.getBlockTime());

        boolean isValid = false;
        if (mockMode) {
            isValid = "SUCCESS".equals(evidence.getStatus())
                    && StrUtil.isNotBlank(evidence.getEvidenceHash())
                    && StrUtil.isNotBlank(evidence.getTxHash());
        } else {
            isValid = verifyOnChain(evidence);
        }

        result.put("valid", isValid);
        result.put("verifiedAt", LocalDateTime.now());

        if (isValid) {
            evidence.setVerified(1);
            evidence.setVerifyTime(LocalDateTime.now());
            evidenceMapper.updateById(evidence);
        }

        log.info("区块链存证核验，证据ID：{}，结果：{}", evidenceId, isValid);
        return result;
    }

    @Override
    public String generateCertificateQrCode(Long evidenceId) {
        EventBlockchainEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) {
            throw new BusinessException("存证记录不存在");
        }

        String certUrl = "/api/blockchain/evidence/certificate/" + evidenceId;
        return certUrl;
    }

    @Override
    public boolean isHighRiskEventType(String eventType) {
        if (StrUtil.isBlank(eventType)) {
            return false;
        }
        return HIGH_RISK_TYPES.contains(eventType.toLowerCase());
    }

    private String calculateFileHash(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return DigestUtil.sha256Hex("");
        }

        try {
            if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                try (InputStream is = new URL(fileUrl).openStream()) {
                    return DigestUtil.sha256Hex(is);
                }
            } else {
                String filePath = uploadPath + File.separator + fileUrl.replace(fileAccessPrefix, "");
                File file = new File(filePath);
                if (file.exists()) {
                    return DigestUtil.sha256Hex(file);
                }
                return DigestUtil.sha256Hex(fileUrl);
            }
        } catch (Exception e) {
            log.warn("计算文件哈希失败：{}，使用URL哈希替代", fileUrl, e);
            return DigestUtil.sha256Hex(fileUrl + System.currentTimeMillis());
        }
    }

    private void mockChainUpload(EventBlockchainEvidence evidence) {
        String txHash = "0x" + IdUtil.simpleUUID() + IdUtil.simpleUUID().substring(0, 24);
        evidence.setTxHash(txHash.substring(0, 66));
        evidence.setBlockHeight(1000000L + new Random().nextInt(999999));
        evidence.setBlockTime(LocalDateTime.now());
        evidence.setStatus("SUCCESS");
        evidence.setVerified(1);
        evidence.setVerifyTime(LocalDateTime.now());
    }

    private boolean verifyOnChain(EventBlockchainEvidence evidence) {
        return false;
    }

    private String generateEvidenceNo() {
        return "BCE" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + IdUtil.randomNumbers(6);
    }

    private BlockchainEvidenceVO convertToVO(EventBlockchainEvidence evidence, EventInfo event) {
        BlockchainEvidenceVO vo = new BlockchainEvidenceVO();
        vo.setId(evidence.getId());
        vo.setEventId(evidence.getEventId());
        vo.setEvidenceNo(evidence.getEvidenceNo());
        vo.setChainType(evidence.getChainType());
        vo.setTxHash(evidence.getTxHash());
        vo.setBlockHeight(evidence.getBlockHeight());
        vo.setBlockTime(evidence.getBlockTime());
        vo.setEvidenceHash(evidence.getEvidenceHash());
        vo.setImageCount(evidence.getImageCount());
        vo.setVideoCount(evidence.getVideoCount());
        vo.setVoiceHash(evidence.getVoiceHash());
        vo.setGpsHash(evidence.getGpsHash());
        vo.setTitleHash(evidence.getTitleHash());
        vo.setDescHash(evidence.getDescHash());
        vo.setStatus(evidence.getStatus());
        vo.setCertificateUrl(evidence.getCertificateUrl());
        vo.setVerified(evidence.getVerified());
        vo.setVerifyTime(evidence.getVerifyTime());
        vo.setRemark(evidence.getRemark());
        vo.setCreatedAt(evidence.getCreatedAt());

        if (StrUtil.isNotBlank(evidence.getImageHashes())) {
            vo.setImageHashes(Arrays.asList(evidence.getImageHashes().split(",")));
        }
        if (StrUtil.isNotBlank(evidence.getVideoHashes())) {
            vo.setVideoHashes(Arrays.asList(evidence.getVideoHashes().split(",")));
        }

        if (event != null) {
            vo.setLng(event.getLng());
            vo.setLat(event.getLat());
            vo.setAddress(event.getAddress());
            vo.setTitle(event.getTitle());
            vo.setReporterName(event.getReporterName());
        }

        return vo;
    }
}
