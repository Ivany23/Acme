package acme.modelo;

import acme.observer.ObservadorEncomenda;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** Cliente que encomenda peças. É um OBSERVADOR das suas encomendas (padrão Observer). */
@Entity
@Table(name = "cliente")
public class Cliente implements ObservadorEncomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String email;

    private String telefone;

    @ElementCollection
    @CollectionTable(name = "cliente_notificacao", joinColumns = @JoinColumn(name = "cliente_id"))
    @OrderColumn(name = "posicao")
    @Column(name = "mensagem", nullable = false, length = 500)
    private List<String> notificacoes = new ArrayList<>();

    protected Cliente() {
    }

    public Cliente(String nome, String email, String telefone) {
        actualizar(nome, email, telefone);
    }

    public void actualizar(String nome, String email, String telefone) {
        this.nome = Validar.obrigatorio(nome, "nome");
        this.email = email;
        this.telefone = telefone;
    }

    @Override
    public void estadoAlterado(Encomenda encomenda) {
        notificacoes.add("Encomenda #" + encomenda.getId() + " está agora " + encomenda.getEstado());
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public List<String> getNotificacoes() { return notificacoes; }
}
