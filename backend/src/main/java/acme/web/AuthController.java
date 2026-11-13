package acme.web;

import acme.config.SegurancaConfig;
import acme.servico.AuthService;
import acme.web.dto.Pedidos.LoginPedido;
import acme.web.dto.Respostas.SessaoDTO;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService servico;

    public AuthController(AuthService servico) {
        this.servico = servico;
    }

    @PostMapping("/login")
    public SessaoDTO login(@Valid @RequestBody LoginPedido pedido) {
        return servico.login(pedido.login(), pedido.password());
    }

    /** Devolve o utilizador da sessão actual. */
    @GetMapping("/eu")
    public UtilizadorDTO eu(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO utilizador) {
        return utilizador;
    }
}
