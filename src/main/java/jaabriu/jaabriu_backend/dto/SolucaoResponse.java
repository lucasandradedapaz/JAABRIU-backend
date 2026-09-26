package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SolucaoResponse {

    private Long id;
    private Integer numero;

    private Long autorId;
    private String autorNome;

    // HTML já sanitizado, pronto pra renderizar
    private String conteudo;

    private String status;
    private String statusLabel;

    private LocalDateTime criadoEm;
}
