package com.gov.grid.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    List<String> uploadFiles(MultipartFile[] files);

    String getFilePath(String filename);
}
