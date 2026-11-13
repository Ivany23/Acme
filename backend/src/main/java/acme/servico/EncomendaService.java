package acme.servico;

import acme.modelo.Cliente;
import acme.modelo.Encomenda;
import acme.modelo.EstadoEncomenda;
import acme.modelo.Peca;
import acme.observer.GeradorRelatorio;
import acme.repositorio.EncomendaRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Respostas.EncomendaDTO;
import acme.web.dto.Respostas.OrcamentoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Encomendas (RF002) e orçamento (RF008). Liga os observadores (padrão Observer) a cada operação. */
@Service
@Transactional
public class EncomendaService {

    private final EncomendaRepository encomendas;
    private final RegistoProducaoRepository registos;
    private final PessoasService pessoas;
    private final CatalogoService catalogo;
    private final CalculadoraCustos calculadora;
    private final GeradorRelatorio geradorRelatorio;

    public EncomendaService(EncomendaRepository encomendas, RegistoProducaoRepository registos,
                            PessoasService pessoas, CatalogoService catalogo, CalculadoraCustos calculadora,
                            GeradorRelatorio geradorRelatorio) {
        this.encomendas = encomendas;
        this.registos = registos;
        this.pessoas = pessoas;
        this.catalogo = catalogo;
        this.calculadora = calculadora;
        this.geradorRelatorio = geradorRelatorio;
    }

    @Transactional(readOnly = true)
    public List<EncomendaDTO> listar(EstadoEncomenda estado, Long clienteId) {
        List<Encomenda> lista;
        if (clienteId != null) {
            lista = encomendas.findByClienteIdOrderByIdDesc(clienteId).stream()
                    .filter(e -> estado == null || e.getEstado() == estado).toList();
        } else if (estado != null) {
            lista = encomendas.findByEstadoOrderByIdDesc(estado);
        } else {
            lista = encomendas.findAllByOrderByIdDesc();
        }
        return lista.stream().map(EncomendaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public EncomendaDTO obter(Long id) {
        return EncomendaDTO.de(encomenda(id));
    }

    @Transactional(readOnly = true)
    public OrcamentoDTO orcamento(Long pecaId, int quantidade, double custosIndirectos) {
        Peca peca = catalogo.peca(pecaId);
        CalculadoraCustos.Orcamento o = calculadora.orcamento(peca, quantidade, custosIndirectos);
        return new OrcamentoDTO(peca.getId(), peca.getNome(), quantidade, o.custoPecas(), o.custoMateriais(),
                o.custosIndirectos(), o.percentagemOrganizacao(), o.custoBase(), o.custoEstimado());
    }

    public EncomendaDTO criar(EncomendaPedido p) {
        Cliente cliente = pessoas.cliente(p.clienteId());
        Peca peca = catalogo.peca(p.pecaId());
        double indirectos = p.custosIndirectos() == null ? 0 : p.custosIndirectos();
        Encomenda e = new Encomenda(cliente, peca, p.quantidade());
        e.definirOrcamento(indirectos, calculadora.calcularCustoEstimado(e, indirectos));
        return EncomendaDTO.de(encomendas.save(e));
    }

    /** Pagamento: a encomenda passa para EM_PRODUCAO e o cliente é avisado. */
    public EncomendaDTO confirmarPagamento(Long id) {
        Encomenda e = comObservadores(id);
        e.confirmarPagamento();
        return EncomendaDTO.de(e);
    }

    /** Muda o estado; ao concluir, o GeradorRelatorio cria o relatório automaticamente (RF009). */
    public EncomendaDTO actualizarEstado(Long id, EstadoEncomenda novoEstado) {
        Encomenda e = comObservadores(id);
        if (novoEstado == EstadoEncomenda.EM_PRODUCAO && !e.isPaga()) {
            throw new IllegalStateException("A encomenda só entra em produção depois de paga");
        }
        e.actualizarEstado(novoEstado);
        return EncomendaDTO.de(e);
    }

    public void apagar(Long id) {
        Encomenda e = encomenda(id);
        if (e.getEstado() != EstadoEncomenda.PENDENTE || !registos.findByEncomendaIdOrderByDataAscHoraInicioAsc(id).isEmpty()) {
            throw new IllegalStateException("Só é possível apagar encomendas pendentes; use o cancelamento");
        }
        encomendas.delete(e);
        encomendas.flush();
    }

    public Encomenda encomenda(Long id) {
        return encomendas.findById(id).orElseThrow(() -> new NaoEncontradoException("Encomenda", id));
    }

    private Encomenda comObservadores(Long id) {
        Encomenda e = encomenda(id);
        e.adicionarObservador(e.getCliente());
        e.adicionarObservador(geradorRelatorio);
        return e;
    }
}
