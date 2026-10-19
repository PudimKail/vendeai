package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.pedido.PagamentoRecusadoException;
import com.pudimkail.vendeai.produto.NaoEncontradoException;
import com.pudimkail.vendeai.usuario.EmailNaoVerificadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Transforma os erros em JSON {"erro": "mensagem"} para o React mostrar ao usuário. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler({IllegalArgumentException.class, PagamentoRecusadoException.class})
    ResponseEntity<Map<String, Object>> regraDeNegocio(RuntimeException e) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    /** 403 com código próprio para o React levar o usuário à tela de confirmar o e-mail. */
    @ExceptionHandler(EmailNaoVerificadoException.class)
    ResponseEntity<Map<String, Object>> emailNaoVerificado(EmailNaoVerificadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage(), "codigo", "EMAIL_NAO_VERIFICADO"));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<Map<String, Object>> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, Object>> login(AuthenticationException e) {
        return erro(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos");
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> semLogin(IllegalStateException e) {
        return erro(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> fotoGrande(MaxUploadSizeExceededException e) {
        return erro(HttpStatus.PAYLOAD_TOO_LARGE, "A foto pode ter no máximo 5 MB");
    }

    /** Erros de validação (@Valid): devolve a primeira mensagem e a lista por campo. */
    @ExceptionHandler(BindException.class)
    ResponseEntity<Map<String, Object>> validacao(BindException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String primeira = campos.values().stream().findFirst().orElse("Dados inválidos");
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("erro", primeira);
        corpo.put("campos", campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    private static ResponseEntity<Map<String, Object>> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem == null ? "Erro inesperado" : mensagem));
    }
}
