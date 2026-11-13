package acme.servico;

import acme.modelo.Artesao;
import acme.modelo.Cliente;
import acme.modelo.Funcionario;
import acme.repositorio.*;
import acme.web.dto.Pedidos.ClientePedido;
import acme.web.dto.Pedidos.FuncionarioPedido;
import acme.web.dto.Pedidos.TipoFuncionario;
import acme.web.dto.Respostas.ClienteDTO;
import acme.web.dto.Respostas.FuncionarioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Clientes (RF002) e funcionários/artesãos (RF003). */
@Service
@Transactional
public class PessoasService {

    private final ClienteRepository clientes;
    private final FuncionarioRepository funcionarios;
    private final ArtesaoRepository artesaos;
    private final EncomendaRepository encomendas;
    private final AlocacaoTurnoRepository alocacoes;
    private final RegistoProducaoRepository registos;
    private final RegistoPontoRepository pontos;

    public PessoasService(ClienteRepository clientes, FuncionarioRepository funcionarios, ArtesaoRepository artesaos,
                          EncomendaRepository encomendas, AlocacaoTurnoRepository alocacoes,
                          RegistoProducaoRepository registos, RegistoPontoRepository pontos) {
        this.clientes = clientes;
        this.funcionarios = funcionarios;
        this.artesaos = artesaos;
        this.encomendas = encomendas;
        this.alocacoes = alocacoes;
        this.registos = registos;
        this.pontos = pontos;
    }

    // ---------- Clientes ----------

    @Transactional(readOnly = true)
    public List<ClienteDTO> listarClientes() {
        return clientes.findAllByOrderByNomeAsc().stream().map(ClienteDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO obterCliente(Long id) {
        return ClienteDTO.de(cliente(id));
    }

    @Transactional(readOnly = true)
    public List<String> notificacoes(Long id) {
        return List.copyOf(cliente(id).getNotificacoes());
    }

    public ClienteDTO criarCliente(ClientePedido p) {
        return ClienteDTO.de(clientes.save(new Cliente(p.nome(), vazioParaNulo(p.email()), p.telefone())));
    }

    public ClienteDTO actualizarCliente(Long id, ClientePedido p) {
        Cliente c = cliente(id);
        c.actualizar(p.nome(), vazioParaNulo(p.email()), p.telefone());
        return ClienteDTO.de(c);
    }

    public void apagarCliente(Long id) {
        Cliente c = cliente(id);
        if (encomendas.existsByClienteId(id)) {
            throw new IllegalStateException("O cliente tem encomendas e não pode ser apagado");
        }
        clientes.delete(c);
        clientes.flush();
    }

    public Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
    }

    // ---------- Funcionários ----------

    @Transactional(readOnly = true)
    public List<FuncionarioDTO> listarFuncionarios() {
        return funcionarios.findAllByOrderByNomeAsc().stream().map(FuncionarioDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioDTO> listarArtesaos() {
        return artesaos.findAllByOrderByIdAsc().stream().map(FuncionarioDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioDTO obterFuncionario(Long id) {
        return FuncionarioDTO.de(funcionario(id));
    }

    public FuncionarioDTO criarFuncionario(FuncionarioPedido p) {
        Funcionario f = p.tipo() == TipoFuncionario.ARTESAO
                ? new Artesao(p.nome(), p.telefone(), p.endereco(), p.especialidade())
                : new Funcionario(p.nome(), p.cargo(), p.telefone(), p.endereco());
        return FuncionarioDTO.de(funcionarios.save(f));
    }

    public FuncionarioDTO actualizarFuncionario(Long id, FuncionarioPedido p) {
        Funcionario f = funcionario(id);
        boolean eArtesao = f instanceof Artesao;
        if (eArtesao != (p.tipo() == TipoFuncionario.ARTESAO)) {
            throw new IllegalArgumentException("Não é possível mudar o tipo do funcionário; crie um novo registo");
        }
        if (f instanceof Artesao a) {
            a.actualizarArtesao(p.nome(), p.telefone(), p.endereco(), p.especialidade());
        } else {
            f.actualizarDados(p.nome(), p.cargo(), p.telefone(), p.endereco());
        }
        return FuncionarioDTO.de(f);
    }

    public void apagarFuncionario(Long id) {
        Funcionario f = funcionario(id);
        if (alocacoes.existsByArtesaoId(id) || registos.existsByArtesaoId(id) || pontos.existsByArtesaoId(id)) {
            throw new IllegalStateException("O artesão tem turnos, produção ou ponto registados e não pode ser apagado");
        }
        funcionarios.delete(f);
        funcionarios.flush();
    }

    public Funcionario funcionario(Long id) {
        return funcionarios.findById(id).orElseThrow(() -> new NaoEncontradoException("Funcionário", id));
    }

    public Artesao artesao(Long id) {
        return artesaos.findById(id).orElseThrow(() -> new NaoEncontradoException("Artesão", id));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
