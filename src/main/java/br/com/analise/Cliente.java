package br.com.analise;

import java.math.BigDecimal;

public record Cliente(
        String id,
        String nome,
        int idade,
        BigDecimal renda,
        int mesesRelacionamento,
        int score,
        boolean inadimplente) {
}
