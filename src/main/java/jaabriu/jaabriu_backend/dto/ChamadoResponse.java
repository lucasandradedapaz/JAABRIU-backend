package jaabriu.jaabriu_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChamadoResponse {

    private Long id;
    private String titulo;
    private String descricao;

    // Mantido por compatibilidade (telas/impressão que já liam esse campo):
    // sempre espelha o conteúdo da solução mais recente do histórico abaixo.
    private String descricaoSolucao;

    private String usuarioNome;
    private Long tecnicoId;
    private String tecnicoNome;

    // Setor pessoal de quem abriu o chamado (já existia)
    private String setor;

    // NOVO — setor responsável pelo chamado, atribuído por admin/técnico
    private String setorResponsavel;
    private String setorResponsavelLabel;

    // NOVO — técnicos atribuídos ao chamado (pode ser mais de um)
    @Builder.Default
    private List<UsuarioResponse> tecnicosAtribuidos = List.of();

    // NOVO — histórico completo de soluções, mais antiga primeiro
    @Builder.Default
    private List<SolucaoResponse> solucoes = List.of();

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