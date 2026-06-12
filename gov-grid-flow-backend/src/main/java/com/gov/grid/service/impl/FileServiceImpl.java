package com.gov.grid.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.service.FileService;
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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv");
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList("mp3", "wav", "m4a", "aac", "ogg", "amr", "webm");
    private static final float IMAGE_COMPRESSION_QUALITY = 0.7f;
    private static final long IMAGE_COMPRESS_THRESHOLD = 500 * 1024;

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.access.prefix:/file}")
    private String accessPrefix;

    @PostConstruct
    public void init() {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
            log.info("创建文件上传目录：{}", uploadDir.getAbsolutePath());
        }
    }

    @Override
    public List<String> uploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的文件");
        }

        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            String fileUrl = uploadSingleFile(file);
            fileUrls.add(fileUrl);
        }

        if (fileUrls.isEmpty()) {
            throw new BusinessException("文件上传失败");
        }

        return fileUrls;
    }

    @Override
    public String getFilePath(String filename) {
        if (StrUtil.isBlank(filename)) {
            throw new BusinessException("文件名不能为空");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException("非法文件名");
        }
        File file = new File(uploadPath, filename);
        if (!file.exists()) {
            throw new BusinessException("文件不存在");
        }
        return file.getAbsolutePath();
    }

    private String uploadSingleFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new BusinessException("文件名不能为空");
        }

        String extension = FileUtil.extName(originalFilename).toLowerCase();
        if (!isAllowedExtension(extension)) {
            throw new BusinessException("不支持的文件类型：" + extension);
        }

        String newFilename = IdUtil.fastSimpleUUID() + "." + extension;
        File destFile = new File(uploadPath, newFilename);

        try {
            if (isImageFile(extension) && file.getSize() > IMAGE_COMPRESS_THRESHOLD) {
                compressAndSaveImage(file, destFile, extension);
            } else {
                file.transferTo(destFile);
            }
            log.info("文件上传成功：{}，大小：{}字节", newFilename, destFile.length());
            return accessPrefix + "/" + newFilename;
        } catch (IOException e) {
            log.error("文件上传失败：{}", originalFilename, e);
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }
    }

    private void compressAndSaveImage(MultipartFile file, File destFile, String extension) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            file.transferTo(destFile);
            return;
        }

        String formatName = extension.equals("jpg") ? "jpeg" : extension;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            file.transferTo(destFile);
            return;
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(IMAGE_COMPRESSION_QUALITY);
        }

        try (OutputStream os = new FileOutputStream(destFile);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private boolean isAllowedExtension(String extension) {
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension)
                || ALLOWED_VIDEO_EXTENSIONS.contains(extension)
                || ALLOWED_AUDIO_EXTENSIONS.contains(extension);
    }

    private boolean isImageFile(String extension) {
        return ALLOWED_IMAGE_EXTENSIONS.contains(extension);
    }

    private boolean isAudioFile(String extension) {
        return ALLOWED_AUDIO_EXTENSIONS.contains(extension);
    }
}
