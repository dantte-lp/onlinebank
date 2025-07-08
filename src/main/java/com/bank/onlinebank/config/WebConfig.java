package com.bank.onlinebank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Конфигурация веб-компонентов Spring MVC
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${app.cors.max-age:3600}")
    private long corsMaxAge;

    /**
     * Настройка обработки trailing slash
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }

    /**
     * Настройка CORS для API endpoints
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*") // Используем patterns для поддержки credentials
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Настройка статических ресурсов
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS файлы
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .resourceChain(true);

        // JavaScript файлы
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .resourceChain(true);

        // Изображения
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true);

        // Font Awesome
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .resourceChain(true);

        // Favicon
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
    }

    /**
     * Настройка View Controllers для простых страниц
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/error").setViewName("error/500");
        registry.addViewController("/404").setViewName("error/404");
    }

    /**
     * Настройка форматирования дат
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        registrar.registerFormatters(registry);
    }

    /**
     * Настройка интерцепторов
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Интерцептор для смены локали
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        registry.addInterceptor(localeChangeInterceptor);

        // Интерцептор для логирования
        registry.addInterceptor(new RequestLoggingInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health/**");

        // Интерцептор для проверки доступности БД
        registry.addInterceptor(new DatabaseAvailabilityInterceptor())
                .addPathPatterns("/clients/**", "/api/clients/**")
                .excludePathPatterns("/api/health/**", "/", "/error/**");
    }

    /**
     * Настройка обработки аргументов методов контроллеров
     */
    @Override
    public void addArgumentResolvers(java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        // Можно добавить кастомные резолверы для аргументов
    }

    /**
     * Настройка путей для контент-негоциации
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(true)
                .parameterName("format")
                .ignoreAcceptHeader(false)
                .defaultContentType(org.springframework.http.MediaType.TEXT_HTML)
                .mediaType("json", org.springframework.http.MediaType.APPLICATION_JSON)
                .mediaType("xml", org.springframework.http.MediaType.APPLICATION_XML)
                .mediaType("html", org.springframework.http.MediaType.TEXT_HTML);
    }

    /**
     * Интерцептор для логирования запросов
     */
    private static class RequestLoggingInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        private static final java.util.logging.Logger logger =
                java.util.logging.Logger.getLogger(RequestLoggingInterceptor.class.getName());

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 Object handler) throws Exception {
            long startTime = System.currentTimeMillis();
            request.setAttribute("startTime", startTime);

            logger.info(String.format("Request: %s %s from %s",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr()));

            return true;
        }

        @Override
        public void afterCompletion(jakarta.servlet.http.HttpServletRequest request,
                                    jakarta.servlet.http.HttpServletResponse response,
                                    Object handler, Exception ex) throws Exception {
            long startTime = (Long) request.getAttribute("startTime");
            long endTime = System.currentTimeMillis();
            long executeTime = endTime - startTime;

            logger.info(String.format("Response: %s %s - Status: %d - Time: %dms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    executeTime));
        }
    }

    /**
     * Интерцептор для проверки доступности БД
     */
    private static class DatabaseAvailabilityInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response,
                                 Object handler) throws Exception {
            // Проверка доступности БД будет реализована через DatabaseHealthIndicator
            return true;
        }
    }
}