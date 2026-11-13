package acme.modelo;

import jakarta.persistence.*;

/** Funcionário contratado (RF003). */
@Entity
@Table(name = "funcionario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo", length = 20)
@DiscriminatorValue("FUNCIONARIO")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cargo;

    private String telefone;

    private String endereco;

    protected Funcionario() {
    }

    public Funcionario(String nome, String cargo, String telefone, String endereco) {
        actualizarDados(nome, cargo, telefone, endereco);
    }

    public void actualizarDados(String nome, String cargo, String telefone, String endereco) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        this.nome = nome.trim();
        this.cargo = Validar.obrigatorio(cargo, "cargo");
        this.telefone = telefone;
        this.endereco = endereco;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
}
