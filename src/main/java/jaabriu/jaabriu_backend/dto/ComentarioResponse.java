package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ComentarioResponse {

    private Long id;

    private String mensagem;

    private Boolean interno;

    private Long autorId;

    private String autorNome;

    private String autorPerfil;

    private Long chamadoId;

    private LocalDateTime createdAt;
}