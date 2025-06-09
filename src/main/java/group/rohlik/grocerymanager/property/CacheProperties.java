package group.rohlik.grocerymanager.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cache")
@Getter
@Setter
public class CacheProperties {
    private int ttlMinutes = 0;
    private int maxSize = 100;
}