package acme.modelo;

import jakarta.persistence.*;

/**
 * Conta de acesso ao sistema (login). Pode pertencer a um funcionário (com o respectivo papel)
 * ou a um cliente (papel CLIENTE). A password é guardada apenas como hash BCrypt.
 */
@Entity
@Table(name = "utilizador")
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String login;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String passwordHash;

    /** Nulo apenas em contas antigas, criadas antes de existirem papéis (tratadas como administrador). */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    protected Utilizador() {
    }

    /** Conta de funcionário sem pessoa associada (ex.: administrador, director). */
    public Utilizador(String login, String nome, String passwordHash, Papel papel) {
        this(login, nome, passwordHash, papel, null, null);
    }

    public Utilizador(String login, String nome, String passwordHash, Papel papel,
                      Funcionario funcionario, Cliente cliente) {
        this.login = Validar.obrigatorio(login, "login").trim().toLowerCase();
        this.passwordHash = Validar.obrigatorio(passwordHash, "passwordHash");
        actualizar(nome, papel, funcionario, cliente);
    }

    public static Utilizador deCliente(Cliente cliente, String login, String passwordHash) {
        Validar.naoNulo(cliente, "cliente");
        return new Utilizador(login, cliente.getNome(), passwordHash, Papel.CLIENTE, null, cliente);
    }

    /** Define nome, papel e a pessoa associada, garantindo a coerência entre eles. */
    public void actualizar(String nome, Papel papel, Funcionario funcionario, Cliente cliente) {
        Validar.naoNulo(papel, "papel");
        if (papel == Papel.CLIENTE) {
            Validar.naoNulo(cliente, "cliente");
            if (funcionario != null) {
                throw new IllegalArgumentException("Uma conta de cliente não pode estar associada a um funcionário");
            }
        } else if (cliente != null) {
            throw new IllegalArgumentException("Uma conta de funcionário não pode estar associada a um cliente");
        }
        if (papel == Papel.ARTESAO && !(funcionario instanceof Artesao)) {
            throw new IllegalArgumentException("Uma conta de artesão tem de estar associada a um artesão registado");
        }
        this.nome = Validar.obrigatorio(nome, "nome");
        this.papel = papel;
        this.funcionario = funcionario;
        this.cliente = cliente;
    }

    public void alterarPasswordHash(String novoHash) {
        this.passwordHash = Validar.obrigatorio(novoHash, "passwordHash");
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public String getPasswordHash() { return passwordHash; }
    public Papel getPapel() { return papel == null ? Papel.ADMINISTRADOR : papel; }
    public boolean temPapelDefinido() { return papel != null; }
    public Funcionario getFuncionario() { return funcionario; }
    public Cliente getCliente() { return cliente; }
}
