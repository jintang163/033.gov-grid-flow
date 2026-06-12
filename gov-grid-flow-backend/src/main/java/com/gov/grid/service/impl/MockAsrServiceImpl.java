package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.service.AsrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MockAsrServiceImpl implements AsrService {

    @Value("${asr.mock.enabled:true}")
    private boolean mockEnabled;

    private static final String[] MOCK_PHRASES = {
            "网格内发现有垃圾堆积未及时清理，天气炎热产生异味，请尽快处理。",
            "小区门口的路灯不亮了，晚上居民出行很不方便，希望能尽快维修。",
            "楼道里有杂物堆积，存在消防隐患，请相关部门派人清理。",
            "公园的健身器材有损坏，存在安全隐患，需要及时更换。",
            "下水道堵塞了，下雨后路面积水严重，影响居民出行。",
            "小区绿化带有杂草丛生，希望能安排人员修剪整理。",
            "附近工地夜间施工噪音很大，严重影响居民休息，请协调处理。",
            "有居民反映邻里之间有矛盾纠纷，需要社区工作人员调解。"
    };

    @Override
    public String speechToText(String audioFilePath, String language) {
        if (StrUtil.isBlank(audioFilePath)) {
            throw new BusinessException("音频文件路径不能为空");
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            throw new BusinessException("音频文件不存在");
        }

        if (!mockEnabled) {
            throw new BusinessException("ASR服务暂未配置，请联系管理员");
        }

        try {
            Thread.sleep(500 + ThreadLocalRandom.current().nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int index = ThreadLocalRandom.current().nextInt(MOCK_PHRASES.length);
        String result = MOCK_PHRASES[index];

        log.info("ASR转写完成，文件：{}，结果：{}", audioFile.getName(), result);
        return result;
    }
}
