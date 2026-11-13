package acme.servico;

/** Lançada quando o pedido não tem sessão válida ou o login falha (responde 401). */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
