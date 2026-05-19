package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HistoricoResponse {

    private Long id;

    private String descricao;

    private Long usuarioId;

    private String usuarioNome;

    private Long chamadoId;

    private LocalDateTime createdAt;
}