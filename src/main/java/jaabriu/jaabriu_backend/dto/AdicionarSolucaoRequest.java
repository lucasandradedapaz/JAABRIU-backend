package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdicionarSolucaoRequest {

    // HTML vindo do editor de texto rico (é sanitizado no service antes de
    // salvar — ver util.HtmlSanitizer)
    @NotBlank(message = "Descreva a solução antes de salvar.")
    private String conteudo;
}
