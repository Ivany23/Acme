package acme.web.dto;

import acme.modelo.*;
import acme.servico.AnalisadorDados;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** Objectos (JSON) devolvidos pela API. Nunca se devolvem entidades JPA directamente. */
public final class Respostas {

    private Respostas() {
    }

    public record UtilizadorDTO(Long id, String login, String nome) {
        public static UtilizadorDTO de(Utilizador u) {
            return new UtilizadorDTO(u.getId(), u.getLogin(), u.getNome());
        }
    }

    public record SessaoDTO(String token, UtilizadorDTO utilizador) {
    }

    public record MaterialDTO(Long id, String nome, String unidadeMedida, double custoUnitario) {
        public static MaterialDTO de(Material m) {
            return new MaterialDTO(m.getId(), m.getNome(), m.getUnidadeMedida(), m.getCustoUnitario());
        }
    }

    public record MaterialPecaDTO(Long materialId, String materialNome, String unidadeMedida,
                                  double quantidadePorUnidade, double custo) {
        public static MaterialPecaDTO de(MaterialPeca mp) {
            Material m = mp.getMaterial();
            return new MaterialPecaDTO(m.getId(), m.getNome(), m.getUnidadeMedida(),
                    mp.getQuantidadePorUnidade(), mp.custo());
        }
    }

    public record EtapaDTO(Long id, String descricao, int ordem) {
        public static EtapaDTO de(EtapaProducao e) {
            return new EtapaDTO(e.getId(), e.getDescricao(), e.getOrdem());
        }
    }

    public record PecaDTO(Long id, String nome, String dimensoes, double precoUnitario,
                          double custoMateriaisPorUnidade, List<MaterialPecaDTO> materiais, List<EtapaDTO> etapas) {
        public static PecaDTO de(Peca p) {
            return new PecaDTO(p.getId(), p.getNome(), p.getDimensoes(), p.getPrecoUnitario(),
                    p.custoMateriaisPorUnidade(),
                    p.getMateriais().stream().map(MaterialPecaDTO::de).toList(),
                    p.getEtapas().stream().map(EtapaDTO::de).toList());
        }
    }

    public record ClienteDTO(Long id, String nome, String email, String telefone) {
        public static ClienteDTO de(Cliente c) {
            return new ClienteDTO(c.getId(), c.getNome(), c.getEmail(), c.getTelefone());
        }
    }

    public record FuncionarioDTO(Long id, String tipo, String nome, String cargo, String telefone,
                                 String endereco, String especialidade) {
        public static FuncionarioDTO de(Funcionario f) {
            boolean artesao = f instanceof Artesao;
            return new FuncionarioDTO(f.getId(), artesao ? "ARTESAO" : "FUNCIONARIO", f.getNome(), f.getCargo(),
                    f.getTelefone(), f.getEndereco(), artesao ? ((Artesao) f).getEspecialidade() : null);
        }
    }

    public record TurnoDTO(Long id, String denominacao, LocalTime horaEntrada, LocalTime horaSaida,
                           long duracaoMinutos) {
        public static TurnoDTO de(Turno t) {
            return new TurnoDTO(t.getId(), t.getDenominacao(), t.getHoraEntrada(), t.getHoraSaida(),
                    t.calcularDuracaoMinutos());
        }
    }

    public record AlocacaoDTO(Long id, Long artesaoId, String artesaoNome, Long turnoId,
                              String turnoDenominacao, LocalDate data) {
        public static AlocacaoDTO de(AlocacaoTurno a) {
            return new AlocacaoDTO(a.getId(), a.getArtesao().getId(), a.getArtesao().getNome(),
                    a.getTurno().getId(), a.getTurno().getDenominacao(), a.getData());
        }
    }

    public record EncomendaDTO(Long id, Long clienteId, String clienteNome, Long pecaId, String pecaNome,
                               int quantidade, EstadoEncomenda estado, boolean paga, double custosIndirectos,
                               double custoEstimado, LocalDateTime dataCriacao) {
        public static EncomendaDTO de(Encomenda e) {
            return new EncomendaDTO(e.getId(), e.getCliente().getId(), e.getCliente().getNome(),
                    e.getPeca().getId(), e.getPeca().getNome(), e.getQuantidade(), e.getEstado(), e.isPaga(),
                    e.getCustosIndirectos(), e.getCustoEstimado(), e.getDataCriacao());
        }
    }

