package group.rohlik.grocerymanager.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import group.rohlik.grocerymanager.property.CacheProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        var caffeineBuilder = Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.getTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(cacheProperties.getMaxSize())
                .recordStats();
        var cacheManager = new CaffeineCacheManager("product", "order");
        cacheManager.setCaffeine(caffeineBuilder);
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

}