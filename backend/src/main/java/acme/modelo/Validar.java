package acme.modelo;

/** Pequenas validações reutilizadas pelas entidades. */
final class Validar {

    private Validar() {
    }

    static String obrigatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório");
        }
        return valor.trim();
    }

    static double naoNegativo(double valor, String campo) {
        if (valor < 0 || Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException("O campo '" + campo + "' não pode ser negativo");
        }
        return valor;
    }

    static <T> T naoNulo(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório");
        }
        return valor;
    }
}
