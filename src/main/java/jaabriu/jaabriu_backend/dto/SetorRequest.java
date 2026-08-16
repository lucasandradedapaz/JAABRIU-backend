package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetorRequest {

    @NotBlank(message = "Setor é obrigatório.")
    private String setor;
}
