package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChamadoResponse {

    private Long id;
    private String titulo;
    private String descricao;
    private String descricaoSolucao;

    private String usuarioNome;
    private String tecnicoNome;

    private String status;
    private String prioridade;
    private String categoria;

    private LocalDateTime slaInicio;
    private LocalDateTime slaFim;
    private LocalDateTime dataFechamento;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}