package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtribuirTecnicoRequest {

    @NotNull(message = "Informe o técnico.")
    private Long tecnicoId;
}
