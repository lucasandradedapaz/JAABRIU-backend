package jaabriu.jaabriu_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long totalChamados;
    private Long chamadosAbertos;
    private Long chamadosEmAndamento;
    private Long chamadosResolvidos;
    private Long chamadosAtrasados;
    private Double mediaAvaliacoes;
}