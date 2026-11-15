package br.edu.estoque.api;

import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErrosApi {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> entrada(IllegalArgumentException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> conflito(IllegalStateException e) {
        return erro(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String,String>> inexistente(NoSuchElementException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> jsonInvalido() {
        return erro(HttpStatus.BAD_REQUEST, "Confira os campos e os tipos de dados enviados.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String,String>> parametro(MethodArgumentTypeMismatchException e) {
        return erro(HttpStatus.BAD_REQUEST, "Valor inválido para " + e.getName() + ".");
    }

    private ResponseEntity<Map<String,String>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("mensagem", mensagem));
    }
}
