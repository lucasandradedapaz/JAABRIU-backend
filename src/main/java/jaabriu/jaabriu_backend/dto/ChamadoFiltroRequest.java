package jaabriu.jaabriu_backend.dto;

import lombok.Data;

@Data
public class ChamadoFiltroRequest {

    private String status;

    private String prioridade;

    private Long tecnicoId;

    private Long usuarioId;
}