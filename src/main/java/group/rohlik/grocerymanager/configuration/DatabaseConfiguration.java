package group.rohlik.grocerymanager.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Tomas Kramec
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
public class DatabaseConfiguration {
}
