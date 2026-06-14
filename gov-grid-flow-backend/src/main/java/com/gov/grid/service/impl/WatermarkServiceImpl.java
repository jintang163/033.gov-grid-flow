package com.gov.grid.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.DigitalEnvelopeDTO;
import com.gov.grid.dto.WatermarkDTO;
import com.gov.grid.dto.WatermarkInfo;
import com.gov.grid.entity.EncryptionKey;
import com.gov.grid.entity.FileWatermark;
import com.gov.grid.mapper.EncryptionKeyMapper;
import com.gov.grid.mapper.FileWatermarkMapper;
import com.gov.grid.service.EncryptionService;
import com.gov.grid.service.FileService;
import com.gov.grid.service.WatermarkService;
import com.gov.grid.vo.TamperCheckVO;
import com.gov.grid.vo.WatermarkResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkServiceImpl implements WatermarkService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "bmp");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "avi", "mov");
    private static final float IMAGE_COMPRESSION_QUALITY = 0.8f;

    private final FileWatermarkMapper fileWatermarkMapper;
    private final EncryptionKeyMapper encryptionKeyMapper;
    private final EncryptionService encryptionService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.access.prefix:/file}")
    private String accessPrefix;

    @Value("${watermark.font.size:14}")
    private int fontSize;

    @Value("${watermark.font.color:#FF0000}")
    private String fontColor;

    @Value("${watermark.opacity:0.3}")
    private float opacity;

    private Font watermarkFont;

    @PostConstruct
    public void init() {
        watermarkFont = new Font("SimHei", Font.PLAIN, fontSize);
    }

    @Override
    public WatermarkResultVO addWatermark(WatermarkDTO watermarkDTO) throws IOException {
        return uploadWithWatermark(
                watermarkDTO.getFile(),
                watermarkDTO.getReportTime(),
                watermarkDTO.getReporterName(),
                watermarkDTO.getEventNo(),
                watermarkDTO.getEventId(),
                watermarkDTO.getReporterId(),
                watermarkDTO.getSensitive()
        );
    }

    @Override
    public WatermarkResultVO uploadWithWatermark(MultipartFile file, String reportTime,
                                                  String reporterName, String eventNo,
                                                  Long eventId, Long reporterId,
                                                  Boolean sensitive) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename).toLowerCase();

        byte[] originalBytes = file.getBytes();
        String originalMd5 = SecureUtil.md5(originalBytes);

        WatermarkInfo watermarkInfo = new WatermarkInfo(reportTime, reporterName, eventNo);
        String watermarkInfoJson = objectMapper.writeValueAsString(watermarkInfo);

        byte[] watermarkedBytes;
        String newFilename;

        if (isImageFile(extension)) {
            watermarkedBytes = addImageWatermark(originalBytes, extension, watermarkInfo);
            newFilename = IdUtil.fastSimpleUUID() + "." + extension;
        } else if (isVideoFile(extension)) {
            watermarkedBytes = originalBytes;
            newFilename = IdUtil.fastSimpleUUID() + "." + extension;
            log.warn("视频文件暂不支持加水印，仅进行MD5存证：{}", originalFilename);
        } else {
            throw new BusinessException("不支持的文件类型：" + extension);
        }

        String watermarkedMd5 = SecureUtil.md5(watermarkedBytes);

        if (Boolean.TRUE.equals(sensitive)) {
            try {
                DigitalEnvelopeDTO envelope = encryptionService.createDigitalEnvelope(watermarkedBytes, 3L);
                watermarkedBytes = objectMapper.writeValueAsBytes(envelope);
                newFilename = IdUtil.fastSimpleUUID() + ".enc";
            } catch (Exception e) {
                log.error("数字信封加密失败", e);
                throw new BusinessException("敏感文件加密失败：" + e.getMessage());
            }
        }

        File destFile = new File(uploadPath, newFilename);
        try (OutputStream os = new FileOutputStream(destFile)) {
            os.write(watermarkedBytes);
        }

        String fileUrl = accessPrefix + "/" + newFilename;

        FileWatermark fileWatermark = new FileWatermark();
        fileWatermark.setFileUrl(fileUrl);
        fileWatermark.setOriginalMd5(originalMd5);
        fileWatermark.setWatermarkedMd5(watermarkedMd5);
        fileWatermark.setWatermarkInfo(watermarkInfoJson);
        fileWatermark.setEventId(eventId);
        fileWatermark.setReporterId(reporterId);
        fileWatermark.setIsEncrypted(Boolean.TRUE.equals(sensitive) ? 1 : 0);
        fileWatermark.setTamperVerified(1);
        fileWatermark.setTamperVerifyTime(LocalDateTime.now());
        fileWatermarkMapper.insert(fileWatermark);

        WatermarkResultVO result = new WatermarkResultVO();
        result.setFileUrl(fileUrl);
        result.setOriginalMd5(originalMd5);
        result.setWatermarkedMd5(watermarkedMd5);
        result.setWatermarkInfo(watermarkInfoJson);
        result.setTampered(false);
        result.setTamperMessage("水印添加成功，MD5已存证");

        log.info("文件水印处理完成：{}，原始MD5：{}，水印MD5：{}", fileUrl, originalMd5, watermarkedMd5);
        return result;
    }

    @Override
    public TamperCheckVO checkTamper(String fileUrl) throws IOException {
        if (StrUtil.isBlank(fileUrl)) {
            throw new BusinessException("文件URL不能为空");
        }

        FileWatermark fileWatermark = fileWatermarkMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileWatermark>()
                        .eq(FileWatermark::getFileUrl, fileUrl)
        );

        if (fileWatermark == null) {
            throw new BusinessException("未找到该文件的水印存证记录");
        }

        String filePath = fileService.getFilePath(fileUrl.substring(fileUrl.lastIndexOf("/") + 1));
        byte[] currentBytes = FileUtil.readBytes(filePath);
        String currentMd5 = SecureUtil.md5(currentBytes);

        TamperCheckVO result = new TamperCheckVO();
        result.setFileWatermarkId(fileWatermark.getId());
        result.setFileUrl(fileUrl);
        result.setOriginalMd5(fileWatermark.getWatermarkedMd5());
        result.setCurrentMd5(currentMd5);
        result.setWatermarkInfo(fileWatermark.getWatermarkInfo());
        result.setVerifyTime(LocalDateTime.now());

        boolean tampered = !currentMd5.equals(fileWatermark.getWatermarkedMd5());
        result.setTampered(tampered);

        if (tampered) {
            result.setMessage("检测到文件被篡改！原始MD5与当前MD5不匹配");
            fileWatermark.setTamperVerified(2);
        } else {
            result.setMessage("文件完整性验证通过，未检测到篡改");
            fileWatermark.setTamperVerified(1);
        }
        fileWatermark.setTamperVerifyTime(LocalDateTime.now());
        fileWatermarkMapper.updateById(fileWatermark);

        log.info("文件篡改检测完成：{}，是否篡改：{}", fileUrl, tampered);
        return result;
    }

    @Override
    public List<TamperCheckVO> checkEventFilesTamper(Long eventId) throws IOException {
        if (eventId == null) {
            throw new BusinessException("事件ID不能为空");
        }

        List<FileWatermark> watermarkList = fileWatermarkMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileWatermark>()
                        .eq(FileWatermark::getEventId, eventId)
        );

        List<TamperCheckVO> results = new ArrayList<>();
        for (FileWatermark watermark : watermarkList) {
            try {
                TamperCheckVO checkVO = checkTamper(watermark.getFileUrl());
                results.add(checkVO);
            } catch (Exception e) {
                log.error("检测文件篡改失败：{}", watermark.getFileUrl(), e);
                TamperCheckVO errorVO = new TamperCheckVO();
                errorVO.setFileWatermarkId(watermark.getId());
                errorVO.setFileUrl(watermark.getFileUrl());
                errorVO.setTampered(null);
                errorVO.setMessage("检测失败：" + e.getMessage());
                errorVO.setVerifyTime(LocalDateTime.now());
                results.add(errorVO);
            }
        }
        return results;
    }

    private byte[] addImageWatermark(byte[] imageBytes, String extension, WatermarkInfo watermarkInfo) throws IOException {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new BusinessException("无法读取图片");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = watermarkedImage.createGraphics();

            g2d.drawImage(image, 0, 0, width, height, null);

            g2d.setFont(watermarkFont);
            g2d.setColor(Color.decode(fontColor));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            FontMetrics fm = g2d.getFontMetrics();
            String line1 = "上报时间：" + watermarkInfo.getReportTime();
            String line2 = "网格员：" + watermarkInfo.getReporterName();
            String line3 = StrUtil.isNotBlank(watermarkInfo.getEventNo()) ?
                    "事件编号：" + watermarkInfo.getEventNo() : "";

            int lineHeight = fm.getHeight();
            int margin = 20;
            int y = height - margin - (StrUtil.isNotBlank(line3) ? 2 : 1) * lineHeight;

            g2d.drawString(line1, margin, y);
            g2d.drawString(line2, margin, y + lineHeight);
            if (StrUtil.isNotBlank(line3)) {
                g2d.drawString(line3, margin, y + 2 * lineHeight);
            }

            int tileWidth = 200;
            int tileHeight = 80;
            int tileRows = height / tileHeight + 1;
            int tileCols = width / tileWidth + 1;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity * 0.5f));
            g2d.rotate(Math.toRadians(-30), width / 2.0, height / 2.0);

            for (int row = 0; row < tileRows; row++) {
                for (int col = 0; col < tileCols; col++) {
                    int x = col * tileWidth - width / 2;
                    int ty = row * tileHeight - height / 2;
                    String tileText = StrUtil.isNotBlank(watermarkInfo.getEventNo()) ?
                            watermarkInfo.getEventNo() : watermarkInfo.getReporterName();
                    g2d.drawString(tileText, x, ty);
                }
            }

            g2d.dispose();

            String formatName = extension.equals("jpg") ? "jpeg" : extension;
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
            if (!writers.hasNext()) {
                throw new BusinessException("不支持的图片格式：" + extension);
            }

            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(IMAGE_COMPRESSION_QUALITY);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(watermarkedImage, null, null), param);
            } finally {
                writer.dispose();
            }

            return baos.toByteArray();
        }
    }

    private boolean isImageFile(String extension) {
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    private boolean isVideoFile(String extension) {
        return ALLOWED_VIDEO_EXTENSIONS.contains(extension);
    }
}
