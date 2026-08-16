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
    private Long tecnicoId;
    private String tecnicoNome;
    private Long tecnicoAtribuidoId;
    private String tecnicoAtribuidoNome;
    private String setor;

    private String status;
    private String prioridade;
    private String categoria;

    private LocalDateTime slaInicio;
    private LocalDateTime slaFim;
    private LocalDateTime dataFechamento;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean atrasado;
}