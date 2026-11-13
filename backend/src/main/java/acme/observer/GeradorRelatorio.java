package acme.observer;

import acme.modelo.EstadoEncomenda;
import acme.modelo.Encomenda;
import acme.modelo.RegistoPonto;
import acme.modelo.RegistoProducao;
import acme.modelo.RelatorioProducao;
import acme.repositorio.RegistoPontoRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.repositorio.RelatorioProducaoRepository;
import acme.servico.CalculadoraCustos;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Observador que gera o relatório automaticamente quando a encomenda é concluída (RF009). */
@Component
public class GeradorRelatorio implements ObservadorEncomenda {

    private final RegistoProducaoRepository registos;
    private final RegistoPontoRepository pontos;
    private final RelatorioProducaoRepository relatorios;
    private final CalculadoraCustos calculadora;

    public GeradorRelatorio(RegistoProducaoRepository registos, RegistoPontoRepository pontos,
                            RelatorioProducaoRepository relatorios, CalculadoraCustos calculadora) {
        this.registos = registos;
        this.pontos = pontos;
        this.relatorios = relatorios;
        this.calculadora = calculadora;
    }

    @Override
    public void estadoAlterado(Encomenda encomenda) {
        if (encomenda.getEstado() != EstadoEncomenda.CONCLUIDA || relatorios.existsByEncomendaId(encomenda.getId())) {
            return;
        }
        List<RegistoProducao> daEncomenda = registos.findByEncomendaIdOrderByDataAscHoraInicioAsc(encomenda.getId());

        // pontos dos artesãos que trabalharam nesta encomenda, nos dias em que trabalharam nela
        Set<String> chaves = new LinkedHashSet<>();
        Map<Long, RegistoPonto> pontosArtesaos = new LinkedHashMap<>();
        for (RegistoProducao r : daEncomenda) {
            Long artesaoId = r.getArtesao().getId();
            LocalDate data = r.getData();
            if (chaves.add(artesaoId + "|" + data)) {
                for (RegistoPonto p : pontos.findByArtesaoIdAndData(artesaoId, data)) {
                    pontosArtesaos.putIfAbsent(p.getId(), p);
                }
            }
        }

        double custoReal = calculadora.calcularCustoReal(daEncomenda);
        relatorios.save(new RelatorioProducao(encomenda, daEncomenda, List.copyOf(pontosArtesaos.values()), custoReal));
    }
}
