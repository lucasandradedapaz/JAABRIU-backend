package jaabriu.jaabriu_backend.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chamados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "descricao_solucao", columnDefinition = "TEXT")
    private String descricaoSolucao;

    // 👤 Usuário que abriu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // 🛠 Técnico responsável
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    // 🏢 Setor responsável pelo chamado (atribuído por admin/técnico —
    // substitui a antiga lógica de "técnico que auxiliou"; ver item 1 do
    // pedido). Reaproveita o mesmo enum Setor já usado pelo setor pessoal
    // do usuário, evitando duplicar estrutura.
    @Enumerated(EnumType.STRING)
    @Column(name = "setor_responsavel", length = 30)
    private Setor setorResponsavel;

    // 👥 Técnicos atribuídos ao chamado — pode ser mais de um (item 1.3).
    // Todos eles "possuem" o chamado, recebem em tempo real quando são
    // atribuídos/removidos, e aparecem na lista de chamados deles.
    @Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chamado_tecnicos",
            joinColumns = @JoinColumn(name = "chamado_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> tecnicosAtribuidos = new HashSet<>();

    // ✅ ENUMS
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "sla_inicio")
    private LocalDateTime slaInicio;

    @Column(name = "sla_fim")
    private LocalDateTime slaFim;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Default
    @Column(nullable = false)
    private Boolean atrasado = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 💬 Comentários
    @Default
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comentario> comentarios = new ArrayList<>();

    // 📎 Anexos
    @Default
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anexo> anexos = new ArrayList<>();

    // ⭐ Avaliações
    @Default
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    // 📜 Histórico
    @Default
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistoricoAlteracao> historicos = new ArrayList<>();

    // 🧾 Histórico de soluções — nunca apagado/substituído, sempre um novo
    // registro (item 2 do pedido). O antigo campo "descricaoSolucao" acima
    // é preservado só pra não quebrar quem já lia esse campo (impressão,
    // por ex.) e passa a espelhar automaticamente a solução mais recente.
    @Default
    @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolucaoChamado> solucoes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dataAbertura == null) {
            dataAbertura = LocalDateTime.now();
        }

        if (status == null) {
            status = Status.ABERTO;
        }

        if (categoria == null) {
            categoria = Categoria.OUTROS;
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}