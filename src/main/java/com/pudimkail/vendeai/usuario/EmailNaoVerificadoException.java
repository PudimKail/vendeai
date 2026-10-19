package com.pudimkail.vendeai.usuario;

public class EmailNaoVerificadoException extends RuntimeException {

    public EmailNaoVerificadoException() {
        super("Confirme seu e-mail com o código que enviamos para continuar");
    }
}
