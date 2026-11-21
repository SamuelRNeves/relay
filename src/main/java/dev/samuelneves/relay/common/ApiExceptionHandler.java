package dev.samuelneves.relay.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Padroniza as respostas de erro da API no formato Problem Details (RFC 9457).
 * Herdando de ResponseEntityExceptionHandler, erros comuns do Spring MVC
 * (JSON malformado, metodo nao suportado, rota inexistente...) ja saem nesse formato.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Falha nas anotacoes de validacao (@NotBlank, @URL...) de um corpo com @Valid. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toViolation)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.");
        problem.setTitle("Requisição inválida");
        problem.setProperty("errors", errors);

        // handleExceptionInternal completa o problema (ex.: "instance" com a rota chamada)
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    /** Um campo que falhou e o motivo. */
    public record FieldViolation(String field, String message) {
    }
}
