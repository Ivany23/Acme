package acme.servico;

import acme.modelo.EstadoEncomenda;
import acme.repositorio.PecaRepository;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Respostas.ClienteDTO;
import acme.web.dto.Respostas.EncomendaDTO;
import acme.web.dto.Respostas.OrcamentoDTO;
import acme.web.dto.Respostas.PecaCatalogoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Casos de uso do actor Cliente (TG1): Solicitar Orçamento de Encomenda e Acompanhar Estado da Encomenda.
 * Cada cliente só vê e altera as suas próprias encomendas.
 */
@Service
@Transactional
public class PortalClienteService {

    private final PecaRepository pecas;
    private final EncomendaService encomendas;
    private final PessoasService pessoas;

    public PortalClienteService(PecaRepository pecas, EncomendaService encomendas, PessoasService pessoas) {
        this.pecas = pecas;
        this.encomendas = encomendas;
        this.pessoas = pessoas;
    }

    @Transactional(readOnly = true)
    public ClienteDTO dados(Long clienteId) {
        return pessoas.obterCliente(clienteId);
    }

    @Transactional(readOnly = true)
    public List<PecaCatalogoDTO> catalogo() {
        return pecas.findAllByOrderByNomeAsc().stream().map(PecaCatalogoDTO::de).toList();
    }

    /** RF008: o custo estimado é apresentado ao cliente antes de confirmar. */
    @Transactional(readOnly = true)
    public OrcamentoDTO orcamento(Long pecaId, int quantidade) {
        return encomendas.orcamento(pecaId, quantidade, 0);
    }

    public EncomendaDTO encomendar(Long clienteId, Long pecaId, int quantidade) {
        return encomendas.criar(new EncomendaPedido(clienteId, pecaId, quantidade, 0.0));
    }

    @Transactional(readOnly = true)
    public List<EncomendaDTO> minhasEncomendas(Long clienteId) {
        return encomendas.listar(null, clienteId);
    }

    @Transactional(readOnly = true)
    public List<String> notificacoes(Long clienteId) {
        return pessoas.notificacoes(clienteId);
    }

    /** O cliente só pode cancelar as suas encomendas que ainda estejam pendentes. */
    public EncomendaDTO cancelar(Long clienteId, Long encomendaId) {
        EncomendaDTO e = encomendas.obter(encomendaId);
        if (!Objects.equals(e.clienteId(), clienteId)) {
            throw new NaoEncontradoException("Encomenda", encomendaId);
        }
        if (e.estado() != EstadoEncomenda.PENDENTE) {
            throw new IllegalStateException("Só pode cancelar encomendas que ainda estão pendentes");
        }
        return encomendas.actualizarEstado(encomendaId, EstadoEncomenda.CANCELADA);
    }
}
