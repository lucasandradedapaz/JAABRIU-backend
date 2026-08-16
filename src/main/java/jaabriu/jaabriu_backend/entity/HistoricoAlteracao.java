package jaabriu.jaabriu_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_alteracoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoAlteracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_alteracao", nullable = false)
    private TipoAlteracao tipoAlteracao;

    @Column(name = "campo_antigo")
    private String campoAntigo;

    @Column(name = "campo_novo")
    private String campoNovo;

    private String descricao;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}