package com.gov.grid.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.grid.common.Result;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.DigitalEnvelopeDTO;
import com.gov.grid.entity.FileWatermark;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.FileWatermarkMapper;
import com.gov.grid.service.EncryptionService;
import com.gov.grid.service.FileService;
import com.gov.grid.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileWatermarkMapper fileWatermarkMapper;
    private final EncryptionService encryptionService;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;

    @ApiOperation("上传文件")
    @PostMapping("/upload")
    public Result<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> fileUrls = fileService.uploadFiles(files);
        return Result.success("文件上传成功", fileUrls);
    }

    @ApiOperation("访问/下载文件（自动解密数字信封）")
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename, HttpServletRequest request) throws Exception {
        String filePath = fileService.getFilePath(filename);
        File file = new File(filePath);
        byte[] fileBytes = FileUtil.readBytes(file);

        String fileUrl = "/file/" + filename;
        FileWatermark fileWatermark = fileWatermarkMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileWatermark>()
                        .eq(FileWatermark::getFileUrl, fileUrl)
        );

        String originalFilename = filename;
        if (fileWatermark != null && fileWatermark.getIsEncrypted() == 1) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            SysUser currentUser = sysUserService.getByUsername(username);
            if (currentUser == null || currentUser.getDeptId() == null) {
                throw new BusinessException("您无权访问此加密文件");
            }

            try {
                DigitalEnvelopeDTO envelope = objectMapper.readValue(fileBytes, DigitalEnvelopeDTO.class);
                fileBytes = encryptionService.openDigitalEnvelope(envelope, currentUser.getDeptId());
                originalFilename = extractOriginalFilename(fileWatermark);
            } catch (Exception e) {
                log.error("解密文件失败：{}", filename, e);
                throw new BusinessException("文件解密失败：" + e.getMessage());
            }
        }

        String contentType = FileUtil.getMimeType(originalFilename);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        boolean isInline = shouldInline(contentType, request);
        String disposition = isInline ? "inline" : "attachment";
        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(fileBytes.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + encodedFilename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }

    @ApiOperation("解密并下载敏感证据（仅处置部门）")
    @GetMapping("/decrypt/{filename}")
    public ResponseEntity<byte[]> decryptFile(@PathVariable String filename,
                                               @RequestParam("deptId") Long deptId) throws Exception {
        String filePath = fileService.getFilePath(filename);
        File file = new File(filePath);
        byte[] fileBytes = FileUtil.readBytes(file);

        String fileUrl = "/file/" + filename;
        FileWatermark fileWatermark = fileWatermarkMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileWatermark>()
                        .eq(FileWatermark::getFileUrl, fileUrl)
        );

        if (fileWatermark == null || fileWatermark.getIsEncrypted() != 1) {
            return getFile(filename, null);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SysUser currentUser = sysUserService.getByUsername(username);
        if (currentUser == null || !deptId.equals(currentUser.getDeptId())) {
            throw new BusinessException("您无权解密此部门的加密文件");
        }

        DigitalEnvelopeDTO envelope = objectMapper.readValue(fileBytes, DigitalEnvelopeDTO.class);
        byte[] decryptedBytes = encryptionService.openDigitalEnvelope(envelope, deptId);
        String originalFilename = extractOriginalFilename(fileWatermark);

        String contentType = FileUtil.getMimeType(originalFilename);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(decryptedBytes.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(decryptedBytes);
    }

    private String extractOriginalFilename(FileWatermark watermark) {
        try {
            if (StrUtil.isNotBlank(watermark.getWatermarkInfo())) {
                com.gov.grid.dto.WatermarkInfo info = objectMapper.readValue(
                        watermark.getWatermarkInfo(), com.gov.grid.dto.WatermarkInfo.class
                );
                if (StrUtil.isNotBlank(info.getEventNo())) {
                    return info.getEventNo() + "_evidence.jpg";
                }
            }
        } catch (Exception e) {
            log.warn("解析水印信息失败", e);
        }
        return "evidence_" + watermark.getId() + ".jpg";
    }

    private boolean shouldInline(String contentType, HttpServletRequest request) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/");
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileController.class);
}
