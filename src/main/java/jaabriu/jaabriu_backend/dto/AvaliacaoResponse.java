package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AvaliacaoResponse {

    private Long id;

    private Integer nota;

    private String comentario;

    private Long chamadoId;

    private Long usuarioId;

    private String usuarioNome;

    private LocalDateTime createdAt;
}