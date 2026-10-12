package br.com.analise;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record Limites(int scoreMinimo, BigDecimal rendaMinima, int mesesMinimosRelacionamento) {

    public static Limites carregar(Path arquivo) throws IOException {
        if (!Files.exists(arquivo)) {
            throw new IllegalArgumentException("Arquivo de limites não encontrado: " + arquivo);
        }
        Properties propriedades = new Properties();
        try (BufferedReader leitor = Files.newBufferedReader(arquivo)) {
            propriedades.load(leitor);
        }
        try {
            return new Limites(
                    Integer.parseInt(lerValor(propriedades, "score.minimo", arquivo)),
                    new BigDecimal(lerValor(propriedades, "renda.minima", arquivo)),
                    Integer.parseInt(lerValor(propriedades, "meses.minimos.relacionamento", arquivo)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido no arquivo de limites: " + arquivo, e);
        }
    }

    private static String lerValor(Properties propriedades, String nome, Path arquivo) {
        String valor = propriedades.getProperty(nome);
        if (valor == null) {
            throw new IllegalArgumentException("Falta o limite " + nome + " em " + arquivo);
        }
        return valor.trim();
    }
}
