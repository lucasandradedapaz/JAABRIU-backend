package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SetorOpcaoResponse {

    // Valor a enviar de volta pro backend (nome do enum, ex: "GEAS")
    private String valor;

    // Texto amigável pra exibir na sugestão (ex: "Serviços Públicos")
    private String label;
}
