package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SlaConfiguracaoRequest {

    @NotNull(message = "Tempo de resposta é obrigatório")
    @Min(value = 1, message = "Tempo de resposta deve ser maior que zero")
    private Integer tempoRespostaMinutos;

    @NotNull(message = "Tempo de resolução é obrigatório")
    @Min(value = 1, message = "Tempo de resolução deve ser maior que zero")
    private Integer tempoResolucaoMinutos;
}
