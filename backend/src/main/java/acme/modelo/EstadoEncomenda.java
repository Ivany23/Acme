package acme.modelo;

import java.util.Set;

public enum EstadoEncomenda {
    PENDENTE, EM_PRODUCAO, CONCLUIDA, CANCELADA;

    /** Transições permitidas: PENDENTE -> EM_PRODUCAO/CANCELADA; EM_PRODUCAO -> CONCLUIDA/CANCELADA. */
    public boolean podeMudarPara(EstadoEncomenda novo) {
        return switch (this) {
            case PENDENTE -> Set.of(EM_PRODUCAO, CANCELADA).contains(novo);
            case EM_PRODUCAO -> Set.of(CONCLUIDA, CANCELADA).contains(novo);
            case CONCLUIDA, CANCELADA -> false;
        };
    }
}
