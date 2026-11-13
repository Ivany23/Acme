package acme.modelo;

import jakarta.persistence.*;

/** Utilizador que pode entrar no sistema (login). A password é guardada apenas como hash BCrypt. */
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

    protected Utilizador() {
    }

    public Utilizador(String login, String nome, String passwordHash) {
        this.login = Validar.obrigatorio(login, "login").toLowerCase();
        this.nome = Validar.obrigatorio(nome, "nome");
        this.passwordHash = Validar.obrigatorio(passwordHash, "passwordHash");
    }

    public void alterarPasswordHash(String novoHash) {
        this.passwordHash = Validar.obrigatorio(novoHash, "passwordHash");
    }

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getNome() { return nome; }
    public String getPasswordHash() { return passwordHash; }
}
