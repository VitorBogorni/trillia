package br.com.analise;

/**
 * Aplica as políticas de crédito na ordem de prioridade.
 * A primeira política atendida decide e encerra a análise.
 */
public class AnaliseCredito {

    private AnaliseCredito() {
    }

    public static Decisao analisar(Cliente cliente, Limites limites) {
        if (cliente.inadimplente()) {
            return new Decisao(Resultado.REPROVADO, "politica_1",
                    "Cliente com inadimplência ativa");
        }
        if (cliente.score() < limites.scoreMinimo()) {
            return new Decisao(Resultado.REPROVADO, "politica_2",
                    "Score de crédito inferior a " + limites.scoreMinimo());
        }
        if (cliente.renda().compareTo(limites.rendaMinima()) < 0) {
            return new Decisao(Resultado.REPROVADO, "politica_3",
                    "Renda mensal inferior ao mínimo de R$ " + limites.rendaMinima());
        }
        if (cliente.mesesRelacionamento() < limites.mesesMinimosRelacionamento()) {
            return new Decisao(Resultado.REPROVADO, "politica_4",
                    "Menos de " + limites.mesesMinimosRelacionamento() + " meses de relacionamento");
        }
        return new Decisao(Resultado.APROVADO, "politica_5",
                "Cliente atende a todos os critérios de crédito");
    }
}
