package acme.servico;

import acme.modelo.Encomenda;
import acme.modelo.Peca;
import acme.modelo.RegistoProducao;

import java.util.List;

/** Calcula os custos de produção (RF008). */
public class CalculadoraCustos {

    private final double percentagemOrganizacao;

    public CalculadoraCustos(double percentagemOrganizacao) {
        if (percentagemOrganizacao < 0) {
            throw new IllegalArgumentException("A percentagem da organização não pode ser negativa");
        }
        this.percentagemOrganizacao = percentagemOrganizacao;
    }

    /** Detalhe do orçamento de uma encomenda. */
    public record Orcamento(double custoPecas, double custoMateriais, double custosIndirectos,
                            double percentagemOrganizacao, double custoBase, double custoEstimado) {
    }

    public Orcamento orcamento(Peca peca, int quantidade, double custosIndirectos) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        if (custosIndirectos < 0) {
            throw new IllegalArgumentException("Os custos indirectos não podem ser negativos");
        }
        double custoPecas = peca.getPrecoUnitario() * quantidade;
        double custoMateriais = peca.custoMateriaisPorUnidade() * quantidade;
        double custoBase = custoPecas + custoMateriais + custosIndirectos;
        double estimado = custoBase * (1 + percentagemOrganizacao / 100);
        return new Orcamento(arredondar(custoPecas), arredondar(custoMateriais), arredondar(custosIndirectos),
                percentagemOrganizacao, arredondar(custoBase), arredondar(estimado));
    }

    public double calcularCustoMateriais(Encomenda e) {
        return e.getPeca().custoMateriaisPorUnidade() * e.getQuantidade();
    }

    /** Custo estimado = (preço da peça × quantidade + materiais + custos indirectos) + percentagem da organização. */
    public double calcularCustoEstimado(Encomenda e, double custosIndirectos) {
        return orcamento(e.getPeca(), e.getQuantidade(), custosIndirectos).custoEstimado();
    }

    /** Custo real = materiais efectivamente usados + custos indirectos registados. */
    public double calcularCustoReal(List<RegistoProducao> registos) {
        double total = 0;
        for (RegistoProducao r : registos) {
            total += r.custoMateriais() + r.getCustosIndirectos();
        }
        return arredondar(total);
    }

    public double getPercentagemOrganizacao() {
        return percentagemOrganizacao;
    }

    static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
