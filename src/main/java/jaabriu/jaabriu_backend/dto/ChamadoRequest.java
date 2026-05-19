package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChamadoRequest {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descricao;

    // ❌ REMOVIDO usuarioId (vem do token agora)

    private Long tecnicoId; // pode continuar opcional

    @NotBlank
    private String status;

    @NotBlank
    private String prioridade;

    @NotBlank
    private String categoria;
}