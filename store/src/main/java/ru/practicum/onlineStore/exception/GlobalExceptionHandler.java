package ru.practicum.onlineStore.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.sql.SQLException;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<String> handleNotFound(ResponseStatusException ex, Model model) {
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            log.error("Страница не найдена: ", ex);
            model.addAttribute("errorTitle", "Страница не найдена");
            model.addAttribute("errorMessage", "Упс! Такой страницы не существует.");
            return Mono.just("error/error");
        }
        return Mono.error(ex);
    }

    @ExceptionHandler(SQLException.class)
    public Mono<String> handleDatabaseError(SQLException ex, Model model) {
        log.error("Ошибка базы данных: ", ex);
        model.addAttribute("errorTitle", "Ошибка базы данных");
        model.addAttribute("errorMessage", "Произошла проблема при обращении к базе. Попробуйте позже.");
        return Mono.just("error/error");
    }

    @ExceptionHandler(Exception.class)
    public Mono<String> handleGeneralError(Exception ex, Model model) {
        log.error("Внутренняя ошибка: ", ex);
        model.addAttribute("errorTitle", "Внутренняя ошибка");
        model.addAttribute("errorMessage", "Что-то пошло не так. Мы уже работаем над этим!");
        return Mono.just("error/error");
    }

}
