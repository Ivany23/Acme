package acme.web;

import acme.config.SegurancaConfig;
import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.servico.AcessoNegadoException;
import acme.servico.ProducaoService;
import acme.web.dto.Pedidos.RegistoPontoPedido;
import acme.web.dto.Pedidos.RegistoProducaoPedido;
import acme.web.dto.Respostas.RegistoPontoDTO;
import acme.web.dto.Respostas.RegistoProducaoDTO;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ProducaoController {

    private final ProducaoService servico;

    public ProducaoController(ProducaoService servico) {
        this.servico = servico;
    }

    /** Um artesão só regista (e apaga) a sua própria produção e o seu próprio ponto. */
    static void verificarProprio(UtilizadorDTO u, Long artesaoId) {
        if (u.papel() == Papel.ARTESAO && !Objects.equals(u.funcionarioId(), artesaoId)) {
            throw new AcessoNegadoException("Um artesão só pode registar a sua própria produção e o seu ponto");
        }
    }

    @GetMapping("/producao")
    @Acesso(funcionarios = true)
    public List<RegistoProducaoDTO> listarRegistos(@RequestParam(required = false) Long encomendaId) {
        return servico.listarRegistos(encomendaId);
    }

    @PostMapping("/producao")
    @ResponseStatus(HttpStatus.CREATED)
    @Acesso({Papel.SUPERVISOR, Papel.ARTESAO})
    public RegistoProducaoDTO registar(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                                       @Valid @RequestBody RegistoProducaoPedido pedido) {
        verificarProprio(u, pedido.artesaoId());
        return servico.registarProducao(pedido);
    }

    @DeleteMapping("/producao/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Acesso({Papel.SUPERVISOR, Papel.ARTESAO})
    public void apagarRegisto(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                              @PathVariable Long id) {
        verificarProprio(u, servico.artesaoDoRegisto(id));
        servico.apagarRegisto(id);
    }

    @GetMapping("/ponto")
    @Acesso(funcionarios = true)
    public List<RegistoPontoDTO> listarPontos(
            @RequestParam(required = false) Long artesaoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.listarPontos(artesaoId, data);
    }

    @PostMapping("/ponto")
    @ResponseStatus(HttpStatus.CREATED)
    @Acesso({Papel.SUPERVISOR, Papel.ARTESAO})
    public RegistoPontoDTO registarPonto(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                                         @Valid @RequestBody RegistoPontoPedido pedido) {
        verificarProprio(u, pedido.artesaoId());
        return servico.registarPonto(pedido);
    }

    @DeleteMapping("/ponto/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Acesso({Papel.SUPERVISOR, Papel.ARTESAO})
    public void apagarPonto(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                            @PathVariable Long id) {
        verificarProprio(u, servico.artesaoDoPonto(id));
        servico.apagarPonto(id);
    }
}
