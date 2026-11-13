package acme.modelo;

import acme.observer.ObservadorEncomenda;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Encomenda de um cliente (RF002). É o SUJEITO observado (padrão Observer). */
@Entity
@Table(name = "encomenda")
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id")
    private Peca peca;

    private int quantidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEncomenda estado = EstadoEncomenda.PENDENTE;

    private double custosIndirectos;

    private double custoEstimado;

    private boolean paga;

    @Column(nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    /** Os observadores não são guardados na base de dados: o serviço liga-os a cada operação. */
    @Transient
    private final List<ObservadorEncomenda> observadores = new ArrayList<>();

    protected Encomenda() {
    }

    public Encomenda(Cliente cliente, Peca peca, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        this.cliente = Validar.naoNulo(cliente, "cliente");
        this.peca = Validar.naoNulo(peca, "peca");
        this.quantidade = quantidade;
    }

    public void adicionarObservador(ObservadorEncomenda observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    /** Muda o estado (respeitando as transições válidas) e avisa todos os observadores. */
    public void actualizarEstado(EstadoEncomenda novoEstado) {
        Validar.naoNulo(novoEstado, "estado");
        if (!estado.podeMudarPara(novoEstado)) {
            throw new IllegalStateException("Não é possível passar a encomenda de " + estado + " para " + novoEstado);
        }
        this.estado = novoEstado;
        for (ObservadorEncomenda o : List.copyOf(observadores)) {
            o.estadoAlterado(this);
        }
    }

    /** Após o pagamento a encomenda entra em produção. */
    public void confirmarPagamento() {
        if (paga) {
            throw new IllegalStateException("A encomenda já foi paga");
        }
        if (estado != EstadoEncomenda.PENDENTE) {
            throw new IllegalStateException("Só é possível pagar encomendas pendentes");
        }
        this.paga = true;
        actualizarEstado(EstadoEncomenda.EM_PRODUCAO);
    }

    public void definirOrcamento(double custosIndirectos, double custoEstimado) {
        this.custosIndirectos = Validar.naoNegativo(custosIndirectos, "custosIndirectos");
        this.custoEstimado = custoEstimado;
    }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Peca getPeca() { return peca; }
    public int getQuantidade() { return quantidade; }
    public EstadoEncomenda getEstado() { return estado; }
    public double getCustosIndirectos() { return custosIndirectos; }
    public double getCustoEstimado() { return custoEstimado; }
    public boolean isPaga() { return paga; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
