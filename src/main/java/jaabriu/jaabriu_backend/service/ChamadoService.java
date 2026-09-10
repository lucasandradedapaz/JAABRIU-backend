package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.DefinirPrioridadeRequest;
import jaabriu.jaabriu_backend.dto.EditarChamadoRequest;
import jaabriu.jaabriu_backend.dto.FecharChamadoRequest;
import jaabriu.jaabriu_backend.entity.*;
import jaabriu.jaabriu_backend.exception.BusinessException;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;
    private final NotificacaoService notificacaoService;
    private final SlaConfiguracaoService slaConfiguracaoService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService,
            NotificacaoService notificacaoService,
            SlaConfiguracaoService slaConfiguracaoService
    ) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
        this.notificacaoService = notificacaoService;
        this.slaConfiguracaoService = slaConfiguracaoService;
    }

    public ChamadoResponse criar(Long usuarioId, ChamadoRequest request) {

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Categoria categoria = Categoria.valueOf(request.getCategoria().toUpperCase());

        // Todo chamado nasce ABERTO — o cliente não decide o status inicial
        Status status = Status.ABERTO;

        // Regra de prioridade por perfil:
        // - USUARIO comum NÃO define prioridade -> fica em branco (aguardando
        //   triagem do técnico/admin). O relógio do SLA só começa a contar
        //   quando alguém definir a prioridade.
        // - TECNICO/ADMIN podem enviar a prioridade já na criação
        Prioridade prioridade = null;
        if (usuario.getPerfil() != Usuario.Perfil.USUARIO) {
            if (request.getPrioridade() != null && !request.getPrioridade().isBlank()) {
                prioridade = Prioridade.valueOf(request.getPrioridade().toUpperCase());
            } else {
                prioridade = Prioridade.MEDIA;
            }
        }

        Chamado chamado = new Chamado();
        chamado.setTitulo(request.getTitulo());
        chamado.setDescricao(request.getDescricao());
        chamado.setUsuario(usuario);
        chamado.setStatus(status);
        chamado.setPrioridade(prioridade);
        chamado.setCategoria(categoria);
        chamado.setCreatedAt(LocalDateTime.now());
        chamado.setUpdatedAt(LocalDateTime.now());

        // Só começa a contar o SLA se já existir prioridade definida
        if (prioridade != null) {
            chamado.setSlaInicio(LocalDateTime.now());
            chamado.setSlaFim(
                    LocalDateTime.now().plusMinutes(
                            slaConfiguracaoService.minutosResolucaoPara(prioridade)
                    )
            );
        }

        Chamado salvo = chamadoRepository.save(chamado);

        historicoService.registrar(
                salvo,
                usuario,
                "Chamado criado",
                TipoAlteracao.OUTRO
        );

        return mapToResponse(salvo);
    }

    // usuarioLogadoId/perfil: usuário comum só vê os próprios chamados;
    // técnico e admin veem todos.
    public List<ChamadoResponse> listarTodos(Long usuarioLogadoId, Usuario.Perfil perfil) {
        List<Chamado> chamados = chamadoRepository.findAll();

        if (perfil == Usuario.Perfil.USUARIO) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getUsuario() != null
                            && ch.getUsuario().getId().equals(usuarioLogadoId))
                    .toList();
        }

        return chamados.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<ChamadoResponse> listarPorFiltro(
            ChamadoFiltroRequest filtro,
            Long usuarioLogadoId,
            Usuario.Perfil perfil
    ) {

        List<Chamado> chamados = chamadoRepository.findAll();

        if (perfil == Usuario.Perfil.USUARIO) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getUsuario() != null
                            && ch.getUsuario().getId().equals(usuarioLogadoId))
                    .toList();
        }

        if (filtro.getStatus() != null && !filtro.getStatus().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getStatus() != null
                            && ch.getStatus().name().equalsIgnoreCase(filtro.getStatus()))
                    .toList();
        }

        if (filtro.getPrioridade() != null && !filtro.getPrioridade().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getPrioridade() != null
                            && ch.getPrioridade().name().equalsIgnoreCase(filtro.getPrioridade()))
                    .toList();
        }

        if (filtro.getCategoria() != null && !filtro.getCategoria().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getCategoria() != null
                            && ch.getCategoria().name().equalsIgnoreCase(filtro.getCategoria()))
                    .toList();
        }

        if (filtro.getSetor() != null && !filtro.getSetor().isBlank()) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getUsuario() != null
                            && ch.getUsuario().getSetor() != null
                            && ch.getUsuario().getSetor().name().equalsIgnoreCase(filtro.getSetor()))
                    .toList();
        }

        if (filtro.getDataInicio() != null) {
            java.time.LocalDateTime inicio = filtro.getDataInicio().atStartOfDay();
            chamados = chamados.stream()
                    .filter(ch -> ch.getCreatedAt() != null && !ch.getCreatedAt().isBefore(inicio))
                    .toList();
        }

        if (filtro.getDataFim() != null) {
            java.time.LocalDateTime fim = filtro.getDataFim().atTime(23, 59, 59);
            chamados = chamados.stream()
                    .filter(ch -> ch.getCreatedAt() != null && !ch.getCreatedAt().isAfter(fim))
                    .toList();
        }

        if (filtro.getTecnicoId() != null) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getTecnico() != null
                            && ch.getTecnico().getId().equals(filtro.getTecnicoId()))
                    .toList();
        }

        if (filtro.getUsuarioId() != null) {
            chamados = chamados.stream()
                    .filter(ch -> ch.getUsuario() != null
                            && ch.getUsuario().getId().equals(filtro.getUsuarioId()))
                    .toList();
        }

        return chamados.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ChamadoResponse atualizarStatus(Long chamadoId, String novoStatus, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);

        Status statusAntigo = chamado.getStatus();
        Status statusNovo = Status.valueOf(novoStatus.toUpperCase());

        if (statusAntigo == statusNovo) {
            return mapToResponse(chamado);
        }

        chamado.setStatus(statusNovo);
        chamado.setUpdatedAt(LocalDateTime.now());

        // Ao resolver, se ainda não houver técnico responsável, quem resolveu assume o chamado
        if (statusNovo == Status.RESOLVIDO
                && chamado.getTecnico() == null
                && (usuarioAcao.getPerfil() == Usuario.Perfil.TECNICO || usuarioAcao.getPerfil() == Usuario.Perfil.ADMIN)) {
            chamado.setTecnico(usuarioAcao);
        }

        // Se o chamado está sendo reaberto, limpa a solução/fechamento anteriores
        if (statusNovo == Status.ABERTO || statusNovo == Status.EM_ANDAMENTO) {
            chamado.setDataFechamento(null);
        }

        Chamado atualizado = chamadoRepository.save(chamado);

        String descricaoHistorico = (statusAntigo == Status.FECHADO || statusAntigo == Status.RESOLVIDO)
                && (statusNovo == Status.ABERTO || statusNovo == Status.EM_ANDAMENTO)
                ? "Chamado reaberto por " + usuarioAcao.getNome()
                : "Status alterado de " + statusAntigo + " para " + statusNovo;

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                descricaoHistorico,
                TipoAlteracao.STATUS
        );

        // 🔔 Notifica o solicitante sobre a mudança de status
        boolean foiReaberto = (statusAntigo == Status.FECHADO || statusAntigo == Status.RESOLVIDO)
                && (statusNovo == Status.ABERTO || statusNovo == Status.EM_ANDAMENTO);

        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                foiReaberto
                        ? "Chamado #" + atualizado.getId() + " reaberto"
                        : "Chamado #" + atualizado.getId() + " atualizado",
                foiReaberto
                        ? "Seu chamado foi reaberto por " + usuarioAcao.getNome() + "."
                        : statusNovo == Status.RESOLVIDO
                                ? "Seu chamado foi resolvido! Assim que for fechado, você poderá avaliar o atendimento."
                                : "O status do seu chamado mudou para " + statusLabel(statusNovo) + ".",
                foiReaberto ? TipoNotificacao.CHAMADO_REABERTO
                        : statusNovo == Status.RESOLVIDO ? TipoNotificacao.CHAMADO_RESOLVIDO
                        : TipoNotificacao.CHAMADO_ATUALIZADO,
                atualizado.getId()
        );

        return mapToResponse(atualizado);
    }

    private String statusLabel(Status status) {
        return switch (status) {
            case ABERTO -> "Aberto";
            case EM_ANDAMENTO -> "Em andamento";
            case RESOLVIDO -> "Resolvido";
            case FECHADO -> "Fechado";
        };
    }
    public ChamadoResponse fecharChamado(Long chamadoId, FecharChamadoRequest request, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);

        if (request.getDescricaoSolucao() == null || request.getDescricaoSolucao().isBlank()) {
            throw new BusinessException("Descrição da solução é obrigatória");
        }

        if (request.getTecnicoAtribuidoId() == null) {
            throw new BusinessException(
                    "Informe o técnico que auxiliou no atendimento antes de finalizar o chamado."
            );
        }

        Usuario tecnicoAtribuido = usuarioRepository.findById(request.getTecnicoAtribuidoId())
                .orElseThrow(() -> new ResourceNotFoundException("Técnico atribuído não encontrado"));

        if (tecnicoAtribuido.getPerfil() != Usuario.Perfil.TECNICO
                && tecnicoAtribuido.getPerfil() != Usuario.Perfil.ADMIN) {
            throw new BusinessException("O técnico atribuído precisa ter perfil de técnico.");
        }

        // Se por algum motivo ainda não há técnico responsável, quem está fechando assume
        if (chamado.getTecnico() == null) {
            chamado.setTecnico(usuarioAcao);
        }

        chamado.setDescricaoSolucao(request.getDescricaoSolucao());
        chamado.setTecnicoAtribuido(tecnicoAtribuido);
        chamado.setStatus(Status.FECHADO);
        chamado.setDataFechamento(LocalDateTime.now());
        chamado.setUpdatedAt(LocalDateTime.now());

        Chamado atualizado = chamadoRepository.save(chamado);

        String descricaoHistorico = String.format(
                "Chamado fechado por %s. Responsável: %s. Auxiliou: %s.",
                usuarioAcao.getNome(),
                atualizado.getTecnico() != null ? atualizado.getTecnico().getNome() : "—",
                tecnicoAtribuido.getNome()
        );

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                descricaoHistorico,
                TipoAlteracao.STATUS
        );

        // 🔔 Notifica o solicitante que o chamado foi fechado (agora ele pode avaliar)
        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                "Chamado #" + atualizado.getId() + " fechado",
                "Seu chamado foi encerrado por " + usuarioAcao.getNome()
                        + ". Que tal avaliar o atendimento?",
                TipoNotificacao.CHAMADO_FECHADO,
                atualizado.getId()
        );

        return mapToResponse(atualizado);
    }

    // NOVO: editar título/descrição do chamado
    public ChamadoResponse editar(Long chamadoId, EditarChamadoRequest request, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);

        chamado.setTitulo(request.getTitulo());
        chamado.setDescricao(request.getDescricao());
        chamado.setUpdatedAt(LocalDateTime.now());

        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                "Chamado editado por " + usuarioAcao.getNome(),
                TipoAlteracao.OUTRO
        );

        // 🔔 Notifica o solicitante que os dados do chamado foram alterados
        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                "Chamado #" + atualizado.getId() + " atualizado",
                usuarioAcao.getNome() + " editou as informações do seu chamado.",
                TipoNotificacao.CHAMADO_ATUALIZADO,
                atualizado.getId()
        );

        return mapToResponse(atualizado);
    }

    // NOVO: excluir chamado
    public void excluir(Long chamadoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        chamadoRepository.delete(chamado);
    }

    // NOVO: técnico/admin define a prioridade de um chamado que o usuário
    // comum abriu sem prioridade. É neste momento que o relógio do SLA
    // começa a contar (antes disso não havia prazo pra medir).
    public ChamadoResponse definirPrioridade(
            Long chamadoId,
            DefinirPrioridadeRequest request,
            Long usuarioAcaoId
    ) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);

        Prioridade prioridade;
        try {
            prioridade = Prioridade.valueOf(request.getPrioridade().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Prioridade inválida.");
        }

        chamado.setPrioridade(prioridade);
        chamado.setUpdatedAt(LocalDateTime.now());

        // Só reinicia o relógio se ainda não tinha sido definido antes
        if (chamado.getSlaInicio() == null) {
            chamado.setSlaInicio(LocalDateTime.now());
            chamado.setSlaFim(
                    LocalDateTime.now().plusMinutes(
                            slaConfiguracaoService.minutosResolucaoPara(prioridade)
                    )
            );
        }

        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                "Prioridade definida como " + prioridade + " por " + usuarioAcao.getNome(),
                TipoAlteracao.PRIORIDADE
        );

        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                "Chamado #" + atualizado.getId() + " priorizado",
                "A prioridade do seu chamado foi definida como " + prioridade + ".",
                TipoNotificacao.CHAMADO_ATUALIZADO,
                atualizado.getId()
        );

        return mapToResponse(atualizado);
    }

    private Usuario resolverUsuarioAcao(Long usuarioAcaoId, Chamado chamado) {
        if (usuarioAcaoId == null) {
            return chamado.getUsuario();
        }
        return usuarioRepository.findById(usuarioAcaoId)
                .orElse(chamado.getUsuario());
    }

    private ChamadoResponse mapToResponse(Chamado chamado) {
        return ChamadoResponse.builder()
                .id(chamado.getId())
                .titulo(chamado.getTitulo())
                .descricao(chamado.getDescricao())
                .descricaoSolucao(chamado.getDescricaoSolucao())
                .usuarioNome(
                        chamado.getUsuario() != null
                                ? chamado.getUsuario().getNome()
                                : null
                )
                .tecnicoId(
                        chamado.getTecnico() != null
                                ? chamado.getTecnico().getId()
                                : null
                )
                .tecnicoNome(
                        chamado.getTecnico() != null
                                ? chamado.getTecnico().getNome()
                                : null
                )
                .tecnicoAtribuidoId(
                        chamado.getTecnicoAtribuido() != null
                                ? chamado.getTecnicoAtribuido().getId()
                                : null
                )
                .tecnicoAtribuidoNome(
                        chamado.getTecnicoAtribuido() != null
                                ? chamado.getTecnicoAtribuido().getNome()
                                : null
                )
                .setor(
                        chamado.getUsuario() != null
                                        && chamado.getUsuario().getSetor() != null
                                ? chamado.getUsuario().getSetor().name()
                                : null
                )
                .status(
                        chamado.getStatus() != null
                                ? chamado.getStatus().name()
                                : null
                )
                .prioridade(
                        chamado.getPrioridade() != null
                                ? chamado.getPrioridade().name()
                                : null
                )
                .categoria(
                        chamado.getCategoria() != null
                                ? chamado.getCategoria().name()
                                : null
                )
                .slaInicio(chamado.getSlaInicio())
                .slaFim(chamado.getSlaFim())
                .dataFechamento(chamado.getDataFechamento())
                .createdAt(chamado.getCreatedAt())
                .updatedAt(chamado.getUpdatedAt())
                .atrasado(calcularAtrasado(chamado))
                .build();
    }

    // Calculado na hora, sempre correto — não depende de nenhum job
    // rodando em segundo plano pra manter atualizado.
    private Boolean calcularAtrasado(Chamado chamado) {
        if (chamado.getStatus() == Status.RESOLVIDO || chamado.getStatus() == Status.FECHADO) {
            return false;
        }
        if (chamado.getSlaFim() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(chamado.getSlaFim());
    }
}