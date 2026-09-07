package jaabriu.jaabriu_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_configuracoes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfiguracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private Prioridade prioridade;

    @Column(name = "tempo_resposta_minutos", nullable = false)
    private Integer tempoRespostaMinutos;

    @Column(name = "tempo_resolucao_minutos", nullable = false)
    private Integer tempoResolucaoMinutos;
}
