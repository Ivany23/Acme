package acme.servico;

import java.time.LocalDate;

/**
 * Critérios para minerar os dados históricos (caso de uso "Definir Critérios de Análise").
 * Todos os campos são opcionais: um campo nulo não filtra.
 *
 * @param inicio  data mínima de geração do relatório (inclusive)
 * @param fim     data máxima de geração do relatório (inclusive)
 * @param pecaId  apenas encomendas desta peça
 * @param artesao apenas relatórios em que este artesão trabalhou (nome)
 */
public record CriteriosAnalise(LocalDate inicio, LocalDate fim, Long pecaId, String artesao) {

    public static final CriteriosAnalise TODOS = new CriteriosAnalise(null, null, null, null);

    public CriteriosAnalise {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser anterior à data inicial");
        }
    }
}
