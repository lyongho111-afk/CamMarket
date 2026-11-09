package com.campus.remarket.controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
@ControllerAdvice public class ErrorControllerAdvice {
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String invalid(RuntimeException error,Model model){model.addAttribute("message",error.getMessage());return "error";}
}
