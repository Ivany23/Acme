package acme.servico;

/** Lançada quando o utilizador está autenticado mas o seu papel não permite a operação (responde 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
