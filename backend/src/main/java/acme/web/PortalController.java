package acme.web;

import acme.config.SegurancaConfig;
import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.servico.PortalClienteService;
import acme.servico.ServicoAutenticacao;
import acme.web.dto.Pedidos.EncomendaClientePedido;
import acme.web.dto.Pedidos.RegistoClientePedido;
import acme.web.dto.Respostas.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Área do cliente (actor Cliente do TG1). Cada cliente só acede aos seus dados. */
@RestController
@RequestMapping("/api/portal")
@Acesso(Papel.CLIENTE)
public class PortalController {

    private final PortalClienteService servico;
    private final ServicoAutenticacao autenticacao;

    public PortalController(PortalClienteService servico, ServicoAutenticacao autenticacao) {
        this.servico = servico;
        this.autenticacao = autenticacao;
    }

    /** Público: caso de uso "Registar Conta de Cliente". */
    @PostMapping("/registar")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoDTO registar(@Valid @RequestBody RegistoClientePedido pedido) {
        return autenticacao.registarCliente(pedido);
    }

    @GetMapping("/eu")
    public ClienteDTO eu(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u) {
        return servico.dados(u.clienteId());
    }

    @GetMapping("/pecas")
    public List<PecaCatalogoDTO> catalogo() {
        return servico.catalogo();
    }

    /** Caso de uso "Solicitar Orçamento de Encomenda" (RF008). */
    @GetMapping("/orcamento")
    public OrcamentoDTO orcamento(@RequestParam Long pecaId, @RequestParam int quantidade) {
        return servico.orcamento(pecaId, quantidade);
    }

    @PostMapping("/encomendas")
    @ResponseStatus(HttpStatus.CREATED)
    public EncomendaDTO encomendar(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                                   @Valid @RequestBody EncomendaClientePedido pedido) {
        return servico.encomendar(u.clienteId(), pedido.pecaId(), pedido.quantidade());
    }

    /** Caso de uso "Acompanhar Estado da Encomenda". */
    @GetMapping("/encomendas")
    public List<EncomendaDTO> minhasEncomendas(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u) {
        return servico.minhasEncomendas(u.clienteId());
    }

    @PostMapping("/encomendas/{id}/cancelar")
    public EncomendaDTO cancelar(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u,
                                 @PathVariable Long id) {
        return servico.cancelar(u.clienteId(), id);
    }

    /** Notificações recebidas através do padrão Observer. */
    @GetMapping("/notificacoes")
    public List<String> notificacoes(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO u) {
        return servico.notificacoes(u.clienteId());
    }
}
