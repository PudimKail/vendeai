package com.pudimkail.vendeai.produto;

import java.math.BigDecimal;

/** Parâmetros da página de busca. Todos são opcionais. */
public record Filtros(String q, String categoria, BigDecimal min, BigDecimal max,
                      String condicao, boolean freteGratis, String ordem) {

    public static final String RELEVANCIA = "relevancia";
    public static final String MENOR_PRECO = "menor-preco";
    public static final String MAIOR_PRECO = "maior-preco";
    public static final String RECENTES = "recentes";

    public String getOrdemOuPadrao() {
        return ordem == null || ordem.isBlank() ? RELEVANCIA : ordem;
    }
}
