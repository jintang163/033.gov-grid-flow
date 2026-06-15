package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.GridMember;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.GridMemberMapper;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.EventHeatWarningService;
import com.gov.grid.service.NotificationService;
import com.gov.grid.vo.EventHeatForecastVO;
import com.gov.grid.vo.EventTypeForecastVO;
import com.gov.grid.vo.HeatmapCalendarVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventHeatWarningServiceImpl implements EventHeatWarningService {

    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final GridMemberMapper gridMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    @Value("${weather.api.enabled:true}")
    private boolean weatherEnabled;

    @Value("${weather.api.mock:true}")
    private boolean weatherMock;

    @Value("${weather.api.provider:qweather}")
    private String weatherProvider;

    @Value("${weather.api.base-url:https://devapi.qweather.com/v7}")
    private String weatherBaseUrl;

    @Value("${weather.api.key:}")
    private String weatherApiKey;

    @Value("${weather.api.timeout:10000}")
    private int weatherTimeout;

    @Value("${warning.heat-threshold:60}")
    private int heatThreshold;

    @Value("${warning.forecast-days:14}")
    private int forecastDays;

    private static final int HEAT_LEVEL_LOW = 1;
    private static final int HEAT_LEVEL_MEDIUM = 2;
    private static final int HEAT_LEVEL_HIGH = 3;
    private static final int HEAT_LEVEL_CRITICAL = 4;

    private static final int HISTORY_DAYS = 180;

    private static final String[] EVENT_TYPES = {
            "environment", "facility", "security", "service",
            "traffic", "public_facility", "dispute", "safety_hazard", "other"
    };

    private static final Map<String, String> EVENT_TYPE_NAMES = new HashMap<>();
    private static final Map<String, Double> WEATHER_EVENT_WEIGHTS = new HashMap<>();

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

        WEATHER_EVENT_WEIGHTS.put("sunny_environment", 1.2);
        WEATHER_EVENT_WEIGHTS.put("sunny_security", 1.1);
        WEATHER_EVENT_WEIGHTS.put("rainy_environment", 1.5);
        WEATHER_EVENT_WEIGHTS.put("rainy_facility", 1.8);
        WEATHER_EVENT_WEIGHTS.put("rainy_traffic", 2.0);
        WEATHER_EVENT_WEIGHTS.put("rainy_safety_hazard", 1.6);
        WEATHER_EVENT_WEIGHTS.put("snow_facility", 2.2);
        WEATHER_EVENT_WEIGHTS.put("snow_traffic", 2.5);
        WEATHER_EVENT_WEIGHTS.put("snow_safety_hazard", 2.0);
        WEATHER_EVENT_WEIGHTS.put("hot_environment", 1.4);
        WEATHER_EVENT_WEIGHTS.put("hot_service", 1.3);
        WEATHER_EVENT_WEIGHTS.put("storm_facility", 2.5);
        WEATHER_EVENT_WEIGHTS.put("storm_safety_hazard", 2.8);
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

        List<GridInfo> grids = resolveGrids(gridId);

        LocalDate today = LocalDate.now();
        int pastDays = dayCount / 2;
        int futureDays = dayCount - pastDays;

        for (int i = pastDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            result.addAll(calculateDayHeatmapForGrids(grids, date, false));
        }

        for (int i = 0; i < futureDays; i++) {
            LocalDate date = today.plusDays(i);
            result.addAll(calculateDayHeatmapForGrids(grids, date, true));
        }

        return aggregateCalendarData(result, gridId);
    }

    @Override
    public List<HeatmapCalendarVO> getCalendarHeatmapByMonth(Integer year, Integer month, Long gridId) {
        List<HeatmapCalendarVO> result = new ArrayList<>();
        List<GridInfo> grids = resolveGrids(gridId);

        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            boolean isFuture = date.isAfter(today);
            result.addAll(calculateDayHeatmapForGrids(grids, date, isFuture));
        }

        return aggregateCalendarData(result, gridId);
    }

    @Override
    public boolean pushWarningNotification(Long gridId) {
        try {
            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid == null) {
                return false;
            }

            EventHeatForecastVO forecast = calculateGridForecast(grid, 24);
            String warningContent = buildWarningContent(grid, forecast);
            String warningTitle = "【事件预警】" + grid.getGridName() + "-" + forecast.getHeatLevelDesc();

            List<Long> memberUserIds = getGridAllMemberUserIds(gridId);

            int successCount = 0;
            for (Long userId : memberUserIds) {
                boolean ok = notificationService.sendAppPush(
                        userId,
                        warningTitle,
                        warningContent,
                        "EVENT_WARNING",
                        gridId
                );
                if (ok) successCount++;
            }

            if (grid.getGridLeaderId() != null && !memberUserIds.contains(grid.getGridLeaderId())) {
                notificationService.sendAppPush(
                        grid.getGridLeaderId(),
                        warningTitle,
                        warningContent,
                        "EVENT_WARNING",
                        gridId
                );
                successCount++;
            }

            log.info("推送预警通知，网格: {}, 接收人数量: {}, 成功数量: {}", gridId, memberUserIds.size(), successCount);
            return successCount > 0;
        } catch (Exception e) {
            log.error("推送预警通知失败, gridId: {}", gridId, e);
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

    public int scanAndPushHighWarning() {
        int pushedCount = 0;
        try {
            List<EventHeatForecastVO> highWarnings = getHighWarningGrids();
            log.info("定时扫描高风险预警，发现 {} 个高风险网格", highWarnings.size());

            for (EventHeatForecastVO warning : highWarnings) {
                if (warning.getHeatScore() >= heatThreshold) {
                    boolean pushed = pushWarningNotification(warning.getGridId());
                    if (pushed) {
                        pushedCount++;
                    }
                }
            }
            log.info("定时扫描高风险预警完成，成功推送 {} 个网格", pushedCount);
        } catch (Exception e) {
            log.error("定时扫描高风险预警异常", e);
        }
        return pushedCount;
    }

    private EventHeatForecastVO calculateGridForecast(GridInfo grid, int hours) {
        EventHeatForecastVO vo = new EventHeatForecastVO();
        vo.setGridId(grid.getId());
        vo.setGridName(grid.getGridName());
        vo.setGridCode(grid.getGridCode());
        vo.setForecastTime(LocalDateTime.now());

        List<EventInfo> historicalEvents = getHistoricalEvents(grid.getId(), HISTORY_DAYS);

        Map<String, Integer> typeCountMap = new HashMap<>();
        Map<Integer, Integer> hourCountMap = new HashMap<>();
        Map<DayOfWeek, Integer> dayOfWeekCountMap = new HashMap<>();
        Map<Month, Integer> monthCountMap = new HashMap<>();

        for (EventInfo event : historicalEvents) {
            String eventType = event.getEventType();
            if (eventType != null) {
                typeCountMap.merge(eventType, 1, Integer::sum);
            }

            LocalDateTime eventTime = event.getEventTimestamp() != null ? event.getEventTimestamp() : event.getCreatedAt();
            if (eventTime != null) {
                hourCountMap.merge(eventTime.getHour(), 1, Integer::sum);
                dayOfWeekCountMap.merge(eventTime.getDayOfWeek(), 1, Integer::sum);
                monthCountMap.merge(eventTime.getMonth(), 1, Integer::sum);
            }
        }

        int totalEvents = historicalEvents.size();

        double baseHeatScore = calculateBaseHeatScore(totalEvents, HISTORY_DAYS, hours);
        double timeFactor = calculateTimeFactor(hourCountMap, dayOfWeekCountMap, monthCountMap, hours);
        double weatherFactor = calculateWeatherFactor(grid);
        double holidayFactor = calculateHolidayFactor();
        double eventTypeFactor = calculateEventTypeFactor(typeCountMap, totalEvents);
        double recurrenceFactor = calculateRecurrenceFactor(grid.getId(), totalEvents);

        double heatScore = baseHeatScore * timeFactor * weatherFactor * holidayFactor * eventTypeFactor * recurrenceFactor;
        heatScore = Math.min(heatScore, 100.0);
        heatScore = BigDecimal.valueOf(heatScore).setScale(2, RoundingMode.HALF_UP).doubleValue();

        vo.setHeatScore(heatScore);
        vo.setHeatLevel(determineHeatLevel(heatScore));
        vo.setHeatLevelDesc(getHeatLevelDesc(vo.getHeatLevel()));

        List<EventTypeForecastVO> typeForecasts = calculateEventTypeForecasts(
                typeCountMap, hourCountMap, dayOfWeekCountMap, totalEvents, hours, weatherFactor
        );
        vo.setEventTypeForecasts(typeForecasts);

        vo.setPredictedEventCount((int) Math.ceil((totalEvents / (double) HISTORY_DAYS) * timeFactor * (hours / 24.0) * 1.2));
        vo.setWeatherCondition(getCurrentWeather(grid).getText());
        vo.setIsHoliday(isTodayHoliday());
        vo.setSuggestion(generateSuggestion(vo.getHeatLevel(), typeForecasts, vo.getHeatScore()));

        return vo;
    }

    private List<HeatmapCalendarVO> calculateDayHeatmapForGrids(List<GridInfo> grids, LocalDate date, boolean isFuture) {
        List<HeatmapCalendarVO> result = new ArrayList<>();

        for (GridInfo grid : grids) {
            HeatmapCalendarVO vo = calculateSingleDayHeatmap(grid, date, isFuture);
            result.add(vo);
        }

        return result;
    }

    private HeatmapCalendarVO calculateSingleDayHeatmap(GridInfo grid, LocalDate date, boolean isFuture) {
        HeatmapCalendarVO vo = new HeatmapCalendarVO();
        vo.setDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        vo.setGridId(grid.getId());
        vo.setGridName(grid.getGridName());

        int eventCount;
        int heatValue;

        if (isFuture) {
            eventCount = predictFutureEventCount(grid, date);
            heatValue = calculateFutureHeatValue(grid, date, eventCount);
        } else {
            eventCount = getHistoricalEventCountByDate(grid.getId(), date);
            heatValue = calculateHistoricalHeatValue(grid.getId(), date, eventCount);
        }

        vo.setEventCount(eventCount);
        vo.setHeatValue(heatValue);
        vo.setHeatLevel(getHeatLevelFromValue(heatValue));

        List<EventTypeForecastVO> topTypes = getTopEventTypesForDate(grid.getId(), date, isFuture, 3);
        vo.setTopEventTypes(topTypes);

        return vo;
    }

    private List<HeatmapCalendarVO> aggregateCalendarData(List<HeatmapCalendarVO> data, Long gridId) {
        if (gridId != null) {
            return data;
        }

        Map<String, HeatmapCalendarVO> aggregatedMap = new LinkedHashMap<>();

        for (HeatmapCalendarVO item : data) {
            String key = item.getDate();

            if (aggregatedMap.containsKey(key)) {
                HeatmapCalendarVO existing = aggregatedMap.get(key);
                int totalHeat = existing.getHeatValue() + item.getHeatValue();
                int totalEvents = existing.getEventCount() + item.getEventCount();

                existing.setEventCount(totalEvents);
                existing.setHeatValue(Math.min(totalHeat, 100));
                existing.setHeatLevel(getHeatLevelFromValue(existing.getHeatValue()));
                existing.setGridId(null);
                existing.setGridName("全部网格");

                if (item.getTopEventTypes() != null) {
                    Map<String, EventTypeForecastVO> typeMap = new HashMap<>();
                    if (existing.getTopEventTypes() != null) {
                        for (EventTypeForecastVO t : existing.getTopEventTypes()) {
                            typeMap.put(t.getEventType(), t);
                        }
                    }
                    for (EventTypeForecastVO t : item.getTopEventTypes()) {
                        if (typeMap.containsKey(t.getEventType())) {
                            EventTypeForecastVO et = typeMap.get(t.getEventType());
                            et.setPredictedCount(et.getPredictedCount() + t.getPredictedCount());
                            et.setProbability(et.getProbability() + t.getProbability());
                        } else {
                            typeMap.put(t.getEventType(), t);
                        }
                    }
                    List<EventTypeForecastVO> merged = new ArrayList<>(typeMap.values());
                    merged.sort(Comparator.comparingInt(EventTypeForecastVO::getPredictedCount).reversed());
                    existing.setTopEventTypes(merged.stream().limit(3).collect(Collectors.toList()));
                }
            } else {
                aggregatedMap.put(key, copyHeatmapCalendar(item));
            }
        }

        return new ArrayList<>(aggregatedMap.values());
    }

    private HeatmapCalendarVO copyHeatmapCalendar(HeatmapCalendarVO source) {
        HeatmapCalendarVO target = new HeatmapCalendarVO();
        target.setDate(source.getDate());
        target.setHeatValue(source.getHeatValue());
        target.setHeatLevel(source.getHeatLevel());
        target.setEventCount(source.getEventCount());
        target.setGridId(source.getGridId());
        target.setGridName(source.getGridName());
        if (source.getTopEventTypes() != null) {
            target.setTopEventTypes(new ArrayList<>(source.getTopEventTypes()));
        }
        return target;
    }

    private List<GridInfo> resolveGrids(Long gridId) {
        List<GridInfo> grids = new ArrayList<>();
        if (gridId != null) {
            GridInfo grid = gridInfoMapper.selectById(gridId);
            if (grid != null) {
                grids.add(grid);
            }
        } else {
            grids = gridInfoMapper.selectList(null);
        }
        return grids;
    }

    private List<Long> getGridAllMemberUserIds(Long gridId) {
        LambdaQueryWrapper<GridMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GridMember::getGridId, gridId);
        List<GridMember> members = gridMemberMapper.selectList(wrapper);

        return members.stream()
                .map(GridMember::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<EventInfo> getHistoricalEvents(Long gridId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, gridId)
                .ge(EventInfo::getCreatedAt, startTime);
        return eventInfoMapper.selectList(wrapper);
    }

    private double calculateBaseHeatScore(int totalEvents, int historyDays, int forecastHours) {
        if (totalEvents == 0) {
            return 5.0;
        }
        double avgEventsPerDay = totalEvents / (double) historyDays;
        double avgEventsPerForecast = avgEventsPerDay * (forecastHours / 24.0);

        double score = avgEventsPerForecast * 20.0;

        return Math.min(score, 80.0);
    }

    private double calculateTimeFactor(Map<Integer, Integer> hourCountMap,
                                       Map<DayOfWeek, Integer> dayOfWeekCountMap,
                                       Map<Month, Integer> monthCountMap,
                                       int forecastHours) {
        double factor = 1.0;

        int totalHourEvents = hourCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalHourEvents > 0) {
            LocalDateTime now = LocalDateTime.now();
            DayOfWeek currentDay = now.getDayOfWeek();
            int currentHour = now.getHour();

            double futureHourWeight = 0.0;
            double normalHourWeight = 1.0 / 24.0;

            for (int i = 0; i < forecastHours; i++) {
                int hour = (currentHour + i) % 24;
                DayOfWeek day = currentDay.plusDays((currentHour + i) / 24);

                double hourRatio = hourCountMap.getOrDefault(hour, 0) / (double) totalHourEvents;
                double weight = hourRatio / normalHourWeight;

                boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
                if (isWeekend) {
                    weight *= 1.15;
                }

                if (hour >= 7 && hour <= 9) weight *= 1.3;
                else if (hour >= 11 && hour <= 13) weight *= 1.2;
                else if (hour >= 17 && hour <= 19) weight *= 1.35;
                else if (hour >= 22 || hour <= 5) weight *= 1.1;

                futureHourWeight += weight;
            }
            factor *= (futureHourWeight / forecastHours);
        }

        int totalDayEvents = dayOfWeekCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalDayEvents > 0) {
            LocalDate today = LocalDate.now();
            DayOfWeek todayDow = today.getDayOfWeek();

            int todayEvents = dayOfWeekCountMap.getOrDefault(todayDow, 0);
            double avgDayEvents = totalDayEvents / 7.0;
            double dayRatio = todayEvents / avgDayEvents;

            factor *= Math.max(0.7, Math.min(dayRatio, 1.8));
        }

        int totalMonthEvents = monthCountMap.values().stream().mapToInt(Integer::intValue).sum();
        if (totalMonthEvents > 0) {
            Month currentMonth = LocalDate.now().getMonth();
            int monthEvents = monthCountMap.getOrDefault(currentMonth, 0);
            double avgMonthEvents = totalMonthEvents / 12.0;
            double monthRatio = monthEvents / avgMonthEvents;

            factor *= Math.max(0.8, Math.min(monthRatio, 1.6));
        }

        return Math.max(0.6, Math.min(factor, 2.5));
    }

    private WeatherInfo getCurrentWeather(GridInfo grid) {
        if (!weatherEnabled) {
            return createDefaultWeather();
        }

        if (weatherMock) {
            return createMockWeather(grid);
        }

        try {
            String location = resolveWeatherLocation(grid);
            if (location == null) {
                return createDefaultWeather();
            }

            String url = weatherBaseUrl + "/weather/now?location=" + location + "&key=" + weatherApiKey;
            HttpResponse response = HttpRequest.get(url)
                    .timeout(weatherTimeout)
                    .execute();

            if (response.isOk()) {
                JSONObject json = JSONUtil.parseObj(response.body());
                if ("200".equals(json.getStr("code")) && json.getJSONObject("now") != null) {
                    JSONObject now = json.getJSONObject("now");
                    WeatherInfo info = new WeatherInfo();
                    info.setText(now.getStr("text"));
                    info.setTemp(now.getDouble("temp"));
                    info.setWindDir(now.getStr("windDir"));
                    info.setWindScale(now.getStr("windScale"));
                    info.setCategory(classifyWeatherCategory(info.getText(), info.getTemp()));
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("获取真实天气失败，使用历史数据模拟，gridId: {}", grid.getId(), e);
        }

        return createMockWeather(grid);
    }

    private double calculateWeatherFactor(GridInfo grid) {
        WeatherInfo weather = getCurrentWeather(grid);
        double categoryFactor = getWeatherCategoryFactor(weather.getCategory());

        double tempFactor = 1.0;
        if (weather.getTemp() != null) {
            double temp = weather.getTemp();
            if (temp >= 35) tempFactor = 1.3;
            else if (temp >= 30) tempFactor = 1.15;
            else if (temp <= 0) tempFactor = 1.2;
            else if (temp <= -10) tempFactor = 1.4;
        }

        return categoryFactor * tempFactor;
    }

    private double calculateHolidayFactor() {
        double factor = 1.0;
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        if ((month == 1 && day >= 20 && day <= 28) ||
            (month == 5 && day >= 1 && day <= 5) ||
            (month == 10 && day >= 1 && day <= 7) ||
            (month == 2 && day >= 10 && day <= 17) ||
            (month == 4 && day >= 4 && day <= 6) ||
            (month == 6 && day >= 7 && day <= 9) ||
            (month == 9 && day >= 29 && day <= 30)) {
            factor *= 1.5;
        }

        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            factor *= 1.25;
        }

        return factor;
    }

    private double calculateEventTypeFactor(Map<String, Integer> typeCountMap, int totalEvents) {
        if (totalEvents == 0) return 1.0;

        double highRiskWeight = 0.0;
        String[] highRiskTypes = {"safety_hazard", "security", "dispute", "traffic"};

        for (String type : highRiskTypes) {
            int count = typeCountMap.getOrDefault(type, 0);
            double ratio = count / (double) totalEvents;
            highRiskWeight += ratio;
        }

        return 1.0 + (highRiskWeight * 0.5);
    }

    private double calculateRecurrenceFactor(Long gridId, int totalEvents) {
        if (totalEvents == 0) return 1.0;

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, gridId)
                .eq(EventInfo::getIsHighRecurrence, 1);
        int recurrenceCount = Math.toIntExact(eventInfoMapper.selectCount(wrapper));

        double recurrenceRatio = recurrenceCount / (double) totalEvents;
        return 1.0 + (recurrenceRatio * 1.5);
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

    private List<EventTypeForecastVO> calculateEventTypeForecasts(
            Map<String, Integer> typeCountMap,
            Map<Integer, Integer> hourCountMap,
            Map<DayOfWeek, Integer> dayOfWeekCountMap,
            int totalEvents,
            int hours,
            double weatherFactor) {

        List<EventTypeForecastVO> result = new ArrayList<>();

        if (totalEvents == 0) {
            for (int i = 0; i < EVENT_TYPES.length; i++) {
                EventTypeForecastVO vo = new EventTypeForecastVO();
                vo.setEventType(EVENT_TYPES[i]);
                vo.setEventTypeName(EVENT_TYPE_NAMES.get(EVENT_TYPES[i]));
                vo.setProbability(BigDecimal.valueOf(100.0 / EVENT_TYPES.length).setScale(2, RoundingMode.HALF_UP).doubleValue());
                vo.setPredictedCount(0);
                vo.setRank(i + 1);
                vo.setTrend("stable");
                result.add(vo);
            }
            return result;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        DayOfWeek currentDay = now.getDayOfWeek();

        int totalHourEvents = hourCountMap.values().stream().mapToInt(Integer::intValue).sum();
        int totalDayEvents = dayOfWeekCountMap.values().stream().mapToInt(Integer::intValue).sum();

        double hourWeight = totalHourEvents > 0
                ? hourCountMap.getOrDefault(currentHour, 0) / Math.max(1.0, totalHourEvents / 24.0)
                : 1.0;
        double dayWeight = totalDayEvents > 0
                ? dayOfWeekCountMap.getOrDefault(currentDay, 0) / Math.max(1.0, totalDayEvents / 7.0)
                : 1.0;

        String weatherCategory = classifyWeatherCategory(getCurrentWeather(null).getText(), null);

        for (String eventType : EVENT_TYPES) {
            int count = typeCountMap.getOrDefault(eventType, 0);
            double baseProbability = (count * 100.0) / totalEvents;

            double weatherWeightKey = weatherCategory + "_" + eventType;
            double eventWeatherFactor = WEATHER_EVENT_WEIGHTS.getOrDefault(weatherWeightKey, 1.0);

            double adjustedProbability = baseProbability * eventWeatherFactor * Math.min(hourWeight, 1.5) * Math.min(dayWeight, 1.5);

            double avgPerDay = count / (double) HISTORY_DAYS;
            double predictedCount = avgPerDay * (hours / 24.0) * hourWeight * dayWeight * eventWeatherFactor;

            int last30DaysCount = getEventsCountInLastDays(typeCountMap, eventType, 30, typeCountMap);
            int last90DaysCount = getEventsCountInLastDays(typeCountMap, eventType, 90, typeCountMap);
            String trend = calculateTrend(count, last30DaysCount, last90DaysCount);

            EventTypeForecastVO vo = new EventTypeForecastVO();
            vo.setEventType(eventType);
            vo.setEventTypeName(EVENT_TYPE_NAMES.get(eventType));
            vo.setProbability(BigDecimal.valueOf(adjustedProbability).setScale(2, RoundingMode.HALF_UP).doubleValue());
            vo.setPredictedCount(Math.max(0, (int) Math.ceil(predictedCount)));
            vo.setTrend(trend);
            result.add(vo);
        }

        double totalProbability = result.stream().mapToDouble(EventTypeForecastVO::getProbability).sum();
        if (totalProbability > 0) {
            for (EventTypeForecastVO vo : result) {
                vo.setProbability(BigDecimal.valueOf(vo.getProbability() * 100.0 / totalProbability)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue());
            }
        }

        result.sort(Comparator.comparingDouble(EventTypeForecastVO::getProbability).reversed());
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    private int getEventsCountInLastDays(Map<String, Integer> totalTypeCount, String eventType, int days,
                                         Map<String, Integer> typeCountMap) {
        int total = typeCountMap.getOrDefault(eventType, 0);
        return (int) (total * (days / (double) HISTORY_DAYS));
    }

    private String calculateTrend(int totalCount, int last30Count, int last90Count) {
        if (last30Count == 0 && last90Count == 0) return "stable";

        double avg30 = last30Count / 30.0;
        double avg90 = last90Count / 90.0;

        if (avg90 == 0) return "stable";

        double changeRate = (avg30 - avg90) / avg90;

        if (changeRate > 0.2) return "up";
        else if (changeRate < -0.2) return "down";
        else return "stable";
    }

    private int predictFutureEventCount(GridInfo grid, LocalDate date) {
        List<EventInfo> events = getHistoricalEvents(grid.getId(), HISTORY_DAYS);
        if (events.isEmpty()) return 0;

        double avgPerDay = events.size() / (double) HISTORY_DAYS;

        DayOfWeek targetDow = date.getDayOfWeek();
        Month targetMonth = date.getMonth();

        Map<DayOfWeek, Integer> dowMap = new HashMap<>();
        Map<Month, Integer> monthMap = new HashMap<>();

        for (EventInfo e : events) {
            LocalDateTime time = e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now();
            dowMap.merge(time.getDayOfWeek(), 1, Integer::sum);
            monthMap.merge(time.getMonth(), 1, Integer::sum);
        }

        double totalDow = dowMap.values().stream().mapToInt(Integer::intValue).sum();
        double dowFactor = totalDow > 0
                ? (dowMap.getOrDefault(targetDow, 0) / (totalDow / 7.0))
                : 1.0;

        double totalMonth = monthMap.values().stream().mapToInt(Integer::intValue).sum();
        double monthFactor = totalMonth > 0
                ? (monthMap.getOrDefault(targetMonth, 0) / (totalMonth / 12.0))
                : 1.0;

        double predicted = avgPerDay * Math.min(dowFactor, 2.0) * Math.min(monthFactor, 2.0);

        DayOfWeek todayDow = LocalDate.now().getDayOfWeek();
        long daysAway = Math.abs(date.toEpochDay() - LocalDate.now().toEpochDay());
        double decayFactor = 1.0 - (daysAway * 0.01);

        return (int) Math.ceil(predicted * Math.max(decayFactor, 0.6));
    }

    private int calculateFutureHeatValue(GridInfo grid, LocalDate date, int eventCount) {
        double base = eventCount * 15.0;

        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            base *= 1.15;
        }

        Month month = date.getMonth();
        if (month == Month.JULY || month == Month.AUGUST) {
            base *= 1.15;
        } else if (month == Month.JANUARY || month == Month.FEBRUARY) {
            base *= 1.1;
        }

        long daysAway = Math.abs(date.toEpochDay() - LocalDate.now().toEpochDay());
        double confidenceFactor = 1.0 - (daysAway * 0.005);
        base *= Math.max(confidenceFactor, 0.7);

        return (int) Math.min(base, 100);
    }

    private int getHistoricalEventCountByDate(Long gridId, LocalDate date) {
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getGridId, gridId)
                .ge(EventInfo::getCreatedAt, date.atStartOfDay())
                .lt(EventInfo::getCreatedAt, date.plusDays(1).atStartOfDay());
        return Math.toIntExact(eventInfoMapper.selectCount(wrapper));
    }

    private int calculateHistoricalHeatValue(Long gridId, LocalDate date, int eventCount) {
        int base = eventCount * 15;

        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            base += 5;
        }

        Month month = date.getMonth();
        if (month == Month.JULY || month == Month.AUGUST) {
            base += 8;
        }

        LambdaQueryWrapper<EventInfo> highRiskWrapper = new LambdaQueryWrapper<>();
        highRiskWrapper.eq(EventInfo::getGridId, gridId)
                .ge(EventInfo::getCreatedAt, date.atStartOfDay())
                .lt(EventInfo::getCreatedAt, date.plusDays(1).atStartOfDay())
                .and(w -> w.eq(EventInfo::getEventType, "safety_hazard")
                        .or().eq(EventInfo::getEventType, "security")
                        .or().eq(EventInfo::getPriority, "URGENT"));
        int highRiskCount = Math.toIntExact(eventInfoMapper.selectCount(highRiskWrapper));
        base += highRiskCount * 8;

        return Math.min(base, 100);
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

    private List<EventTypeForecastVO> getTopEventTypesForDate(Long gridId, LocalDate date, boolean isFuture, int topN) {
        List<EventTypeForecastVO> result = new ArrayList<>();
        Map<String, Integer> typeCountMap;

        if (isFuture) {
            List<EventInfo> events = getHistoricalEvents(gridId, HISTORY_DAYS);
            typeCountMap = new HashMap<>();
            for (EventInfo e : events) {
                if (e.getEventType() != null) {
                    typeCountMap.merge(e.getEventType(), 1, Integer::sum);
                }
            }
        } else {
            List<EventInfo> events = getEventsByGridAndDate(gridId, date);
            typeCountMap = new HashMap<>();
            for (EventInfo e : events) {
                if (e.getEventType() != null) {
                    typeCountMap.merge(e.getEventType(), 1, Integer::sum);
                }
            }
        }

        int total = typeCountMap.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : typeCountMap.entrySet()) {
            EventTypeForecastVO vo = new EventTypeForecastVO();
            vo.setEventType(entry.getKey());
            vo.setEventTypeName(EVENT_TYPE_NAMES.getOrDefault(entry.getKey(), entry.getKey()));
            vo.setPredictedCount(entry.getValue());
            vo.setProbability(total > 0 ? BigDecimal.valueOf((entry.getValue() * 100.0) / total)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0);
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

    private WeatherInfo createDefaultWeather() {
        WeatherInfo info = new WeatherInfo();
        info.setText("晴");
        info.setTemp(25.0);
        info.setCategory("sunny");
        return info;
    }

    private WeatherInfo createMockWeather(GridInfo grid) {
        List<EventInfo> events = getHistoricalEvents(grid != null ? grid.getId() : 0L, 30);
        WeatherInfo info = new WeatherInfo();

        int summerEvents = (int) events.stream()
                .filter(e -> {
                    LocalDateTime t = e.getCreatedAt();
                    return t != null && (t.getMonth() == Month.JULY || t.getMonth() == Month.AUGUST);
                }).count();

        int rainEvents = (int) events.stream()
                .filter(e -> "facility".equals(e.getEventType()) || "traffic".equals(e.getEventType()))
                .count();

        double avgEvents = events.size() / 30.0;
        double temp = 15.0 + (summerEvents / Math.max(1.0, events.size())) * 20.0;

        if (rainEvents > avgEvents * 0.4) {
            info.setText("小雨");
            info.setCategory("rainy");
        } else if (temp >= 32) {
            info.setText("晴转多云");
            info.setCategory("hot");
        } else if (temp >= 20) {
            info.setText("多云");
            info.setCategory("cloudy");
        } else if (temp <= 0) {
            info.setText("小雪");
            info.setCategory("snow");
        } else {
            info.setText("晴");
            info.setCategory("sunny");
        }
        info.setTemp(BigDecimal.valueOf(temp).setScale(1, RoundingMode.HALF_UP).doubleValue());

        return info;
    }

    private String resolveWeatherLocation(GridInfo grid) {
        if (grid == null || grid.getLng() == null || grid.getLat() == null) {
            return null;
        }
        return grid.getLng().toString() + "," + grid.getLat().toString();
    }

    private double getWeatherCategoryFactor(String category) {
        switch (category) {
            case "sunny":
                return 0.9;
            case "cloudy":
                return 1.0;
            case "rainy":
                return 1.4;
            case "storm":
                return 1.9;
            case "snow":
                return 1.7;
            case "hot":
                return 1.25;
            case "cold":
                return 1.2;
            case "fog":
                return 1.3;
            default:
                return 1.0;
        }
    }

    private String classifyWeatherCategory(String weatherText, Double temp) {
        if (weatherText == null) return "sunny";
        String text = weatherText.toLowerCase();

        if (text.contains("暴") || text.contains("雷")) return "storm";
        if (text.contains("雪")) return "snow";
        if (text.contains("雨")) return "rainy";
        if (text.contains("雾") || text.contains("霾")) return "fog";
        if (text.contains("阴") || text.contains("云")) return "cloudy";

        if (temp != null) {
            if (temp >= 33) return "hot";
            if (temp <= 0) return "cold";
        }

        return "sunny";
    }

    private boolean isTodayHoliday() {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        return (month == 1 && day >= 20 && day <= 28) ||
                (month == 5 && day >= 1 && day <= 5) ||
                (month == 10 && day >= 1 && day <= 7);
    }

    private String generateSuggestion(int heatLevel, List<EventTypeForecastVO> typeForecasts, double heatScore) {
        StringBuilder suggestion = new StringBuilder();

        switch (heatLevel) {
            case HEAT_LEVEL_LOW:
                suggestion.append("当前区域事件风险较低，建议按常规频次开展巡查工作，每日不少于2次常规巡查。");
                break;
            case HEAT_LEVEL_MEDIUM:
                suggestion.append("当前区域存在一定事件风险，建议适当增加巡查频次，每日巡查3-4次，重点关注人流密集区域。");
                break;
            case HEAT_LEVEL_HIGH:
                suggestion.append("当前区域事件风险较高，建议加强巡查力度，每日巡查不少于5次，网格员轮班值守，重点关注高发区域和风险时段。");
                break;
            case HEAT_LEVEL_CRITICAL:
                suggestion.append("【紧急预警】当前区域事件风险极高，请立即启动应急预案，增派网格员加强防控，实行全天候巡查机制，重点风险区域安排专人定点值守！");
                break;
            default:
                break;
        }

        if (heatScore >= 70) {
            suggestion.append("建议提前准备应急物资，与相关部门做好联动准备。");
        }

        if (typeForecasts != null && !typeForecasts.isEmpty()) {
            suggestion.append(" 重点关注事件类型：");
            List<String> topTypes = typeForecasts.stream()
                    .limit(3)
                    .map(f -> f.getEventTypeName() + "(" + f.getProbability() + "%)")
                    .collect(Collectors.toList());
            suggestion.append(String.join("、", topTypes));
            suggestion.append("。");

            List<String> highRiskSuggestions = new ArrayList<>();
            for (EventTypeForecastVO f : typeForecasts) {
                if ("safety_hazard".equals(f.getEventType()) && f.getProbability() > 15) {
                    highRiskSuggestions.add("重点排查安全隐患点");
                }
                if ("security".equals(f.getEventType()) && f.getProbability() > 15) {
                    highRiskSuggestions.add("加强治安巡逻");
                }
                if ("traffic".equals(f.getEventType()) && f.getProbability() > 15) {
                    highRiskSuggestions.add("关注交通拥堵节点疏导");
                }
                if ("dispute".equals(f.getEventType()) && f.getProbability() > 15) {
                    highRiskSuggestions.add("提前介入矛盾调解");
                }
                if ("facility".equals(f.getEventType()) && f.getProbability() > 15) {
                    highRiskSuggestions.add("检查市政设施完好性");
                }
            }
            if (!highRiskSuggestions.isEmpty()) {
                suggestion.append(" 专项建议：").append(String.join("；", highRiskSuggestions)).append("。");
            }
        }

        return suggestion.toString();
    }

    private String buildWarningContent(GridInfo grid, EventHeatForecastVO forecast) {
        StringBuilder content = new StringBuilder();
        content.append("【网格事件预警通知】\n");
        content.append("网格名称：").append(grid.getGridName()).append("\n");
        content.append("预警等级：").append(forecast.getHeatLevelDesc()).append("（评分：").append(forecast.getHeatScore()).append("）\n");
        content.append("预测时段：未来24小时\n");
        content.append("预计事件数：").append(forecast.getPredictedEventCount()).append(" 件\n");
        content.append("天气状况：").append(forecast.getWeatherCondition()).append("\n");
        content.append("是否节假日：").append(forecast.getIsHoliday() ? "是" : "否").append("\n\n");
        content.append("巡查建议：\n").append(forecast.getSuggestion());
        return content.toString();
    }

    static class WeatherInfo {
        private String text;
        private Double temp;
        private String windDir;
        private String windScale;
        private String category;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Double getTemp() { return temp; }
        public void setTemp(Double temp) { this.temp = temp; }
        public String getWindDir() { return windDir; }
        public void setWindDir(String windDir) { this.windDir = windDir; }
        public String getWindScale() { return windScale; }
        public void setWindScale(String windScale) { this.windScale = windScale; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
