package com.bank.onlinebank.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Глобальный обработчик исключений для всего приложения
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка исключения ClientNotFoundException
     */
    @ExceptionHandler(ClientNotFoundException.class)
    public Object handleClientNotFound(ClientNotFoundException ex, HttpServletRequest request) {
        log.error("Клиент не найден: {}", ex.getMessage());

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND, ex.getMessage());
            problemDetail.setTitle("Клиент не найден");
            problemDetail.setType(URI.create("/errors/client-not-found"));
            problemDetail.setProperty("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/404");
            mav.addObject("message", ex.getMessage());
            return mav;
        }
    }

    /**
     * Обработка исключения ClientAlreadyExistsException
     */
    @ExceptionHandler(ClientAlreadyExistsException.class)
    public Object handleClientAlreadyExists(ClientAlreadyExistsException ex, HttpServletRequest request) {
        log.error("Клиент уже существует: {}", ex.getMessage());

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, ex.getMessage());
            problemDetail.setTitle("Дублирование данных");
            problemDetail.setType(URI.create("/errors/duplicate-client"));
            problemDetail.setProperty("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/400");
            mav.addObject("message", ex.getMessage());
            return mav;
        }
    }

    /**
     * Обработка исключения DatabaseConnectionException
     */
    @ExceptionHandler(DatabaseConnectionException.class)
    public Object handleDatabaseConnection(DatabaseConnectionException ex, HttpServletRequest request) {
        log.error("Ошибка подключения к БД: {}", ex.getMessage());

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.SERVICE_UNAVAILABLE, "База данных временно недоступна");
            problemDetail.setTitle("Ошибка базы данных");
            problemDetail.setType(URI.create("/errors/database-unavailable"));
            problemDetail.setProperty("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/503");
            mav.addObject("message", "База данных временно недоступна. Пожалуйста, попробуйте позже.");
            return mav;
        }
    }

    @ExceptionHandler(DatabaseUnavailableException.class)
    public ProblemDetail handleDatabaseUnavailableException(DatabaseUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Сервис временно недоступен");
        problemDetail.setProperty("timestamp", java.time.Instant.now());
        return problemDetail;
    }

    /**
     * Обработка ошибок валидации
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, replacement) -> existing
                ));

        log.warn("Ошибки валидации: {}", errors);

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, "Ошибка валидации данных");
            problemDetail.setTitle("Некорректные данные");
            problemDetail.setType(URI.create("/errors/validation-failed"));
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("errors", errors);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/400");
            mav.addObject("message", "Проверьте правильность заполнения формы");
            mav.addObject("errors", errors);
            return mav;
        }
    }

    /**
     * Обработка 404 ошибок
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Страница не найдена: {}", ex.getRequestURL());

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND, "Запрашиваемый ресурс не найден");
            problemDetail.setTitle("Ресурс не найден");
            problemDetail.setType(URI.create("/errors/not-found"));
            problemDetail.setProperty("timestamp", Instant.now());
            problemDetail.setProperty("path", ex.getRequestURL());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/404");
            mav.addObject("message", "Запрашиваемая страница не найдена");
            return mav;
        }
    }

    /**
     * Обработка IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Некорректный аргумент: {}", ex.getMessage());

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.BAD_REQUEST, ex.getMessage());
            problemDetail.setTitle("Некорректный запрос");
            problemDetail.setType(URI.create("/errors/bad-request"));
            problemDetail.setProperty("timestamp", Instant.now());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/400");
            mav.addObject("message", "Некорректный запрос: " + ex.getMessage());
            return mav;
        }
    }

    /**
     * Обработка всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public Object handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Непредвиденная ошибка", ex);

        if (isApiRequest(request)) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Произошла внутренняя ошибка сервера");
            problemDetail.setTitle("Внутренняя ошибка");
            problemDetail.setType(URI.create("/errors/internal-error"));
            problemDetail.setProperty("timestamp", Instant.now());

            // В production не показываем детали ошибки
            if (!"production".equals(System.getProperty("spring.profiles.active"))) {
                problemDetail.setProperty("debug", ex.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
        } else {
            ModelAndView mav = new ModelAndView("error/500");
            mav.addObject("message", "Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.");
            return mav;
        }
    }

    /**
     * Определяет, является ли запрос API-запросом
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return path.startsWith("/api/") ||
                (accept != null && accept.contains("application/json"));
    }

    /**
     * Создание детального сообщения об ошибке для логирования
     */
    private Map<String, Object> createErrorDetails(HttpServletRequest request, Exception ex) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", Instant.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("exception", ex.getClass().getName());

        return errorDetails;
    }
}