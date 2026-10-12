# Análise de crédito

Lê um CSV de clientes, aplica as políticas de crédito na ordem de prioridade e gera um CSV com o resultado, a política aplicada e o motivo de cada cliente.

## Como executar

Requer Java 17 ou mais novo. O Maven não precisa estar instalado: o `mvnw` baixa a versão certa.

```bash
./mvnw package          # no Windows: .\mvnw.cmd package
java -jar target/analise-credito.jar clientes_analise.csv resultado_analise.csv
```

O `package` compila, roda os testes e gera o `.jar`. Rode os comandos na raiz do projeto, onde fica o `limites.properties`.

## Fluxo

1. **Carrega os limites** das políticas do arquivo `limites.properties`.
2. **Confere os arquivos**: a entrada precisa existir, a pasta de saída também, e a saída não pode ser o próprio arquivo de entrada.
3. **Confere o cabeçalho** do CSV, para não ler colunas fora de ordem.
4. **Para cada cliente**, uma linha por vez: valida os dados, aplica as políticas na ordem e grava a decisão num arquivo temporário.
5. **Se todas as linhas forem válidas**, o temporário vira o arquivo de saída. Se alguma for inválida, o programa para, mostra a linha com problema e não deixa resultado pela metade.

## Arquivo de entrada

É o primeiro argumento do comando. O arquivo do desafio está na raiz do projeto, mas qualquer CSV no mesmo formato funciona:

```
id,Nome,Idade,Renda,Meses Relacionamento,Score,Inadimplente
1,Norean Agate,33,9268.89,84,292,true
```

A primeira linha precisa ser exatamente esse cabeçalho. Codificação UTF-8, separador vírgula, decimal com ponto e `Inadimplente` como `true` ou `false`. Linhas em branco são ignoradas.

## Arquivo de saída

É o segundo argumento do comando. O arquivo é criado (ou sobrescrito) em UTF-8, com uma linha por cliente, na ordem da entrada:

```
id,Nome,Resultado,PoliticaAplicada,Motivo
1,Norean Agate,REPROVADO,politica_1,Cliente com inadimplência ativa
3,Vonni McCrie,REPROVADO,politica_4,Menos de 6 meses de relacionamento
6,Monah Slegg,APROVADO,politica_5,Cliente atende a todos os critérios de crédito
```

## Políticas

A primeira política atendida decide e encerra a análise.

| Política | Condição | Resultado | Clientes no arquivo do desafio |
|---|---|---|---|
| `politica_1` | Inadimplência ativa | REPROVADO | 506 |
| `politica_2` | Score menor que 500 | REPROVADO | 228 |
| `politica_3` | Renda menor que R$ 1.621,00 | REPROVADO | 12 |
| `politica_4` | Menos de 6 meses de relacionamento | REPROVADO | 12 |
| `politica_5` | Nenhuma das anteriores | APROVADO | 242 |

Os limites ficam no `limites.properties` e podem ser ajustados sem mudar o código:

```properties
score.minimo=500
renda.minima=1621.00
meses.minimos.relacionamento=6
```

## Decisões técnicas

- **Renda mínima de R$ 1.621,00**, o salário mínimo de 2026. O enunciado deixa o valor em aberto, e esta é uma referência oficial, fácil de auditar.
- **Políticas em sequência de `if` com `return`**, na ordem do enunciado. O `return` encerra a análise na primeira política atendida, e o último, sem condição, garante uma decisão para todo cliente.
- **Limites num arquivo de configuração**, para a área de crédito ajustar score, renda e meses sem mexer no código. Os motivos usam os mesmos valores, então mudam junto.
- **Leitura linha a linha com arquivo temporário.** O uso de memória não cresce com o tamanho do arquivo, e o resultado só aparece quando está completo.
- **Linha inválida interrompe o processamento**: coluna faltando, número inválido, score fora de 0 a 1000 ou `Inadimplente` diferente de `true`/`false`. A mensagem mostra a linha e o programa termina com código 1, como também acontece com cabeçalho diferente, arquivo vazio, inexistente ou fora de UTF-8, e limites ausentes ou inválidos. Tratar um valor desconhecido como "não inadimplente" poderia aprovar quem deveria ser reprovado.
- **Cabeçalho conferido**, ignorando o caractere invisível que o Excel grava no início de alguns arquivos, para não recusar um CSV correto.
- **Sem biblioteca de CSV.** O arquivo não tem aspas nem vírgula dentro dos campos, então o `split` resolve. Um nome com vírgula é recusado, em vez de ser lido errado.
- **Renda em `BigDecimal` e resultado em `enum`**: valor monetário exato, e só APROVADO ou REPROVADO são aceitos pelo compilador.
- **Regra separada do arquivo.** `AnaliseCredito` tem as políticas e é testada sem arquivo; `Main` lê e grava o CSV; `Limites` carrega a configuração; `Cliente`, `Decisao` e `Resultado` são só dados.
- **Idade não é usada**, porque o enunciado define exatamente cinco políticas.

## Testes

`./mvnw test` roda os testes, que cobrem:
- cada política, a prioridade entre elas e as fronteiras (score 500, renda de R$ 1.621,00 e 6 meses não reprovam);
- limites diferentes mudando a decisão e o motivo;
- cabeçalho, inclusive com o caractere invisível do Excel;
- linhas inválidas, arquivos inexistentes, vazios ou fora de UTF-8, e saída igual à entrada;
- linha inválida sem deixar resultado pela metade;
- o arquivo do desafio, conferindo os totais da tabela acima.
