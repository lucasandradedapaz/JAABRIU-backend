package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.AdicionarSolucaoRequest;
import jaabriu.jaabriu_backend.dto.AtribuirSetorRequest;
import jaabriu.jaabriu_backend.dto.AtribuirTecnicoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoFiltroRequest;
import jaabriu.jaabriu_backend.dto.ChamadoRequest;
import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.dto.DefinirPrioridadeRequest;
import jaabriu.jaabriu_backend.dto.EditarChamadoRequest;
import jaabriu.jaabriu_backend.dto.SolucaoResponse;
import jaabriu.jaabriu_backend.dto.UsuarioResponse;
import jaabriu.jaabriu_backend.entity.*;
import jaabriu.jaabriu_backend.exception.BusinessException;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.SolucaoChamadoRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import jaabriu.jaabriu_backend.util.HtmlSanitizer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SolucaoChamadoRepository solucaoChamadoRepository;
    private final HistoricoService historicoService;
    private final NotificacaoService notificacaoService;
    private final SlaConfiguracaoService slaConfiguracaoService;
    private final ChamadoRealtimeService chamadoRealtimeService;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            SolucaoChamadoRepository solucaoChamadoRepository,
            HistoricoService historicoService,
            NotificacaoService notificacaoService,
            SlaConfiguracaoService slaConfiguracaoService,
            ChamadoRealtimeService chamadoRealtimeService
    ) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.solucaoChamadoRepository = solucaoChamadoRepository;
        this.historicoService = historicoService;
        this.notificacaoService = notificacaoService;
        this.slaConfiguracaoService = slaConfiguracaoService;
        this.chamadoRealtimeService = chamadoRealtimeService;
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

        ChamadoResponse resposta = mapToResponse(salvo);
        chamadoRealtimeService.transmitir(salvo, resposta, "CHAMADO_CRIADO");

        return resposta;
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

        // Fechar exige que já exista pelo menos uma solução registrada no
        // histórico (item 5 do pedido: o status continua controlado pelas
        // regras já existentes, só trocamos "técnico que auxiliou" por essa
        // checagem do histórico de soluções).
        if (statusNovo == Status.FECHADO && !solucaoChamadoRepository.existsByChamado(chamado)) {
            throw new BusinessException(
                    "Registre uma solução antes de fechar o chamado."
            );
        }

        chamado.setStatus(statusNovo);
        chamado.setUpdatedAt(LocalDateTime.now());

        // Ao resolver/fechar, se ainda não houver técnico responsável, quem
        // fez a ação assume o chamado
        if ((statusNovo == Status.RESOLVIDO || statusNovo == Status.FECHADO)
                && chamado.getTecnico() == null
                && (usuarioAcao.getPerfil() == Usuario.Perfil.TECNICO || usuarioAcao.getPerfil() == Usuario.Perfil.ADMIN)) {
            chamado.setTecnico(usuarioAcao);
        }

        if (statusNovo == Status.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }

        // Se o chamado está sendo reaberto, limpa só a data de fechamento —
        // o histórico de soluções (item 2.2 do pedido) NUNCA é apagado nem
        // substituído aqui.
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

        String tituloNotificacao;
        String mensagemNotificacao;
        TipoNotificacao tipoNotificacao;

        if (foiReaberto) {
            tituloNotificacao = "Chamado #" + atualizado.getId() + " reaberto";
            mensagemNotificacao = "Seu chamado foi reaberto por " + usuarioAcao.getNome() + ".";
            tipoNotificacao = TipoNotificacao.CHAMADO_REABERTO;
        } else if (statusNovo == Status.RESOLVIDO) {
            tituloNotificacao = "Chamado #" + atualizado.getId() + " atualizado";
            mensagemNotificacao = "Seu chamado foi resolvido! Assim que for fechado, você poderá avaliar o atendimento.";
            tipoNotificacao = TipoNotificacao.CHAMADO_RESOLVIDO;
        } else if (statusNovo == Status.FECHADO) {
            tituloNotificacao = "Chamado #" + atualizado.getId() + " fechado";
            mensagemNotificacao = "Seu chamado foi encerrado por " + usuarioAcao.getNome()
                    + ". Que tal avaliar o atendimento?";
            tipoNotificacao = TipoNotificacao.CHAMADO_FECHADO;
        } else {
            tituloNotificacao = "Chamado #" + atualizado.getId() + " atualizado";
            mensagemNotificacao = "O status do seu chamado mudou para " + statusLabel(statusNovo) + ".";
            tipoNotificacao = TipoNotificacao.CHAMADO_ATUALIZADO;
        }

        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                tituloNotificacao,
                mensagemNotificacao,
                tipoNotificacao,
                atualizado.getId()
        );

        ChamadoResponse respostaStatus = mapToResponse(atualizado);
        chamadoRealtimeService.transmitir(atualizado, respostaStatus, "CHAMADO_ATUALIZADO");

        return respostaStatus;
    }

    private String statusLabel(Status status) {
        return switch (status) {
            case ABERTO -> "Aberto";
            case EM_ANDAMENTO -> "Em andamento";
            case RESOLVIDO -> "Resolvido";
            case FECHADO -> "Fechado";
        };
    }

    // ============================================================
    // 1. ATRIBUIÇÃO DE SETOR E TÉCNICOS
    // ============================================================

    // Garante, dentro do service (e não só escondendo botão no frontend),
    // que só ADMIN/TECNICO conseguem atribuir setor/técnicos ou registrar
    // solução — item 8 do pedido.
    private void exigirGerenciador(Usuario usuarioAcao) {
        if (usuarioAcao.getPerfil() != Usuario.Perfil.TECNICO
                && usuarioAcao.getPerfil() != Usuario.Perfil.ADMIN) {
            throw new AccessDeniedException(
                    "Apenas administradores ou técnicos podem realizar esta ação."
            );
        }
    }

    // Usuário comum só pode ver os próprios dados; técnico/admin veem tudo.
    private void exigirVisualizacao(Chamado chamado, Long usuarioLogadoId, Usuario.Perfil perfil) {
        if (perfil == Usuario.Perfil.USUARIO
                && (chamado.getUsuario() == null || !chamado.getUsuario().getId().equals(usuarioLogadoId))) {
            throw new AccessDeniedException("Você não tem permissão para ver este chamado.");
        }
    }

    public ChamadoResponse atribuirSetor(Long chamadoId, AtribuirSetorRequest request, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);
        exigirGerenciador(usuarioAcao);

        Setor setor;
        try {
            setor = Setor.valueOf(request.getSetor().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Setor inválido.");
        }

        chamado.setSetorResponsavel(setor);
        chamado.setUpdatedAt(LocalDateTime.now());

        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                "Setor responsável definido como " + setor.getDescricao() + " por " + usuarioAcao.getNome(),
                TipoAlteracao.SETOR
        );

        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                "Chamado #" + atualizado.getId() + " atualizado",
                usuarioAcao.getNome() + " definiu o setor responsável como " + setor.getDescricao() + ".",
                TipoNotificacao.CHAMADO_ATUALIZADO,
                atualizado.getId()
        );

        ChamadoResponse resposta = mapToResponse(atualizado);
        chamadoRealtimeService.transmitir(atualizado, resposta, "CHAMADO_ATUALIZADO");

        return resposta;
    }

    public ChamadoResponse adicionarTecnico(Long chamadoId, AtribuirTecnicoRequest request, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);
        exigirGerenciador(usuarioAcao);

        Usuario tecnico = usuarioRepository.findById(request.getTecnicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Técnico não encontrado"));

        if (tecnico.getPerfil() != Usuario.Perfil.TECNICO || !Boolean.TRUE.equals(tecnico.getAtivo())) {
            throw new BusinessException("Só é possível atribuir técnicos ativos.");
        }

        // Idempotente: se já estiver atribuído, apenas retorna o estado
        // atual (evita duplicidade — item 1.2/1.4 do pedido).
        boolean jaAtribuido = chamado.getTecnicosAtribuidos().stream()
                .anyMatch(t -> t.getId().equals(tecnico.getId()));

        if (!jaAtribuido) {
            chamado.getTecnicosAtribuidos().add(tecnico);
            chamado.setUpdatedAt(LocalDateTime.now());
            chamado = chamadoRepository.save(chamado);

            historicoService.registrar(
                    chamado,
                    usuarioAcao,
                    tecnico.getNome() + " foi atribuído ao chamado por " + usuarioAcao.getNome(),
                    TipoAlteracao.TECNICO
            );

            // 🔔 + tempo real: o técnico atribuído recebe o chamado na hora,
            // sem precisar de F5 (item 1.6/1.7 do pedido) — o broadcast
            // "CHAMADO_ATUALIZADO" abaixo já cobre isso porque técnico/admin
            // recebem todos os chamados pelo canal de equipe.
            notificacaoService.notificar(
                    tecnico,
                    usuarioAcao.getId(),
                    "Você foi atribuído ao chamado #" + chamado.getId(),
                    usuarioAcao.getNome() + " atribuiu você a este chamado.",
                    TipoNotificacao.CHAMADO_ATUALIZADO,
                    chamado.getId()
            );
        }

        ChamadoResponse resposta = mapToResponse(chamado);
        chamadoRealtimeService.transmitir(chamado, resposta, "CHAMADO_ATUALIZADO");

        return resposta;
    }

    public ChamadoResponse removerTecnico(Long chamadoId, Long tecnicoId, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);
        exigirGerenciador(usuarioAcao);

        Usuario tecnico = chamado.getTecnicosAtribuidos().stream()
                .filter(t -> t.getId().equals(tecnicoId))
                .findFirst()
                .orElse(null);

        if (tecnico != null) {
            chamado.getTecnicosAtribuidos().remove(tecnico);
            chamado.setUpdatedAt(LocalDateTime.now());
            chamado = chamadoRepository.save(chamado);

            historicoService.registrar(
                    chamado,
                    usuarioAcao,
                    tecnico.getNome() + " foi removido do chamado por " + usuarioAcao.getNome(),
                    TipoAlteracao.TECNICO
            );

            notificacaoService.notificar(
                    tecnico,
                    usuarioAcao.getId(),
                    "Você foi removido do chamado #" + chamado.getId(),
                    usuarioAcao.getNome() + " removeu você deste chamado.",
                    TipoNotificacao.CHAMADO_ATUALIZADO,
                    chamado.getId()
            );
        }

        ChamadoResponse resposta = mapToResponse(chamado);
        chamadoRealtimeService.transmitir(chamado, resposta, "CHAMADO_ATUALIZADO");

        return resposta;
    }

    // ============================================================
    // 2/3/4. HISTÓRICO DE SOLUÇÕES
    // ============================================================

    public ChamadoResponse adicionarSolucao(Long chamadoId, AdicionarSolucaoRequest request, Long usuarioAcaoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuarioAcao = resolverUsuarioAcao(usuarioAcaoId, chamado);
        exigirGerenciador(usuarioAcao);

        // 🧼 Sanitiza o HTML do editor ANTES de qualquer outra coisa —
        // nunca confiar em conteúdo vindo do cliente (item 3.2 do pedido).
        String conteudoSeguro = HtmlSanitizer.sanitizar(request.getConteudo());

        if (HtmlSanitizer.isBlank(conteudoSeguro)) {
            throw new BusinessException("Descreva a solução antes de salvar.");
        }

        int proximoNumero = (int) solucaoChamadoRepository.countByChamado(chamado) + 1;

        SolucaoChamado solucao = SolucaoChamado.builder()
                .chamado(chamado)
                .autor(usuarioAcao)
                .numero(proximoNumero)
                .conteudo(conteudoSeguro)
                .statusNoMomento(chamado.getStatus())
                .criadoEm(LocalDateTime.now())
                .build();

        solucaoChamadoRepository.save(solucao);

        // Mantido só por compatibilidade com telas/impressão que ainda leem
        // esse campo — sempre espelha a solução mais recente, nunca é a
        // fonte de verdade (que é o histórico em solucoes_chamado).
        chamado.setDescricaoSolucao(conteudoSeguro);
        chamado.setUpdatedAt(LocalDateTime.now());
        Chamado atualizado = chamadoRepository.save(chamado);

        historicoService.registrar(
                atualizado,
                usuarioAcao,
                "Solução #" + proximoNumero + " registrada por " + usuarioAcao.getNome(),
                TipoAlteracao.SOLUCAO
        );

        // 🔔 Notifica quem abriu o chamado
        notificacaoService.notificar(
                atualizado.getUsuario(),
                usuarioAcao.getId(),
                "Nova solução no chamado #" + atualizado.getId(),
                usuarioAcao.getNome() + " registrou uma nova solução para o seu chamado.",
                TipoNotificacao.CHAMADO_ATUALIZADO,
                atualizado.getId()
        );

        ChamadoResponse resposta = mapToResponse(atualizado);
        chamadoRealtimeService.transmitir(atualizado, resposta, "CHAMADO_ATUALIZADO");

        return resposta;
    }

    public List<SolucaoResponse> listarSolucoes(Long chamadoId, Long usuarioLogadoId, Usuario.Perfil perfil) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        exigirVisualizacao(chamado, usuarioLogadoId, perfil);

        return solucaoChamadoRepository.findByChamadoOrderByNumeroAsc(chamado)
                .stream()
                .map(this::mapSolucaoToResponse)
                .toList();
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

        ChamadoResponse respostaEdicao = mapToResponse(atualizado);
        chamadoRealtimeService.transmitir(atualizado, respostaEdicao, "CHAMADO_ATUALIZADO");

        return respostaEdicao;
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

        ChamadoResponse respostaPrioridade = mapToResponse(atualizado);
        chamadoRealtimeService.transmitir(atualizado, respostaPrioridade, "CHAMADO_ATUALIZADO");

        return respostaPrioridade;
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
                .setor(
                        chamado.getUsuario() != null
                                        && chamado.getUsuario().getSetor() != null
                                ? chamado.getUsuario().getSetor().name()
                                : null
                )
                .setorResponsavel(
                        chamado.getSetorResponsavel() != null
                                ? chamado.getSetorResponsavel().name()
                                : null
                )
                .setorResponsavelLabel(
                        chamado.getSetorResponsavel() != null
                                ? chamado.getSetorResponsavel().getDescricao()
                                : null
                )
                .tecnicosAtribuidos(
                        chamado.getTecnicosAtribuidos() == null
                                ? List.of()
                                : chamado.getTecnicosAtribuidos().stream()
                                        .sorted(Comparator.comparing(Usuario::getNome, String.CASE_INSENSITIVE_ORDER))
                                        .map(t -> UsuarioResponse.builder()
                                                .id(t.getId())
                                                .nome(t.getNome())
                                                .email(t.getEmail())
                                                .perfil(t.getPerfil().name())
                                                .ativo(t.getAtivo())
                                                .build())
                                        .toList()
                )
                .solucoes(
                        chamado.getId() == null
                                ? List.of()
                                : solucaoChamadoRepository.findByChamadoOrderByNumeroAsc(chamado)
                                        .stream()
                                        .map(this::mapSolucaoToResponse)
                                        .toList()
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

    private SolucaoResponse mapSolucaoToResponse(SolucaoChamado solucao) {
        return SolucaoResponse.builder()
                .id(solucao.getId())
                .numero(solucao.getNumero())
                .autorId(solucao.getAutor() != null ? solucao.getAutor().getId() : null)
                .autorNome(solucao.getAutor() != null ? solucao.getAutor().getNome() : null)
                .conteudo(solucao.getConteudo())
                .status(solucao.getStatusNoMomento() != null ? solucao.getStatusNoMomento().name() : null)
                .statusLabel(solucao.getStatusNoMomento() != null ? statusLabel(solucao.getStatusNoMomento()) : null)
                .criadoEm(solucao.getCriadoEm())
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