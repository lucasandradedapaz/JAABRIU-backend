package jaabriu.jaabriu_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cada registro é UMA solução escrita por um técnico/admin. Um chamado pode
 * ter várias (reaberto -> nova solução), e NENHUMA é apagada/substituída —
 * é sempre um novo registro, formando o histórico de soluções (item 2 do
 * pedido).
 */
@Entity
@Table(name = "solucoes_chamado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolucaoChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    // Sequencial por chamado: Solução #1, #2, #3... (não é o ID do banco,
    // que é global e não teria essa contagem "por chamado").
    @Column(nullable = false)
    private Integer numero;

    // HTML já sanitizado (ver util.HtmlSanitizer) — só a formatação que o
    // editor permite (negrito, itálico, listas, alinhamento, link etc.)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    // Status do chamado no momento em que essa solução foi registrada,
    // pra dar contexto no histórico (ex: "Resolvido", "Fechado").
    @Enumerated(EnumType.STRING)
    @Column(name = "status_no_momento")
    private Status statusNoMomento;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
