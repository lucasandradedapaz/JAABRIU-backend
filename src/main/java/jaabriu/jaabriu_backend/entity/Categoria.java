package jaabriu.jaabriu_backend.entity;

public enum Categoria {
    INFRAESTRUTURA,
    SOFTWARE,
    HARDWARE,
    REDE,
    ACESSO,
    OUTROS;

    public String getNome() {
        return name();
    }
}