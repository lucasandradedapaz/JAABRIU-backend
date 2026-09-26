package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtribuirSetorRequest {

    @NotBlank(message = "Informe o setor.")
    private String setor;
}
