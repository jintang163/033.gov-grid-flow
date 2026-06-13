package com.gov.grid.config;

import com.gov.grid.quartz.EventUrgeJob;
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
}
