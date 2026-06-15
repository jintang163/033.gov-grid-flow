package com.gov.grid.config;

import com.gov.grid.quartz.EventUrgeJob;
import com.gov.grid.quartz.EventHeatWarningJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Value("${urge.cron:0 */5 * * * ?}")
    private String urgeCron;

    @Value("${warning.cron:0 0 8,12,18 * * ?}")
    private String warningCron;

    @Bean
    public JobDetail eventUrgeJobDetail() {
        return JobBuilder.newJob(EventUrgeJob.class)
                .withIdentity("eventUrgeJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger eventUrgeTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(urgeCron);
        return TriggerBuilder.newTrigger()
                .forJob(eventUrgeJobDetail())
                .withIdentity("eventUrgeTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }

    @Bean
    public JobDetail eventHeatWarningJobDetail() {
        return JobBuilder.newJob(EventHeatWarningJob.class)
                .withIdentity("eventHeatWarningJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger eventHeatWarningTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(warningCron);
        return TriggerBuilder.newTrigger()
                .forJob(eventHeatWarningJobDetail())
                .withIdentity("eventHeatWarningTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
