package com.bank.onlinebank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

/**
 * Конфигурация Thymeleaf шаблонизатора
 */
@Configuration
public class ThymeleafConfig {

    @Value("${thymeleaf.cache:false}")
    private boolean cache;

    @Value("${thymeleaf.check-template:true}")
    private boolean checkTemplate;

    @Value("${thymeleaf.check-template-location:true}")
    private boolean checkTemplateLocation;

    private final ApplicationContext applicationContext;

    public ThymeleafConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Настройка резолвера шаблонов
     */
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateResolver.setCacheable(cache);
        templateResolver.setCheckExistence(checkTemplate);

        return templateResolver;
    }

    /**
     * Настройка движка шаблонов
     */
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        templateEngine.setEnableSpringELCompiler(true);

        // Добавляем диалект для работы с Java 8 Time API
        templateEngine.addDialect(new Java8TimeDialect());

        // Добавляем кастомный диалект для утилит
        templateEngine.addDialect(new BankUtilityDialect());

        return templateEngine;
    }

    /**
     * Настройка ViewResolver
     */
    @Bean
    public ViewResolver viewResolver() {
        ThymeleafViewResolver resolver = new ThymeleafViewResolver();
        resolver.setTemplateEngine(templateEngine());
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setOrder(1);
        resolver.setViewNames(new String[]{"*"});
        resolver.setCache(cache);

        return resolver;
    }

    /**
     * Кастомный диалект для банковских утилит
     */
    public static class BankUtilityDialect extends org.thymeleaf.dialect.AbstractProcessorDialect {

        private static final String PREFIX = "bank";

        public BankUtilityDialect() {
            super("Bank Utility Dialect", PREFIX, 1000);
        }

        @Override
        public java.util.Set<org.thymeleaf.processor.IProcessor> getProcessors(String dialectPrefix) {
            java.util.Set<org.thymeleaf.processor.IProcessor> processors = new java.util.HashSet<>();

            // Процессор для форматирования номера счета
            processors.add(new AccountNumberProcessor(dialectPrefix));

            // Процессор для форматирования телефона
            processors.add(new PhoneNumberProcessor(dialectPrefix));

            // Процессор для отображения валюты
            processors.add(new CurrencyProcessor(dialectPrefix));

            return processors;
        }
    }

    /**
     * Процессор для форматирования номера счета
     */
    private static class AccountNumberProcessor extends org.thymeleaf.processor.element.AbstractAttributeTagProcessor {

        private static final String ATTR_NAME = "account";
        private static final int PRECEDENCE = 10000;

        public AccountNumberProcessor(String dialectPrefix) {
            super(
                    TemplateMode.HTML,
                    dialectPrefix,
                    null,
                    false,
                    ATTR_NAME,
                    true,
                    PRECEDENCE,
                    true
            );
        }

        @Override
        protected void doProcess(
                org.thymeleaf.context.ITemplateContext context,
                org.thymeleaf.model.IProcessableElementTag tag,
                org.thymeleaf.engine.AttributeName attributeName,
                String attributeValue,
                org.thymeleaf.processor.element.IElementTagStructureHandler structureHandler) {

            final org.thymeleaf.standard.expression.IStandardExpressionParser parser =
                    org.thymeleaf.standard.expression.StandardExpressions.getExpressionParser(context.getConfiguration());

            final org.thymeleaf.standard.expression.IStandardExpression expression =
                    parser.parseExpression(context, attributeValue);

            final String account = String.valueOf(expression.execute(context));

            if (account != null && account.length() == 20) {
                // Форматируем как XXXX XXXX XXXX XXXX XXXX
                String formatted = account.replaceAll("(.{4})", "$1 ").trim();
                structureHandler.setBody(formatted, false);
            }
        }
    }

    /**
     * Процессор для форматирования номера телефона
     */
    private static class PhoneNumberProcessor extends org.thymeleaf.processor.element.AbstractAttributeTagProcessor {

        private static final String ATTR_NAME = "phone";
        private static final int PRECEDENCE = 10000;

        public PhoneNumberProcessor(String dialectPrefix) {
            super(
                    TemplateMode.HTML,
                    dialectPrefix,
                    null,
                    false,
                    ATTR_NAME,
                    true,
                    PRECEDENCE,
                    true
            );
        }

        @Override
        protected void doProcess(
                org.thymeleaf.context.ITemplateContext context,
                org.thymeleaf.model.IProcessableElementTag tag,
                org.thymeleaf.engine.AttributeName attributeName,
                String attributeValue,
                org.thymeleaf.processor.element.IElementTagStructureHandler structureHandler) {

            final org.thymeleaf.standard.expression.IStandardExpressionParser parser =
                    org.thymeleaf.standard.expression.StandardExpressions.getExpressionParser(context.getConfiguration());

            final org.thymeleaf.standard.expression.IStandardExpression expression =
                    parser.parseExpression(context, attributeValue);

            final String phone = String.valueOf(expression.execute(context));

            if (phone != null && phone.startsWith("+")) {
                // Базовое форматирование для международных номеров
                structureHandler.setBody(phone, false);
            }
        }
    }

    /**
     * Процессор для отображения валюты
     */
    private static class CurrencyProcessor extends org.thymeleaf.processor.element.AbstractAttributeTagProcessor {

        private static final String ATTR_NAME = "currency";
        private static final int PRECEDENCE = 10000;

        public CurrencyProcessor(String dialectPrefix) {
            super(
                    TemplateMode.HTML,
                    dialectPrefix,
                    null,
                    false,
                    ATTR_NAME,
                    true,
                    PRECEDENCE,
                    true
            );
        }

        @Override
        protected void doProcess(
                org.thymeleaf.context.ITemplateContext context,
                org.thymeleaf.model.IProcessableElementTag tag,
                org.thymeleaf.engine.AttributeName attributeName,
                String attributeValue,
                org.thymeleaf.processor.element.IElementTagStructureHandler structureHandler) {

            final org.thymeleaf.standard.expression.IStandardExpressionParser parser =
                    org.thymeleaf.standard.expression.StandardExpressions.getExpressionParser(context.getConfiguration());

            final org.thymeleaf.standard.expression.IStandardExpression expression =
                    parser.parseExpression(context, attributeValue);

            final Object currencyObj = expression.execute(context);

            if (currencyObj != null) {
                String html = "<span class=\"currency-badge\">" + currencyObj + "</span>";
                structureHandler.setBody(html, false);
            }
        }
    }
}