package br.com.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LimitesTest {

    @TempDir
    Path pasta;

    @Test
    void carregaOArquivoDeLimitesDoProjeto() throws IOException {
        Limites limites = Limites.carregar(Path.of("limites.properties"));

        assertEquals(new Limites(500, new BigDecimal("1621.00"), 6), limites);
    }

    @Test
    void recusaArquivoDeLimitesInexistente() {
        Path inexistente = pasta.resolve("nao-existe.properties");

        assertThrows(IllegalArgumentException.class, () -> Limites.carregar(inexistente));
    }

    @Test
    void recusaLimiteFaltando() throws IOException {
        Path arquivo = pasta.resolve("limites.properties");
        Files.writeString(arquivo, "score.minimo=500\nmeses.minimos.relacionamento=6\n");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> Limites.carregar(arquivo));
        assertTrue(erro.getMessage().contains("renda.minima"), erro.getMessage());
    }

    @Test
    void recusaLimiteQueNaoEhNumero() throws IOException {
        Path arquivo = pasta.resolve("limites.properties");
        Files.writeString(arquivo, "score.minimo=quinhentos\nrenda.minima=1621.00\nmeses.minimos.relacionamento=6\n");

        assertThrows(IllegalArgumentException.class, () -> Limites.carregar(arquivo));
    }
}
