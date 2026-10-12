package br.com.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    private static final String CABECALHO_ENTRADA = "id,Nome,Idade,Renda,Meses Relacionamento,Score,Inadimplente";
    private static final Limites LIMITES = new Limites(500, new BigDecimal("1621.00"), 6);

    @TempDir
    Path pasta;

    @Test
    void geraUmaLinhaPorClienteDoArquivoDoDesafio() throws IOException {
        List<String> saida = processar(Path.of("clientes_analise.csv"));

        assertEquals(1001, saida.size()); // cabeçalho + 1000 clientes
        assertEquals("id,Nome,Resultado,PoliticaAplicada,Motivo", saida.get(0));
        assertEquals("1,Norean Agate,REPROVADO,politica_1,Cliente com inadimplência ativa", saida.get(1));
    }

    @Test
    void distribuicaoDasPoliticasNoArquivoDoDesafio() throws IOException {
        List<String> saida = processar(Path.of("clientes_analise.csv"));

        // Totais conferidos com uma contagem feita por fora do programa
        assertEquals(506, contar(saida, "politica_1"));
        assertEquals(228, contar(saida, "politica_2"));
        assertEquals(12, contar(saida, "politica_3"));
        assertEquals(12, contar(saida, "politica_4"));
        assertEquals(242, contar(saida, "politica_5"));
    }

    @Test
    void ignoraLinhaEmBranco() throws IOException {
        Path entrada = criarEntrada("1,Fulano,30,5000.00,24,800,false", "");

        List<String> saida = processar(entrada);

        assertEquals(2, saida.size());
    }

    @Test
    void aceitaCabecalhoComCaractereInvisivelDoExcel() throws IOException {
        Path entrada = pasta.resolve("entrada.csv");
        Files.writeString(entrada, "﻿" + CABECALHO_ENTRADA + "\n1,Fulano,30,5000.00,24,800,false\n");

        List<String> saida = processar(entrada);

        assertEquals(2, saida.size());
    }

    @Test
    void recusaCabecalhoDiferente() throws IOException {
        Path entrada = pasta.resolve("entrada.csv");
        Files.writeString(entrada, "id,Nome,Idade,Renda,Score,Meses Relacionamento,Inadimplente\n"
                + "1,Fulano,30,5000.00,800,24,false\n");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> processar(entrada));
        assertTrue(erro.getMessage().contains("Cabeçalho"), erro.getMessage());
    }

    @Test
    void recusaArquivoVazio() throws IOException {
        Path entrada = pasta.resolve("entrada.csv");
        Files.writeString(entrada, "");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> processar(entrada));
        assertTrue(erro.getMessage().contains("vazio"), erro.getMessage());
    }

    @Test
    void recusaInadimplenteDiferenteDeTrueOuFalse() throws IOException {
        recusaMostrandoALinha("1,Fulano,30,5000.00,24,800,sim");
    }

    @Test
    void recusaLinhaComColunaFaltando() throws IOException {
        recusaMostrandoALinha("1,Fulano,30,5000.00,24,800");
    }

    @Test
    void recusaNumeroInvalido() throws IOException {
        recusaMostrandoALinha("1,Fulano,30,abc,24,800,false");
    }

    @Test
    void recusaScoreForaDe0A1000() throws IOException {
        recusaMostrandoALinha("1,Fulano,30,5000.00,24,1500,false");
    }

    @Test
    void linhaInvalidaNaoDeixaResultadoPelaMetade() throws IOException {
        Path entrada = criarEntrada("1,Fulano,30,5000.00,24,800,false", "2,Ciclano,30,abc,24,800,false");
        Path saida = pasta.resolve("resultado.csv");

        assertThrows(IllegalArgumentException.class, () -> Main.processar(entrada, saida, LIMITES));

        assertFalse(Files.exists(saida));
        assertFalse(Files.exists(pasta.resolve("resultado.csv.tmp")));
    }

    @Test
    void recusaArquivoDeEntradaInexistente() {
        Path inexistente = pasta.resolve("nao-existe.csv");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> processar(inexistente));
        assertTrue(erro.getMessage().contains("nao-existe.csv"), erro.getMessage());
    }

    @Test
    void recusaPastaDeSaidaInexistente() throws IOException {
        Path entrada = criarEntrada("1,Fulano,30,5000.00,24,800,false");
        Path saida = pasta.resolve("nao-existe").resolve("resultado.csv");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> Main.processar(entrada, saida, LIMITES));
        assertTrue(erro.getMessage().contains("nao-existe"), erro.getMessage());
    }

    @Test
    void recusaSaidaIgualAEntradaSemApagarAEntrada() throws IOException {
        Path entrada = criarEntrada("1,Fulano,30,5000.00,24,800,false");
        String conteudoOriginal = Files.readString(entrada);

        assertThrows(IllegalArgumentException.class, () -> Main.processar(entrada, entrada, LIMITES));

        assertEquals(conteudoOriginal, Files.readString(entrada));
    }

    @Test
    void recusaArquivoForaDeUtf8() throws IOException {
        Path entrada = pasta.resolve("entrada.csv");
        String conteudo = CABECALHO_ENTRADA + "\n1,João,30,5000.00,24,800,false\n";
        // Codificação em que o Excel salva CSV no Windows
        Files.write(entrada, conteudo.getBytes(Charset.forName("windows-1252")));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> processar(entrada));
        assertTrue(erro.getMessage().contains("UTF-8"), erro.getMessage());
    }

    private void recusaMostrandoALinha(String linhaInvalida) throws IOException {
        Path entrada = criarEntrada(linhaInvalida);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> processar(entrada));
        assertTrue(erro.getMessage().contains(linhaInvalida), erro.getMessage());
    }

    private List<String> processar(Path entrada) throws IOException {
        Path saida = pasta.resolve("resultado.csv");
        Main.processar(entrada, saida, LIMITES);
        return Files.readAllLines(saida);
    }

    private Path criarEntrada(String... clientes) throws IOException {
        Path entrada = pasta.resolve("entrada.csv");
        Files.writeString(entrada, CABECALHO_ENTRADA + "\n" + String.join("\n", clientes) + "\n");
        return entrada;
    }

    private static int contar(List<String> linhas, String politica) {
        int total = 0;
        for (String linha : linhas) {
            if (linha.contains("," + politica + ",")) {
                total++;
            }
        }
        return total;
    }
}
