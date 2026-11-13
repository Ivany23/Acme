package acme.modelo;

import jakarta.persistence.*;

/** Matéria-prima usada no fabrico das peças (RF001, RF006). */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String unidadeMedida;

    private double custoUnitario;

    protected Material() {
    }

    public Material(String nome, String unidadeMedida, double custoUnitario) {
        actualizar(nome, unidadeMedida, custoUnitario);
    }

    public void actualizar(String nome, String unidadeMedida, double custoUnitario) {
        this.nome = Validar.obrigatorio(nome, "nome");
        this.unidadeMedida = Validar.obrigatorio(unidadeMedida, "unidadeMedida");
        this.custoUnitario = Validar.naoNegativo(custoUnitario, "custoUnitario");
    }

    public double calcularCusto(double quantidade) {
        return quantidade * custoUnitario;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public double getCustoUnitario() { return custoUnitario; }
}
