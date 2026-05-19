package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class FecharChamadoRequest {

    @NotBlank(message = "Descrição da solução é obrigatória")
    private String descricaoSolucao;

    public FecharChamadoRequest() {
    }

    public String getDescricaoSolucao() {
        return descricaoSolucao;
    }

    public void setDescricaoSolucao(String descricaoSolucao) {
        this.descricaoSolucao = descricaoSolucao;
    }
}