package org.djar.football.match.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Object> handleNotFound(NotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ex.getMessage(), new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({InvalidRequestExeption.class})
    public ResponseEntity<Object> handleValidation(Exception ex, WebRequest request) {
        return new ResponseEntity<>(ex.getMessage(), new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
}