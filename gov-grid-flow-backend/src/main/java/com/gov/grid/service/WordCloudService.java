package com.gov.grid.service;

import java.util.List;
import java.util.Map;

public interface WordCloudService {

    List<Map.Entry<String, Integer>> extractKeywords(List<String> texts, int topN);

    List<Map.Entry<String, Integer>> extractKeywords(String text, int topN);

    List<String> segment(String text);
}
