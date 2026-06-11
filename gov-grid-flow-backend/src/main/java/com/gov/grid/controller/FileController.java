package com.gov.grid.controller;

import cn.hutool.core.io.FileUtil;
import com.gov.grid.common.Result;
import com.gov.grid.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Api(tags = "文件管理")
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @ApiOperation("上传文件")
    @PostMapping("/upload")
    public Result<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> fileUrls = fileService.uploadFiles(files);
        return Result.success("文件上传成功", fileUrls);
    }

    @ApiOperation("访问/下载文件")
    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename, HttpServletRequest request) {
        String filePath = fileService.getFilePath(filename);
        File file = new File(filePath);

        byte[] fileBytes = FileUtil.readBytes(file);
        String contentType = FileUtil.getMimeType(filePath);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        boolean isInline = shouldInline(contentType, request);
        String disposition = isInline ? "inline" : "attachment";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(fileBytes.length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + encodedFilename + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileBytes);
    }

    private boolean shouldInline(String contentType, HttpServletRequest request) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/") || contentType.startsWith("video/");
    }
}
