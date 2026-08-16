package jaabriu.jaabriu_backend.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ChamadoFiltroRequest {

    private String status;

    private String prioridade;

    private String categoria;

    private String setor;

    private Long tecnicoId;

    private Long usuarioId;

    // Período de abertura do chamado (inclusive nas duas pontas)
    private LocalDate dataInicio;

    private LocalDate dataFim;
}