package acme.servico;

/** Lançada quando um registo pedido não existe (responde 404). */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String entidade, Object id) {
        super(entidade + " com id " + id + " não foi encontrado(a)");
    }
}
