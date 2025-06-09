package group.rohlik.grocerymanager.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Tomas Kramec
 */
@ConfigurationProperties(prefix = "grocery-manager.db-transaction-retry")
@Getter
@Setter
public class DatabaseTransactionRetryProperties {
    private int maxAttempts = 3;
    private long initialInterval = 1000; // initial backoff interval in milliseconds
    private long maxInterval = 5000; // maximum backoff interval in milliseconds
    private double multiplier = 1.5; // backoff multiplier
    private  Class<? extends Throwable>[] retryableExceptions;
}
