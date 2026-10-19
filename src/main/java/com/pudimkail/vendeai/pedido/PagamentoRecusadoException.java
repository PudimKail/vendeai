package com.pudimkail.vendeai.pedido;

public class PagamentoRecusadoException extends RuntimeException {

    public PagamentoRecusadoException(String mensagem) {
        super(mensagem);
    }
}
