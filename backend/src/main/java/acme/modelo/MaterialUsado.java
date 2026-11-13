package acme.modelo;

import jakarta.persistence.*;

/** Quantidade de material efectivamente consumida num registo de produção (RF006). */
@Entity
@Table(name = "material_usado")
public class MaterialUsado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registo_id")
    private RegistoProducao registo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    private double quantidade;

    protected MaterialUsado() {
    }

    MaterialUsado(RegistoProducao registo, Material material, double quantidade) {
        this.registo = registo;
        this.material = Validar.naoNulo(material, "material");
        this.quantidade = Validar.naoNegativo(quantidade, "quantidade");
    }

    public double custo() {
        return material.calcularCusto(quantidade);
    }

    public Long getId() { return id; }
    public Material getMaterial() { return material; }
    public double getQuantidade() { return quantidade; }
}
