package group.rohlik.grocerymanager.property;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author Tomas Kramec
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "grocery-manager.schedule.order.expire")
public class ExpireOrderScheduleProperties {
    @NotNull
    private Duration threshold;
    @NotNull
    private Duration bottomThreshold;
    @NotNull
    private Integer maxSize;
    @NotNull
    private Integer batchUpdateSize;
    @NotNull
    private Integer maxRetryAttempts;
}
