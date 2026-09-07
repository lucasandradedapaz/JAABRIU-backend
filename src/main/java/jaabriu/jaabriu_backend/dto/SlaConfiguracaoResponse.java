package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlaConfiguracaoResponse {
    private String prioridade;
    private Integer tempoRespostaMinutos;
    private Integer tempoResolucaoMinutos;
}
