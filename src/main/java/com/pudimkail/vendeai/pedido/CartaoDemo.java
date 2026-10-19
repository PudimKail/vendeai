package com.pudimkail.vendeai.pedido;

import java.time.YearMonth;

/**
 * Simula a operadora de cartão (MODO DEMONSTRAÇÃO, nada é cobrado).
 * Cartões de teste: 4111 1111 1111 1111 é aprovado; 4000 0000 0000 0002 é recusado.
 * O número completo e o CVV nunca são salvos: guardamos só os 4 últimos dígitos.
 */
final class CartaoDemo {

    static final String CARTAO_RECUSADO = "4000000000000002";

    private CartaoDemo() {
    }

    /** Devolve null se aprovado, ou o motivo da recusa. */
    static String validar(String numero, String validade, String cvv) {
        if (!numero.matches("\\d{13,19}") || !luhnValido(numero)) {
            return "Número de cartão inválido";
        }
        if (!cvv.matches("\\d{3,4}")) {
            return "CVV inválido";
        }
        if (!validade.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return "Validade inválida. Use MM/AA";
        }
        YearMonth vence = YearMonth.of(2000 + Integer.parseInt(validade.substring(3)), Integer.parseInt(validade.substring(0, 2)));
        if (vence.isBefore(YearMonth.now())) {
            return "Cartão vencido";
        }
        if (numero.equals(CARTAO_RECUSADO)) {
            return "Pagamento recusado pelo banco emissor";
        }
        return null;
    }

    /** Algoritmo de Luhn: o dígito verificador que todo cartão de crédito tem. */
    static boolean luhnValido(String numero) {
        int soma = 0;
        boolean dobrar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (dobrar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }
}
