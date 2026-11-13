package acme;

import acme.modelo.*;

import java.time.LocalTime;

/** Objectos de teste reutilizados pelos testes unitários (sem base de dados). */
public final class Dados {

    private Dados() {
    }

    public static Material madeira() {
        return new Material("Madeira", "kg", 50);
    }

    /** Cadeira: 1000 MT por unidade + 5 kg de madeira (250 MT) por unidade. */
    public static Peca cadeira(Material madeira) {
        Peca p = new Peca("Cadeira", "45x45x90 cm", 1000);
        p.adicionarMaterial(madeira, 5);
        p.adicionarEtapa("Montagem", 2);
        p.adicionarEtapa("Corte", 1);
        return p;
    }

    public static Cliente cliente() {
        return new Cliente("Ana", "ana@mail.com", "845556677");
    }

    public static Turno matutino() {
        return new Turno("Matutino", LocalTime.of(7, 0), LocalTime.of(15, 0));
    }

    public static Turno vespertino() {
        return new Turno("Vespertino", LocalTime.of(15, 0), LocalTime.of(23, 0));
    }

    public static Artesao artesao(String nome) {
        return new Artesao(nome, "841234567", "Maputo", "Carpintaria");
    }
}
