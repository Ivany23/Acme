package acme.modelo;

import jakarta.persistence.*;

/** Etapa do processo de fabrico de uma peça (RF001). */
@Entity
@Table(name = "etapa_producao")
public class EtapaProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id")
    private Peca peca;

    @Column(nullable = false)
    private String descricao;

    private int ordem;

    protected EtapaProducao() {
    }

    EtapaProducao(Peca peca, String descricao, int ordem) {
        this.peca = peca;
        actualizar(descricao, ordem);
    }

    public void actualizar(String descricao, int ordem) {
        this.descricao = Validar.obrigatorio(descricao, "descricao");
        this.ordem = ordem;
    }

    public Long getId() { return id; }
    public Peca getPeca() { return peca; }
    public String getDescricao() { return descricao; }
    public int getOrdem() { return ordem; }
}
