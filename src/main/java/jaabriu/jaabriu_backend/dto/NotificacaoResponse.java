package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificacaoResponse {

    private Long id;
    private String titulo;
    private String mensagem;
    private String tipo;
    private Long chamadoId;
    private Boolean lida;
    private LocalDateTime createdAt;
}
