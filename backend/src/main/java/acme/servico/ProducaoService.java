package acme.servico;

import acme.modelo.*;
import acme.repositorio.RegistoPontoRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.web.dto.Pedidos.QuantidadeMaterialPedido;
import acme.web.dto.Pedidos.RegistoPontoPedido;
import acme.web.dto.Pedidos.RegistoProducaoPedido;
import acme.web.dto.Respostas.RegistoPontoDTO;
import acme.web.dto.Respostas.RegistoProducaoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Registo de produção (RF006) e registo de ponto (RF007). */
@Service
@Transactional
public class ProducaoService {

    private final RegistoProducaoRepository registos;
    private final RegistoPontoRepository pontos;
    private final EncomendaService encomendas;
    private final PessoasService pessoas;
    private final GestorAlocacao gestorAlocacao;
    private final CatalogoService catalogo;

    public ProducaoService(RegistoProducaoRepository registos, RegistoPontoRepository pontos,
                           EncomendaService encomendas, PessoasService pessoas, GestorAlocacao gestorAlocacao,
                           CatalogoService catalogo) {
        this.registos = registos;
        this.pontos = pontos;
        this.encomendas = encomendas;
        this.pessoas = pessoas;
        this.gestorAlocacao = gestorAlocacao;
        this.catalogo = catalogo;
    }

    // ---------- Produção ----------

    @Transactional(readOnly = true)
    public List<RegistoProducaoDTO> listarRegistos(Long encomendaId) {
        List<RegistoProducao> lista = encomendaId == null
                ? registos.findAllByOrderByDataDescHoraInicioDesc()
                : registos.findByEncomendaIdOrderByDataAscHoraInicioAsc(encomendaId);
        return lista.stream().map(RegistoProducaoDTO::de).toList();
    }

    public RegistoProducaoDTO registarProducao(RegistoProducaoPedido p) {
        Encomenda encomenda = encomendas.encomenda(p.encomendaId());
        if (encomenda.getEstado() != EstadoEncomenda.EM_PRODUCAO) {
            throw new IllegalStateException("Só é possível registar produção em encomendas EM_PRODUCAO");
        }
        EtapaProducao etapa = encomenda.getPeca().obterEtapa(p.etapaId());
        if (etapa == null) {
            throw new IllegalArgumentException("A etapa " + p.etapaId() + " não pertence à peça desta encomenda");
        }
        Artesao artesao = pessoas.artesao(p.artesaoId());
        Turno turno = gestorAlocacao.turno(p.turnoId());
        LocalDate data = p.data() == null ? LocalDate.now() : p.data();
        double indirectos = p.custosIndirectos() == null ? 0 : p.custosIndirectos();

        RegistoProducao r = new RegistoProducao(artesao, encomenda, etapa, turno, data, p.horaInicio(),
                p.horaFim(), indirectos, Boolean.TRUE.equals(p.retrabalho()));
        if (p.materiaisUsados() != null) {
            for (QuantidadeMaterialPedido q : p.materiaisUsados()) {
                r.adicionarMaterialUsado(catalogo.material(q.materialId()), q.quantidade());
            }
        }
        return RegistoProducaoDTO.de(registos.save(r));
    }

    public void apagarRegisto(Long id) {
        RegistoProducao r = registos.findById(id).orElseThrow(() -> new NaoEncontradoException("Registo de produção", id));
        if (r.getEncomenda().getEstado() != EstadoEncomenda.EM_PRODUCAO) {
            throw new IllegalStateException("Só é possível apagar registos de encomendas EM_PRODUCAO");
        }
        registos.delete(r);
    }

    // ---------- Ponto ----------

    @Transactional(readOnly = true)
    public List<RegistoPontoDTO> listarPontos(Long artesaoId, LocalDate data) {
        List<RegistoPonto> lista;
        if (artesaoId != null && data != null) {
            lista = pontos.findByArtesaoIdAndData(artesaoId, data);
        } else if (artesaoId != null) {
            lista = pontos.findByArtesaoIdOrderByDataDesc(artesaoId);
        } else if (data != null) {
            lista = pontos.findByDataOrderByHoraEntradaAsc(data);
        } else {
            lista = pontos.findAllByOrderByDataDescHoraEntradaDesc();
        }
        return lista.stream().map(RegistoPontoDTO::de).toList();
    }

    /** Se o turno não for indicado, usa o turno alocado ao artesão nesse dia (RF005). */
    public RegistoPontoDTO registarPonto(RegistoPontoPedido p) {
        Artesao artesao = pessoas.artesao(p.artesaoId());
        Turno turno = p.turnoId() != null
                ? gestorAlocacao.turno(p.turnoId())
                : gestorAlocacao.turnoDoArtesao(artesao.getId(), p.data()).orElseThrow(() ->
                        new IllegalStateException("O artesão não tem turno alocado nesse dia; indique o turno"));
        boolean jaExiste = pontos.findByArtesaoIdAndData(artesao.getId(), p.data()).stream()
                .anyMatch(x -> Objects.equals(x.getTurno().getId(), turno.getId()));
        if (jaExiste) {
            throw new IllegalStateException("Já existe um registo de ponto deste artesão nesse turno e dia");
        }
        return RegistoPontoDTO.de(pontos.save(new RegistoPonto(artesao, turno, p.data(), p.horaEntrada(), p.horaSaida())));
    }

    public void apagarPonto(Long id) {
        RegistoPonto p = pontos.findById(id).orElseThrow(() -> new NaoEncontradoException("Registo de ponto", id));
        pontos.delete(p);
    }
}
