package group.rohlik.grocerymanager.configuration;

import group.rohlik.grocerymanager.property.CacheProperties;
import group.rohlik.grocerymanager.property.DatabaseTransactionRetryProperties;
import group.rohlik.grocerymanager.property.ExpireOrderScheduleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        ExpireOrderScheduleProperties.class,
        CacheProperties.class,
        DatabaseTransactionRetryProperties.class
})
public class ApplicationConfiguration {
}
