package acme.observer;

import acme.modelo.Encomenda;

/** Padrão OBSERVER: quem quer ser avisado quando uma encomenda muda de estado. */
public interface ObservadorEncomenda {
    void estadoAlterado(Encomenda encomenda);
}