    public record OrcamentoDTO(Long pecaId, String pecaNome, int quantidade, double custoPecas,
                               double custoMateriais, double custosIndirectos, double percentagemOrganizacao,
                               double custoBase, double custoEstimado) {
    }

    public record MaterialUsadoDTO(Long materialId, String materialNome, String unidadeMedida,
                                   double quantidade, double custo) {
        public static MaterialUsadoDTO de(MaterialUsado mu) {
            Material m = mu.getMaterial();
            return new MaterialUsadoDTO(m.getId(), m.getNome(), m.getUnidadeMedida(), mu.getQuantidade(), mu.custo());
        }
    }

    public record RegistoProducaoDTO(Long id, Long artesaoId, String artesaoNome, Long encomendaId,
                                     Long etapaId, String etapaDescricao, Long turnoId, String turnoDenominacao,
                                     LocalDate data, LocalTime horaInicio, LocalTime horaFim,
                                     long tempoGastoMinutos, List<MaterialUsadoDTO> materiaisUsados,
                                     double custoMateriais, double custosIndirectos, boolean retrabalho) {
        public static RegistoProducaoDTO de(RegistoProducao r) {
            return new RegistoProducaoDTO(r.getId(), r.getArtesao().getId(), r.getArtesao().getNome(),
                    r.getEncomenda().getId(), r.getEtapa().getId(), r.getEtapa().getDescricao(),
                    r.getTurno().getId(), r.getTurno().getDenominacao(), r.getData(), r.getHoraInicio(),
                    r.getHoraFim(), r.calcularTempoGastoMinutos(),
                    r.getMateriaisUsados().stream().map(MaterialUsadoDTO::de).toList(),
                    r.custoMateriais(), r.getCustosIndirectos(), r.isRetrabalho());
        }
    }

    public record RegistoPontoDTO(Long id, Long artesaoId, String artesaoNome, Long turnoId,
                                  String turnoDenominacao, LocalDate data, LocalTime horaEntrada,
                                  LocalTime horaSaida, long minutosTrabalhados, long minutosExtra) {
        public static RegistoPontoDTO de(RegistoPonto p) {
            return new RegistoPontoDTO(p.getId(), p.getArtesao().getId(), p.getArtesao().getNome(),
                    p.getTurno().getId(), p.getTurno().getDenominacao(), p.getData(), p.getHoraEntrada(),
                    p.getHoraSaida(), p.calcularMinutosTrabalhados(), p.calcularMinutosExtra());
        }
    }

    public record RelatorioDTO(Long id, Long encomendaId, String clienteNome, String pecaNome, int quantidade,
                               Map<String, Long> minutosPorEtapa, Map<String, Long> minutosPorArtesao,
                               Map<String, Double> materiaisConsumidos, Map<String, Long> minutosExtraPorArtesao,
                               long retrabalhos, double custoEstimado, double custoReal, double desvio,
                               LocalDateTime dataGeracao) {
        public static RelatorioDTO de(RelatorioProducao r) {
            Encomenda e = r.getEncomenda();
            return new RelatorioDTO(r.getId(), e.getId(), e.getCliente().getNome(), e.getPeca().getNome(),
                    e.getQuantidade(), new java.util.TreeMap<>(r.getMinutosPorEtapa()), new java.util.TreeMap<>(r.getMinutosPorArtesao()),
                    new java.util.TreeMap<>(r.getMateriaisConsumidos()), new java.util.TreeMap<>(r.getMinutosExtraPorArtesao()),
                    r.getRetrabalhos(), r.getCustoEstimado(), r.getCustoReal(),
                    Math.round((r.getCustoEstimado() - r.getCustoReal()) * 100.0) / 100.0, r.getDataGeracao());
        }
    }

    public record AnaliseDTO(int totalRelatorios, double custoMedioPorEncomenda, long totalMinutosExtra,
                             double desvioMedioOrcamento, long totalRetrabalhos) {
        public static AnaliseDTO de(AnalisadorDados a) {
            return new AnaliseDTO(a.totalRelatorios(), a.custoMedioPorEncomenda(), a.totalMinutosExtra(),
                    a.desvioMedioOrcamento(), a.totalRetrabalhos());
        }
    }

    public record DashboardDTO(long totalClientes, long totalPecas, long totalMateriais, long totalFuncionarios,
                               long totalArtesaos, long totalTurnos, Map<EstadoEncomenda, Long> encomendasPorEstado,
                               AnaliseDTO analise) {
    }
}
