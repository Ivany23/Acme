package acme.modelo;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Artesão: funcionário que fabrica as peças. */
@Entity
@DiscriminatorValue("ARTESAO")
public class Artesao extends Funcionario {

    public static final String CARGO = "Artesão";

    private String especialidade;

    protected Artesao() {
    }

    public Artesao(String nome, String telefone, String endereco, String especialidade) {
        super(nome, CARGO, telefone, endereco);
        this.especialidade = especialidade;
    }

    public void actualizarArtesao(String nome, String telefone, String endereco, String especialidade) {
        actualizarDados(nome, CARGO, telefone, endereco);
        this.especialidade = especialidade;
    }

    public String getEspecialidade() { return especialidade; }
}
