package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DefinirPrioridadeRequest {

    @NotBlank(message = "Prioridade é obrigatória")
    private String prioridade;
}