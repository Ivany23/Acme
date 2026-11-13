package acme.servico;

import acme.modelo.RelatorioProducao;

import java.util.List;

/** Minera os dados históricos dos relatórios de produção (RF009). */
public class AnalisadorDados {

    private final List<RelatorioProducao> relatorios;

    public AnalisadorDados(List<RelatorioProducao> relatorios) {
        this.relatorios = relatorios;
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
}
