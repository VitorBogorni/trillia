package br.com.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class AnaliseCreditoTest {

    private static final Limites LIMITES = new Limites(500, new BigDecimal("1621.00"), 6);

    @Test
    void inadimplenteReprovaPelaPolitica1() {
        Decisao decisao = AnaliseCredito.analisar(cliente(true, 800, "5000.00", 24), LIMITES);
        assertEquals(new Decisao(Resultado.REPROVADO, "politica_1",
                "Cliente com inadimplência ativa"), decisao);
    }

    @Test
    void scoreAbaixoDe500ReprovaPelaPolitica2() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 499, "5000.00", 24), LIMITES);
        assertEquals(new Decisao(Resultado.REPROVADO, "politica_2",
                "Score de crédito inferior a 500"), decisao);
    }

    @Test
    void rendaAbaixoDoMinimoReprovaPelaPolitica3() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "1620.99", 24), LIMITES);
        assertEquals(new Decisao(Resultado.REPROVADO, "politica_3",
                "Renda mensal inferior ao mínimo de R$ 1621.00"), decisao);
    }

    @Test
    void menosDe6MesesDeRelacionamentoReprovaPelaPolitica4() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "5000.00", 5), LIMITES);
        assertEquals(new Decisao(Resultado.REPROVADO, "politica_4",
                "Menos de 6 meses de relacionamento"), decisao);
    }

    @Test
    void clienteQueAtendeTodosOsCriteriosAprovaPelaPolitica5() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "5000.00", 24), LIMITES);
        assertEquals(new Decisao(Resultado.APROVADO, "politica_5",
                "Cliente atende a todos os critérios de crédito"), decisao);
    }

    // Prioridade: cliente que cai em mais de uma política recebe a primeira

    @Test
    void clienteQueFalhaEmTudoCaiNaPolitica1() {
        Decisao decisao = AnaliseCredito.analisar(cliente(true, 100, "1000.00", 0), LIMITES);
        confere(decisao, Resultado.REPROVADO, "politica_1");
    }

    @Test
    void scoreBaixoTemPrioridadeSobreRendaBaixa() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 300, "1000.00", 24), LIMITES);
        confere(decisao, Resultado.REPROVADO, "politica_2");
    }

    @Test
    void rendaBaixaTemPrioridadeSobrePoucoRelacionamento() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "1000.00", 2), LIMITES);
        confere(decisao, Resultado.REPROVADO, "politica_3");
    }

    // Fronteira: "inferior a" e "menos de" não incluem o próprio limite

    @Test
    void score500NaoReprova() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 500, "5000.00", 24), LIMITES);
        confere(decisao, Resultado.APROVADO, "politica_5");
    }

    @Test
    void rendaIgualAoMinimoNaoReprova() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "1621.00", 24), LIMITES);
        confere(decisao, Resultado.APROVADO, "politica_5");
    }

    @Test
    void rendaIgualAoMinimoSemCentavosNaoReprova() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "1621", 24), LIMITES);
        confere(decisao, Resultado.APROVADO, "politica_5");
    }

    @Test
    void seisMesesDeRelacionamentoNaoReprova() {
        Decisao decisao = AnaliseCredito.analisar(cliente(false, 800, "5000.00", 6), LIMITES);
        confere(decisao, Resultado.APROVADO, "politica_5");
    }

    @Test
    void outrosLimitesMudamADecisaoEOMotivo() {
        Limites limitesMaisRigidos = new Limites(600, new BigDecimal("1621.00"), 6);

        Decisao decisao = AnaliseCredito.analisar(cliente(false, 550, "5000.00", 24), limitesMaisRigidos);

        assertEquals(new Decisao(Resultado.REPROVADO, "politica_2",
                "Score de crédito inferior a 600"), decisao);
    }

    private static Cliente cliente(boolean inadimplente, int score, String renda, int mesesRelacionamento) {
        return new Cliente("1", "Cliente Teste", 30, new BigDecimal(renda), mesesRelacionamento, score, inadimplente);
    }

    private static void confere(Decisao decisao, Resultado resultadoEsperado, String politicaEsperada) {
        assertEquals(resultadoEsperado, decisao.resultado());
        assertEquals(politicaEsperada, decisao.politica());
    }
}
