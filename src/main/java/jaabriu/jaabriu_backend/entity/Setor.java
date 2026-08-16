package jaabriu.jaabriu_backend.entity;

public enum Setor {

    GEAS("GEAS"),
    OBRAS("Obras"),
    SERVICOS_PUBLICOS("Serviços Públicos");

    private final String descricao;

    Setor(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
