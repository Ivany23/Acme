package acme.modelo;

import jakarta.persistence.*;

/** Quantidade de um material necessária para fabricar UMA unidade de uma peça. */
@Entity
@Table(name = "material_peca")
public class MaterialPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id")
    private Peca peca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id")
    private Material material;

    private double quantidadePorUnidade;

    protected MaterialPeca() {
    }

    MaterialPeca(Peca peca, Material material, double quantidadePorUnidade) {
        this.peca = peca;
        this.material = Validar.naoNulo(material, "material");
        this.quantidadePorUnidade = Validar.naoNegativo(quantidadePorUnidade, "quantidadePorUnidade");
    }

    public double custo() {
        return material.calcularCusto(quantidadePorUnidade);
    }

    public Long getId() { return id; }
    public Material getMaterial() { return material; }
    public double getQuantidadePorUnidade() { return quantidadePorUnidade; }
}
