package acme.servico;

import acme.modelo.RelatorioProducao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Minera os dados históricos dos relatórios de produção (RF009), segundo critérios de análise. */
public class AnalisadorDados {

    private final List<RelatorioProducao> relatorios;

    public AnalisadorDados(List<RelatorioProducao> relatorios) {
        this.relatorios = relatorios;
    }

    /** Devolve um novo analisador apenas com os relatórios que cumprem os critérios. */
    public AnalisadorDados filtrar(CriteriosAnalise c) {
        if (c == null) {
            return this;
        }
        List<RelatorioProducao> escolhidos = new ArrayList<>();
        for (RelatorioProducao r : relatorios) {
            LocalDate dia = r.getDataGeracao().toLocalDate();
            if (c.inicio() != null && dia.isBefore(c.inicio())) continue;
            if (c.fim() != null && dia.isAfter(c.fim())) continue;
            if (c.pecaId() != null && !Objects.equals(r.getEncomenda().getPeca().getId(), c.pecaId())) continue;
            if (c.artesao() != null && !c.artesao().isBlank()
                    && !r.getMinutosPorArtesao().containsKey(c.artesao())) continue;
            escolhidos.add(r);
        }
        return new AnalisadorDados(escolhidos);
    }

    public List<RelatorioProducao> getRelatorios() {
        return relatorios;
    }

    public double custoMedioPorEncomenda() {
        if (relatorios.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (RelatorioProducao r : relatorios) {
            total += r.getCustoReal();
        }
        return CalculadoraCustos.arredondar(total / relatorios.size());
    }

    public long totalMinutosExtra() {
        long total = 0;
        for (RelatorioProducao r : relatorios) {
            for (long minutos : r.getMinutosExtraPorArtesao().values()) {
                total += minutos;
            }
        }
        return total;
    }

    /** Diferença média entre o custo estimado e o custo real (positivo = estimativa acima do real). */
    public double desvioMedioOrcamento() {
        if (relatorios.isEmpty()) {
            return 0;
        }
        double total = 0;
        for (RelatorioProducao r : relatorios) {
            total += r.getCustoEstimado() - r.getCustoReal();
        }
        return CalculadoraCustos.arredondar(total / relatorios.size());
    }

    public long totalRetrabalhos() {
        long total = 0;
        for (RelatorioProducao r : relatorios) {
            total += r.getRetrabalhos();
        }
        return total;
    }

    public int totalRelatorios() {
        return relatorios.size();
    }

    /** Custo real médio de cada peça. */
    public Map<String, Double> custoMedioPorPeca() {
        Map<String, double[]> soma = new TreeMap<>();
        for (RelatorioProducao r : relatorios) {
            double[] s = soma.computeIfAbsent(r.getEncomenda().getPeca().getNome(), k -> new double[2]);
            s[0] += r.getCustoReal();
            s[1]++;
        }
        Map<String, Double> media = new TreeMap<>();
        soma.forEach((peca, s) -> media.put(peca, CalculadoraCustos.arredondar(s[0] / s[1])));
        return media;
    }

    /** Total de minutos de produção de cada artesão. */
    public Map<String, Long> minutosPorArtesao() {
        Map<String, Long> total = new TreeMap<>();
        for (RelatorioProducao r : relatorios) {
            r.getMinutosPorArtesao().forEach((artesao, min) -> total.merge(artesao, min, Long::sum));
        }
        return total;
    }

    /** Total de horas extraordinárias (minutos) de cada artesão, para apoiar o seu pagamento. */
    public Map<String, Long> minutosExtraPorArtesao() {
        Map<String, Long> total = new TreeMap<>();
        for (RelatorioProducao r : relatorios) {
            r.getMinutosExtraPorArtesao().forEach((artesao, min) -> total.merge(artesao, min, Long::sum));
        }
        return total;
    }
}
