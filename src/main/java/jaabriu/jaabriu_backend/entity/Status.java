package jaabriu.jaabriu_backend.entity;

public enum Status {

    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    RESOLVIDO("Resolvido"),
    FECHADO("Fechado");

    private final String descricao;

    Status(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}