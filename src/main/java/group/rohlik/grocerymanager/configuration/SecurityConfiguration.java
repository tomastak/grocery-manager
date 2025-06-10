package group.rohlik.grocerymanager.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Optional;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "spring.security", value = "provider", havingValue = "basic")
    @RequiredArgsConstructor
    public static class BasicAuthenticationWebSecurityConfigurerAdapter {

        private final SecurityProperties securityProperties;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService userDetailsService,
                                                       CorsConfigurationSource corsConfigurationSource) throws Exception {
            log.info("Custom security provider: Basic");
            configureWebSecurityConfigurerAdapter(http, corsConfigurationSource);
            http.userDetailsService(userDetailsService);
            return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        protected UserDetailsService userDetailsService() {
            var manager = new InMemoryUserDetailsManager();
            var encodedPassword = passwordEncoder().encode(securityProperties.getUser().getPassword());
            manager.createUser(User.withUsername(securityProperties.getUser().getName()).password(encodedPassword)
                    .authorities(securityProperties.getUser().getRoles().toArray(new String[0])).build());
            return manager;
        }

        @Bean
        public AuditorAware<String> getAuditor() {
            return () -> {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    return Optional.of(((UserDetails) authentication.getPrincipal()).getUsername());
                }
                return Optional.empty();
            };
        }
    }

    private static void configureWebSecurityConfigurerAdapter(final HttpSecurity http,
                                                              final CorsConfigurationSource corsSource) throws Exception {
        http
                .headers(headersConfigurer -> headersConfigurer.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher("/console/**"),
                                antMatcher("/management/health"),
                                antMatcher("/management/prometheus"),
                                antMatcher("/swagger-ui/**"),
                                antMatcher("/v3/api-docs/**"),
                                antMatcher("/swagger-ui.html"),
                                antMatcher("/"),
                                /**
                                 * Spring Boot uses /error mapping to handle all exceptions thrown during processing of requests.
                                 * To preserve the original response code, we need to permit this mapping.
                                 * Otherwise, Spring Security will return 401 Unauthorized for all exceptions.
                                 * See https://github.com/spring-projects/spring-security/issues/12771 for details.
                                  */
                                antMatcher("/error")).permitAll()
                        .requestMatchers(regexMatcher("/management/health(?:/liveness)?(?:/readiness)?")).permitAll()
                        .requestMatchers(antMatcher("/management/**")).hasAuthority("GM_ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().hasAuthority("GM_USER")
                )
                .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.ignoringRequestMatchers(new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON,
                        new MediaType("application", "hal+json"))))
                .cors(cors -> cors.configurationSource(corsSource))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(httpSecuritySessionManagementConfigurer -> httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern(CorsConfiguration.ALL);
        configuration.setAllowedMethods(Arrays.asList(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),HttpMethod.PATCH.name(), HttpMethod.OPTIONS.name()));
        configuration.addAllowedHeader(CorsConfiguration.ALL);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



}