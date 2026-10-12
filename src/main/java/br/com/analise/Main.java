package br.com.analise;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Main {

    private static final Path ARQUIVO_LIMITES = Path.of("limites.properties");
    private static final String CABECALHO_ENTRADA = "id,Nome,Idade,Renda,Meses Relacionamento,Score,Inadimplente";
    private static final String CABECALHO_SAIDA = "id,Nome,Resultado,PoliticaAplicada,Motivo";
    private static final int COLUNAS_DA_ENTRADA = 7;
    private static final int SCORE_MAXIMO = 1000;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Uso: java -jar target/analise-credito.jar <arquivo-de-entrada.csv> <arquivo-de-saida.csv>");
            System.exit(1);
        }
        try {
            Limites limites = Limites.carregar(ARQUIVO_LIMITES);
            int clientesAnalisados = processar(Path.of(args[0]), Path.of(args[1]), limites);
            System.out.println("Clientes analisados: " + clientesAnalisados + ". Resultado gravado em " + args[1]);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erro ao ler ou gravar arquivo: " + e);
            System.exit(1);
        }
    }

    public static int processar(Path arquivoEntrada, Path arquivoSaida, Limites limites) throws IOException {
        conferirArquivos(arquivoEntrada, arquivoSaida);

        // O resultado é gravado num temporário e só vira o arquivo de saída no fim:
        // se alguma linha for inválida, não fica resultado pela metade
        Path arquivoTemporario = arquivoSaida.resolveSibling(arquivoSaida.getFileName() + ".tmp");
        try {
            int clientesAnalisados = analisarClientes(arquivoEntrada, arquivoTemporario, limites);
            Files.move(arquivoTemporario, arquivoSaida, StandardCopyOption.REPLACE_EXISTING);
            return clientesAnalisados;
        } finally {
            Files.deleteIfExists(arquivoTemporario);
        }
    }

    private static void conferirArquivos(Path arquivoEntrada, Path arquivoSaida) {
        if (!Files.exists(arquivoEntrada)) {
            throw new IllegalArgumentException("Arquivo de entrada não encontrado: " + arquivoEntrada);
        }
        Path pastaSaida = arquivoSaida.toAbsolutePath().getParent();
        if (!Files.isDirectory(pastaSaida)) {
            throw new IllegalArgumentException("Pasta de saída não encontrada: " + pastaSaida);
        }
        if (arquivoEntrada.toAbsolutePath().normalize().equals(arquivoSaida.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("O arquivo de saída precisa ser diferente do arquivo de entrada");
        }
    }

    private static int analisarClientes(Path arquivoEntrada, Path arquivoTemporario, Limites limites) throws IOException {
        int clientesAnalisados = 0;
        try (BufferedReader entrada = Files.newBufferedReader(arquivoEntrada);
             BufferedWriter saida = Files.newBufferedWriter(arquivoTemporario)) {
            conferirCabecalho(entrada.readLine());
            saida.write(CABECALHO_SAIDA);
            saida.newLine();

            String linha = entrada.readLine();
            while (linha != null) {
                if (!linha.isBlank()) {
                    Cliente cliente = lerCliente(linha);
                    Decisao decisao = AnaliseCredito.analisar(cliente, limites);
                    saida.write(montarLinhaSaida(cliente, decisao));
                    saida.newLine();
                    clientesAnalisados++;
                }
                linha = entrada.readLine();
            }
        } catch (MalformedInputException e) {
            throw new IllegalArgumentException("O arquivo de entrada precisa estar em UTF-8: " + arquivoEntrada, e);
        }
        return clientesAnalisados;
    }

    // O Excel pode gravar um caractere invisível (BOM) no início do arquivo; ele não faz parte do cabeçalho
    private static void conferirCabecalho(String cabecalho) {
        if (cabecalho == null) {
            throw new IllegalArgumentException("Arquivo de entrada vazio");
        }
        if (!cabecalho.replace("﻿", "").equals(CABECALHO_ENTRADA)) {
            throw new IllegalArgumentException("Cabeçalho diferente de " + CABECALHO_ENTRADA + ": " + cabecalho);
        }
    }

    private static Cliente lerCliente(String linha) {
        String[] campos = linha.split(",");
        if (campos.length != COLUNAS_DA_ENTRADA) {
            throw new IllegalArgumentException(
                    "Linha deveria ter " + COLUNAS_DA_ENTRADA + " colunas: " + linha);
        }
        try {
            int score = Integer.parseInt(campos[5]);
            // Score acima do máximo passaria pela política 2 e poderia ser aprovado
            if (score < 0 || score > SCORE_MAXIMO) {
                throw new IllegalArgumentException("Score deve estar entre 0 e " + SCORE_MAXIMO + ": " + linha);
            }
            return new Cliente(
                    campos[0],
                    campos[1],
                    Integer.parseInt(campos[2]),
                    new BigDecimal(campos[3]),
                    Integer.parseInt(campos[4]),
                    score,
                    lerInadimplente(campos[6], linha));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número inválido na linha: " + linha, e);
        }
    }

    // Valor desconhecido não pode virar "não inadimplente": aprovaria quem deveria ser reprovado
    private static boolean lerInadimplente(String valor, String linha) {
        if (valor.equals("true")) {
            return true;
        }
        if (valor.equals("false")) {
            return false;
        }
        throw new IllegalArgumentException("Inadimplente deve ser true ou false: " + linha);
    }

    private static String montarLinhaSaida(Cliente cliente, Decisao decisao) {
        return String.join(",",
                cliente.id(),
                cliente.nome(),
                decisao.resultado().name(),
                decisao.politica(),
                decisao.motivo());
    }
}
