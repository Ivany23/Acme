package acme.modelo;

/** Perfis de acesso, correspondentes aos actores do diagrama de casos de uso (TG1, Tabela 1). */
public enum Papel {
    ADMINISTRADOR("Administrador"),
    DIRECTOR_GERAL("Director-Geral"),
    CONTADOR("Contador"),
    SUPERVISOR("Supervisor de Turno"),
    GERENTE_VENDAS("Gerente de Vendas"),
    ARTESAO("Artesão"),
    CLIENTE("Cliente");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean eFuncionario() {
        return this != CLIENTE;
    }
}
