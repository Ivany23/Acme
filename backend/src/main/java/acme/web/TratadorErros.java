package acme.web;

import acme.servico.AcessoNegadoException;
import acme.servico.NaoAutenticadoException;
import acme.servico.NaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Converte as excepções em respostas JSON claras, sempre no mesmo formato. */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    public record Erro(int status, String erro, String mensagem, Map<String, String> campos, LocalDateTime data) {
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<Erro> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<Erro> naoAutenticado(NaoAutenticadoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Erro> acessoNegado(AcessoNegadoException e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Erro> semRecurso(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "Endereço não encontrado", null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Erro> argumentoInvalido(IllegalArgumentException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Erro> conflito(IllegalStateException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(f.getField(), f.getDefaultMessage());
        }
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Erro> validacaoParametros(HandlerMethodValidationException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Erro> jsonInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo do pedido inválido (verifique o JSON, datas AAAA-MM-DD e horas HH:mm)", null);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Erro> parametroInvalido(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro em falta ou inválido: " + e.getMessage(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Erro> metodo(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT,
                "A operação viola a integridade dos dados (registo duplicado ou ainda em uso)", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Erro> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", null);
    }

    private static ResponseEntity<Erro> resposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new Erro(status.value(), status.getReasonPhrase(), mensagem, campos, LocalDateTime.now()));
    }
}
