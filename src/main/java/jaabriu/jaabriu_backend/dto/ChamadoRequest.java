package jaabriu.jaabriu_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChamadoRequest {

    @NotBlank
    private String titulo;

    @NotBlank
    private String descricao;

    // ❌ REMOVIDO usuarioId (vem do token agora)

    private Long tecnicoId; // pode continuar opcional

    // Status inicial não é mais informado pelo cliente: todo chamado
    // novo nasce como ABERTO (definido no service). Campo mantido só
    // para não quebrar payloads antigos, mas é ignorado.
    private String status;

    // Opcional: usuário comum não define prioridade (o service aplica
    // MÉDIA automaticamente). Técnico/admin podem enviar um valor.
    private String prioridade;

    @NotBlank
    private String categoria;
}