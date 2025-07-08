package com.bank.onlinebank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

/**
 * Конфигурация шаблонизатора Thymeleaf.
 *
 * <p>Эта конфигурация настраивает основные компоненты, необходимые для интеграции
 * Thymeleaf со Spring Boot. Она определяет, где находятся HTML-шаблоны,
 * как они должны обрабатываться и как будут рендериться представления (view)
 * в веб-приложении.</p>
 *
 * <p>В этой версии мы отказались от кастомных диалектов для форматирования данных.
 * Вместо этого, вся логика форматирования (например, для номеров счетов или телефонов)
 * инкапсулирована непосредственно в DTO-классах. Это упрощает код, повышает его
 * читаемость и облегчает поддержку, так как вся логика представления данных
 * находится в одном месте.</p>
 */
@Configuration
public class ThymeleafConfig {

    /**
     * Настраивает резолвер шаблонов.
     * <p>Этот бин указывает Spring, где искать HTML-шаблоны (в classpath:/templates/),
     * какое у них расширение (.html) и в какой кодировке они сохранены (UTF-8).
     * Кэширование отключается для удобства разработки, чтобы изменения в шаблонах
     * применялись без перезапуска приложения. В production-среде кэширование
     * рекомендуется включить для повышения производительности.</p>
     *
     * @return настроенный резолвер шаблонов.
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateResolver.setCacheable(false); // Для разработки. В проде лучше true.
        return templateResolver;
    }

    /**
     * Настраивает движок шаблонов Thymeleaf.
     * <p>Движок использует резолвер для поиска и обработки шаблонов.
     * Он также может быть настроен для использования дополнительных диалектов,
     * но в данной конфигурации мы используем только стандартные возможности Spring.</p>
     *
     * @return настроенный движок шаблонов.
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    /**
     * Настраивает резолвер представлений Thymeleaf.
     * <p>Этот бин интегрирует Thymeleaf в механизм представлений Spring MVC.
     * Он указывает, что для рендеринга view будет использоваться движок Thymeleaf,
     * и устанавливает кодировку ответа.</p>
     *
     * @return настроенный резолвер представлений.
     */
    @Bean
    public ThymeleafViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        return viewResolver;
    }
}