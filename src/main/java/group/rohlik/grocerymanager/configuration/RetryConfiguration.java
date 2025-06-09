package group.rohlik.grocerymanager.configuration;

import group.rohlik.grocerymanager.property.DatabaseTransactionRetryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tomas Kramec
 */
@EnableRetry
@Configuration
public class RetryConfiguration {

    @Bean
    public RetryOperationsInterceptor productServiceRetryInterceptor(DatabaseTransactionRetryProperties retryProperties) {
        final Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        if (retryProperties.getRetryableExceptions() != null) {
            for (Class<? extends Throwable> clazz : retryProperties.getRetryableExceptions()) {
                retryableExceptions.put(clazz, true);
            }
        }

        return RetryInterceptorBuilder
                .stateless()
                .retryPolicy(new SimpleRetryPolicy(retryProperties.getMaxAttempts(), retryableExceptions, true))
                .backOffOptions(retryProperties.getInitialInterval(), retryProperties.getMultiplier(), retryProperties.getMaxInterval())
                .build();
    }
}
