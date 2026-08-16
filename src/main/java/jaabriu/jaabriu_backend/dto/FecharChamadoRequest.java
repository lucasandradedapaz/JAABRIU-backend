package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FecharChamadoRequest {

    @NotBlank(message = "Descrição da solução é obrigatória")
    private String descricaoSolucao;

    @NotNull(message = "Informe o técnico que auxiliou no atendimento antes de finalizar o chamado.")
    private Long tecnicoAtribuidoId;

    public FecharChamadoRequest() {
    }

    public String getDescricaoSolucao() {
        return descricaoSolucao;
    }

    public void setDescricaoSolucao(String descricaoSolucao) {
        this.descricaoSolucao = descricaoSolucao;
    }

    public Long getTecnicoAtribuidoId() {
        return tecnicoAtribuidoId;
    }

    public void setTecnicoAtribuidoId(Long tecnicoAtribuidoId) {
        this.tecnicoAtribuidoId = tecnicoAtribuidoId;
    }
}