package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.AsrService;
import com.gov.grid.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@Api(tags = "语音转写")
@RestController
@RequestMapping("/asr")
@RequiredArgsConstructor
public class AsrController {

    private final AsrService asrService;
    private final FileService fileService;

    @ApiOperation("上传语音并转写为文字")
    @PostMapping("/transcribe")
    public Result<String> transcribeVoice(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "zh-CN") String language) {
        if (file.isEmpty()) {
            return Result.error("请上传语音文件");
        }

        try {
            java.util.List<String> urls = fileService.uploadFiles(new MultipartFile[]{file});
            if (urls.isEmpty()) {
                return Result.error("语音文件上传失败");
            }

            String voiceUrl = urls.get(0);
            String fileName = voiceUrl.substring(voiceUrl.lastIndexOf("/") + 1);
            String filePath = fileService.getFilePath(fileName);

            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                return Result.error("语音文件不存在");
            }

            String text = asrService.speechToText(filePath, language);

            log.info("语音转写成功，文件：{}，长度：{}字", fileName, text.length());

            return Result.success("转写成功", text);
        } catch (Exception e) {
            log.error("语音转写失败", e);
            return Result.error("语音转写失败：" + e.getMessage());
        }
    }
}
