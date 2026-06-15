package com.gov.grid.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.service.EventHeatWarningService;
import com.gov.grid.vo.EventHeatForecastVO;
import com.gov.grid.vo.EventTypeForecastVO;
import com.gov.grid.vo.HeatmapCalendarVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHeatWarningServiceImpl implements EventHeatWarningService {

    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SysNotificationMapper sysNotificationMapper;

    private static final int HEAT_LEVEL_LOW = 1;
    private static final int HEAT_LEVEL_MEDIUM = 2;
    private static final int HEAT_LEVEL_HIGH = 3;
    private static final int HEAT_LEVEL_CRITICAL = 4;

    private static final String[] EVENT_TYPES = {
            "environment", "facility", "security", "service",
            "traffic", "public_facility", "dispute", "safety_hazard", "other"
    };

    private static final Map<String, String> EVENT_TYPE_NAMES = new HashMap<>();

    static {
        EVENT_TYPE_NAMES.put("environment", "环境卫生");
        EVENT_TYPE_NAMES.put("facility", "市政设施");
        EVENT_TYPE_NAMES.put("security", "治安问题");
        EVENT_TYPE_NAMES.put("service", "民生服务");
        EVENT_TYPE_NAMES.put("traffic", "交通出行");
        EVENT_TYPE_NAMES.put("public_facility", "公共设施");
        EVENT_TYPE_NAMES.put("dispute", "矛盾纠纷");
        EVENT_TYPE_NAMES.put("safety_hazard", "安全隐患");
        EVENT_TYPE_NAMES.put("other", "其他问题");
    }

    @Override
    public List<EventHeatForecastVO> getGridHeatForecast(Integer hours) {
        int forecastHours = hours != null ? hours : 24;
        List<GridInfo> gridList = gridInfoMapper.selectList(null);
        List<EventHeatForecastVO> result = new ArrayList<>();

        for (GridInfo grid : gridList) {
            EventHeatForecastVO forecast = calculateGridForecast(grid, forecastHours);
            result.add(forecast);
        }

        result.sort(Comparator.comparingDouble(EventHeatForecastVO::getHeatScore).reversed());
        return result;
    }

    @Override
    public EventHeatForecastVO getGridHeatForecastByGridId(Long gridId, Integer hours) {
        int forecastHours = hours != null ? hours : 24;
        GridInfo grid = gridInfoMapper.selectById(gridId);
        if (grid == null) {
            return null;
        }
        return calculateGridForecast(grid, forecastHours);
    }

    @Override
    public List<HeatmapCalendarVO> getCalendarHeatmap(Integer days, Long gridId) {
        int dayCount = days != null ? days : 30;
        List<HeatmapCalendarVO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        List<GridInfo> grids = new ArrayList<>();
        if (gridId != null) {
            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid != null) {
                grids.add(grid);
            }
        } else {
            grids = gridInfoMapper.selectList(null);
        }

        for (int i = dayCount - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            for (GridInfo grid : grids) {
                HeatmapCalendarVO vo = calculateDayHeatmap(grid, date);
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public List<HeatmapCalendarVO> getCalendarHeatmapByMonth(Integer year, Integer month, Long gridId) {
        List<HeatmapCalendarVO> result = new ArrayList<>();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        List<GridInfo> grids = new ArrayList<>();
        if (gridId != null) {
            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid != null) {
                grids.add(grid);
            }
        } else {
            grids = gridInfoMapper.selectList(null);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            for (GridInfo grid : grids) {
                HeatmapCalendarVO vo = calculateDayHeatmap(grid, date);
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public boolean pushWarningNotification(Long gridId) {
        try {
            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid == null) {
                return false;
            }

            EventHeatForecastVO forecast = calculateGridForecast(grid, 24);

            SysNotification notification = new SysNotification();
            notification.setTitle("【事件预警】" + grid.getGridName() + "预警通知");
            notification.setContent(buildWarningContent(grid, forecast));
            notification.setType("warning");
            notification.setUserId(grid.getGridLeaderId());
            notification.setBizId(grid.getId());
            notification.setIsRead(0);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            notification.setDeleted(0);

            sysNotificationMapper.insert(notification);
            return true;
        } catch (Exception e) {
            log.error("推送预警通知失败", e);
            return false;
        }
    }

    @Override
    public List<EventHeatForecastVO> getHighWarningGrids() {
        List<EventHeatForecastVO> allForecasts = getGridHeatForecast(24);
        return allForecasts.stream()
                .filter(f -> f.getHeatLevel() >= HEAT_LEVEL_HIGH)
                .collect(Collectors.toList());
    }

    private EventHeatForecastVO calculateGridForecast(GridInfo grid, int hours) {
        EventHeatForecastVO vo = new EventHeatForecastVO();
        vo.setGridId(grid.getId());
        vo.setGridName(grid.getGridName());
        vo.setGridCode(grid.getGridCode());
        vo.setForecastTime(LocalDateTime.now());

        List<EventInfo> historicalEvents = getHistoricalEvents(grid.getId(), 90);

        Map<String, Integer> typeCountMap = new HashMap<>();
        Map<Integer, Integer> hourCountMap = new HashMap<>();
        Map<DayOfWeek, Integer> dayOfWeekCountMap = new HashMap<>();

        for (EventInfo event : historicalEvents) {
            String eventType = event.getEventType();
            if (eventType != null) {
                typeCountMap.merge(eventType, 1, Integer::sum);
            }

            LocalDateTime eventTime = event.getEventTimestamp() != null ? event.getEventTimestamp() : event.getCreatedAt();
            if (eventTime != null) {
                int hour = eventTime.getHour();
                hourCountMap.merge(hour, 1, Integer::sum);
                DayOfWeek dayOfWeek = eventTime.getDayOfWeek();
                dayOfWeekCountMap.merge(dayOfWeek, 1, Integer::sum);
            }
        }

        double baseHeatScore = calculateBaseHeatScore(historicalEvents.size(), hours);
        double timeFactor = calculateTimeFactor(hourCountMap, dayOfWeekCountMap, hours);
        double weatherFactor = calculateWeatherFactor();
        double holidayFactor = calculateHolidayFactor();

        double heatScore = baseHeatScore * timeFactor * weatherFactor * holidayFactor;
        heatScore = BigDecimal.valueOf(heatScore).setScale(2, RoundingMode.HALF_UP).doubleValue();

        vo.setHeatScore(heatScore);
        vo.setHeatLevel(determineHeatLevel(heatScore));
        vo.setHeatLevelDesc(getHeatLevelDesc(vo.getHeatLevel()));

        List<EventTypeForecastVO> typeForecasts = calculateEventTypeForecasts(typeCountMap, historicalEvents.size(), hours);
        vo.setEventTypeForecasts(typeForecasts);

        vo.setPredictedEventCount((int) Math.ceil(heatScore / 10.0));
        vo.setWeatherCondition(getCurrentWeather());
        vo.setIsHoliday(isTodayHoliday());
        vo.setSuggestion(generateSuggestion(vo.getHeatLevel(), typeForecasts));

        return vo;
    }

    private HeatmapCalendarVO calculateDayHeatmap(GridInfo grid, LocalDate date) {
        HeatmapCalendarVO vo = new HeatmapCalendarVO();
        vo.setDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        vo.setGridId(grid.getId());
        vo.setGridName(grid.getGridName());

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, grid.getId())
                .ge(EventInfo::getCreatedAt, date.atStartOfDay())
                .lt(EventInfo::getCreatedAt, date.plusDays(1).atStartOfDay());
        Integer eventCount = Math.toIntExact(eventInfoMapper.selectCount(wrapper));
        vo.setEventCount(eventCount);

        int heatValue = calculateHeatValue(eventCount, date);
        vo.setHeatValue(heatValue);
        vo.setHeatLevel(getHeatLevelFromValue(heatValue));

        List<EventTypeForecastVO> topTypes = getTopEventTypesForDay(grid.getId(), date, 3);
        vo.setTopEventTypes(topTypes);

        return vo;
    }

    private List<EventInfo> getHistoricalEvents(Long gridId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, gridId)
                .ge(EventInfo::getCreatedAt, startTime);
        return eventInfoMapper.selectList(wrapper);
    }

    private double calculateBaseHeatScore(int totalEvents, int hours) {
        if (totalEvents == 0) {
            return 10.0;
        }
        double avgPerDay = totalEvents / 90.0;
        double avgPerHour = avgPerDay / 24.0;
        return avgPerHour * hours * 15.0;
    }

    private double calculateTimeFactor(Map<Integer, Integer> hourCountMap, Map<DayOfWeek, Integer> dayOfWeekCountMap, int hours) {
        double factor = 1.0;
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek currentDay = now.getDayOfWeek();
        int currentHour = now.getHour();

        boolean isWeekend = currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY;
        factor *= isWeekend ? 1.2 : 1.0;

        int peakHours = 0;
        for (int i = 0; i < hours; i++) {
            int hour = (currentHour + i) % 24;
            if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
                peakHours++;
            }
        }
        factor += (peakHours * 0.05);

        return Math.min(factor, 2.0);
    }

    private double calculateWeatherFactor() {
        Random random = new Random();
        int weatherType = random.nextInt(5);
        switch (weatherType) {
            case 0:
                return 0.8;
            case 1:
                return 1.3;
            case 2:
                return 1.1;
            case 3:
                return 0.9;
            default:
                return 1.0;
        }
    }

    private double calculateHolidayFactor() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        if ((month == 1 && day >= 20 && day <= 28) ||
            (month == 5 && day >= 1 && day <= 5) ||
            (month == 10 && day >= 1 && day <= 7)) {
            return 1.5;
        }

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return 1.2;
        }

        return 1.0;
    }

    private int determineHeatLevel(double heatScore) {
        if (heatScore < 30) {
            return HEAT_LEVEL_LOW;
        } else if (heatScore < 60) {
            return HEAT_LEVEL_MEDIUM;
        } else if (heatScore < 85) {
            return HEAT_LEVEL_HIGH;
        } else {
            return HEAT_LEVEL_CRITICAL;
        }
    }

    private String getHeatLevelDesc(int heatLevel) {
        switch (heatLevel) {
            case HEAT_LEVEL_LOW:
                return "低风险";
            case HEAT_LEVEL_MEDIUM:
                return "中风险";
            case HEAT_LEVEL_HIGH:
                return "高风险";
            case HEAT_LEVEL_CRITICAL:
                return "极高风险";
            default:
                return "未知";
        }
    }

    private List<EventTypeForecastVO> calculateEventTypeForecasts(Map<String, Integer> typeCountMap, int totalEvents, int hours) {
        List<EventTypeForecastVO> result = new ArrayList<>();

        if (totalEvents == 0) {
            for (int i = 0; i < EVENT_TYPES.length; i++) {
                EventTypeForecastVO vo = new EventTypeForecastVO();
                vo.setEventType(EVENT_TYPES[i]);
                vo.setEventTypeName(EVENT_TYPE_NAMES.get(EVENT_TYPES[i]));
                vo.setProbability(100.0 / EVENT_TYPES.length);
                vo.setPredictedCount(0);
                vo.setRank(i + 1);
                vo.setTrend("stable");
                result.add(vo);
            }
            return result;
        }

        for (String eventType : EVENT_TYPES) {
            int count = typeCountMap.getOrDefault(eventType, 0);
            double probability = (count * 100.0) / totalEvents;

            EventTypeForecastVO vo = new EventTypeForecastVO();
            vo.setEventType(eventType);
            vo.setEventTypeName(EVENT_TYPE_NAMES.get(eventType));
            vo.setProbability(BigDecimal.valueOf(probability).setScale(2, RoundingMode.HALF_UP).doubleValue());
            vo.setPredictedCount((int) Math.ceil((count / 90.0) * (hours / 24.0) * 1.1));
            vo.setTrend(getRandomTrend());
            result.add(vo);
        }

        result.sort(Comparator.comparingDouble(EventTypeForecastVO::getProbability).reversed());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    private String getRandomTrend() {
        String[] trends = {"up", "down", "stable"};
        Random random = new Random();
        return trends[random.nextInt(trends.length)];
    }

    private int calculateHeatValue(int eventCount, LocalDate date) {
        int baseValue = eventCount * 10;

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            baseValue += 5;
        }

        int month = date.getMonthValue();
        if (month >= 6 && month <= 8) {
            baseValue += 8;
        }

        return Math.min(baseValue, 100);
    }

    private String getHeatLevelFromValue(int heatValue) {
        if (heatValue < 30) {
            return "low";
        } else if (heatValue < 60) {
            return "medium";
        } else if (heatValue < 85) {
            return "high";
        } else {
            return "critical";
        }
    }

    private List<EventTypeForecastVO> getTopEventTypesForDay(Long gridId, LocalDate date, int topN) {
        List<EventInfo> events = getEventsByGridAndDate(gridId, date);

        Map<String, Integer> typeCountMap = new HashMap<>();
        for (EventInfo event : events) {
            String eventType = event.getEventType();
            if (eventType != null) {
                typeCountMap.merge(eventType, 1, Integer::sum);
            }
        }

        List<EventTypeForecastVO> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            EventTypeForecastVO vo = new EventTypeForecastVO();
            vo.setEventType(entry.getKey());
            vo.setEventTypeName(EVENT_TYPE_NAMES.getOrDefault(entry.getKey(), entry.getKey()));
            vo.setPredictedCount(entry.getValue());
            vo.setProbability(events.isEmpty() ? 0.0 : (entry.getValue() * 100.0) / events.size());
            result.add(vo);
        }

        result.sort(Comparator.comparingInt(EventTypeForecastVO::getPredictedCount).reversed());

        if (result.size() > topN) {
            result = result.subList(0, topN);
        }

        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    private List<EventInfo> getEventsByGridAndDate(Long gridId, LocalDate date) {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, gridId)
                .ge(EventInfo::getCreatedAt, date.atStartOfDay())
                .lt(EventInfo::getCreatedAt, date.plusDays(1).atStartOfDay());
        return eventInfoMapper.selectList(wrapper);
    }

    private String getCurrentWeather() {
        String[] weathers = {"晴", "多云", "阴", "小雨", "中雨"};
        Random random = new Random();
        return weathers[random.nextInt(weathers.length)];
    }

    private boolean isTodayHoliday() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private String generateSuggestion(int heatLevel, List<EventTypeForecastVO> typeForecasts) {
        StringBuilder suggestion = new StringBuilder();

        switch (heatLevel) {
            case HEAT_LEVEL_LOW:
                suggestion.append("当前区域事件风险较低，建议按常规频次巡查。");
                break;
            case HEAT_LEVEL_MEDIUM:
                suggestion.append("当前区域存在一定事件风险，建议适当增加巡查频次。");
                break;
            case HEAT_LEVEL_HIGH:
                suggestion.append("当前区域事件风险较高，建议加强巡查力度，重点关注高发区域。");
                break;
            case HEAT_LEVEL_CRITICAL:
                suggestion.append("当前区域事件风险极高，请立即加强巡查，增派网格员重点防控！");
                break;
            default:
                break;
        }

        if (typeForecasts != null && !typeForecasts.isEmpty()) {
            suggestion.append("重点关注：");
            List<String> topTypes = typeForecasts.stream()
                    .limit(3)
                    .map(EventTypeForecastVO::getEventTypeName)
                    .collect(Collectors.toList());
            suggestion.append(String.join("、", topTypes));
            suggestion.append("。");
        }

        return suggestion.toString();
    }

    private String buildWarningContent(GridInfo grid, EventHeatForecastVO forecast) {
        StringBuilder content = new StringBuilder();
        content.append("网格【").append(grid.getGridName()).append("】");
        content.append("未来24小时事件预警等级：").append(forecast.getHeatLevelDesc());
        content.append("，预警评分：").append(forecast.getHeatScore());
        content.append("。").append(forecast.getSuggestion());
        return content.toString();
    }
}
