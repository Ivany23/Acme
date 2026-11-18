package acme.seguranca;

import acme.modelo.Papel;

import java.util.Arrays;

/** Decide se um papel pode usar um endpoint (controlo de acesso baseado em papéis). */
public final class RegrasAcesso {

    private RegrasAcesso() {
    }

    /**
     * Regras:
     * 1. Sem anotação -> ninguém tem acesso (negado por omissão).
     * 2. O administrador acede a tudo o que é de funcionários, mas não à área de cliente.
     * 3. "funcionarios = true" autoriza qualquer papel de funcionário.
     * 4. Caso contrário, o papel tem de constar da lista.
     */
    public static boolean permitido(Papel papel, Acesso acesso) {
        if (papel == null || acesso == null) {
            return false;
        }
        boolean listado = Arrays.asList(acesso.value()).contains(papel);
        if (listado) {
            return true;
        }
        if (acesso.funcionarios() && papel.eFuncionario()) {
            return true;
        }
        boolean paraClientes = Arrays.asList(acesso.value()).contains(Papel.CLIENTE);
        return papel == Papel.ADMINISTRADOR && !paraClientes && (acesso.funcionarios() || acesso.value().length > 0);
    }
}
