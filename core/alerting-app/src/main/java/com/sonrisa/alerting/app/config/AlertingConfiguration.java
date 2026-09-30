package com.sonrisa.alerting.app.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the application's configuration groups and derived beans. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        ScheduleProperties.class,
        DeliveryProperties.class,
        RetentionProperties.class,
        AlertingSecurityProperties.class,
        AlertingManagementProperties.class})
public class AlertingConfiguration {

    /**
     * The one configured zone (default Europe/Budapest). Scheduling and the rendering of times in
     * messages use this bean; stored times stay UTC instants (Section 13.2).
     */
    @Bean
    ZoneId alertingZone(ScheduleProperties schedule) {
        return schedule.zone();
    }
}
