package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ComentarioRequest {

    @NotBlank(message = "A mensagem do comentário é obrigatória")
    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
    private String mensagem;

    private Boolean interno = false;
}