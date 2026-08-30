package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ComentarioRequest;
import jaabriu.jaabriu_backend.dto.ComentarioResponse;
import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Comentario;
import jaabriu.jaabriu_backend.entity.TipoNotificacao;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.ChamadoRepository;
import jaabriu.jaabriu_backend.repository.ComentarioRepository;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistoricoService historicoService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificacaoService notificacaoService;

    public ComentarioService(
            ComentarioRepository comentarioRepository,
            ChamadoRepository chamadoRepository,
            UsuarioRepository usuarioRepository,
            HistoricoService historicoService,
            SimpMessagingTemplate messagingTemplate,
            NotificacaoService notificacaoService
    ) {
        this.comentarioRepository = comentarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historicoService = historicoService;
        this.messagingTemplate = messagingTemplate;
        this.notificacaoService = notificacaoService;
    }

    public ComentarioResponse adicionarComentario(
            Long chamadoId,
            Long usuarioId,
            ComentarioRequest request
    ) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Comentario comentario = new Comentario();
        comentario.setMensagem(request.getMensagem());
        comentario.setInterno(Boolean.TRUE.equals(request.getInterno()));
        comentario.setChamado(chamado);
        comentario.setAutor(usuario);
        comentario.setCreatedAt(LocalDateTime.now());

        Comentario salvo = comentarioRepository.save(comentario);

        historicoService.registrar(
                chamado,
                usuario,
                "Comentário adicionado ao chamado"
        );

        ComentarioResponse response = mapToResponse(salvo);

        // 🔴 Tempo real: publica a nova mensagem pra quem estiver com o
        // chamado aberto (usuário, técnico ou admin), sem precisar de F5.
        messagingTemplate.convertAndSend(
                "/topic/chamados/" + chamadoId + "/comentarios",
                response
        );

        // 🔔 Notifica todo mundo envolvido no chamado, menos quem escreveu
        String titulo = "Nova mensagem no chamado #" + chamado.getId();
        String mensagemNotificacao = usuario.getNome() + " enviou uma nova mensagem.";

        Set<Usuario> envolvidos = new HashSet<>();
        if (chamado.getUsuario() != null) envolvidos.add(chamado.getUsuario());
        if (chamado.getTecnico() != null) envolvidos.add(chamado.getTecnico());
        if (chamado.getTecnicoAtribuido() != null) envolvidos.add(chamado.getTecnicoAtribuido());

        for (Usuario destinatario : envolvidos) {
            notificacaoService.notificar(
                    destinatario,
                    usuario.getId(),
                    titulo,
                    mensagemNotificacao,
                    TipoNotificacao.NOVA_MENSAGEM,
                    chamado.getId()
            );
        }

        return response;
    }

    public List<ComentarioResponse> listarPorChamado(Long chamadoId) {

        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamado não encontrado"));

        return comentarioRepository.findByChamadoOrderByCreatedAtDesc(chamado)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ComentarioResponse mapToResponse(Comentario comentario) {
        return ComentarioResponse.builder()
                .id(comentario.getId())
                .mensagem(comentario.getMensagem())
                .interno(comentario.getInterno())
                .autorId(comentario.getAutor().getId())
                .autorNome(comentario.getAutor().getNome())
                .autorPerfil(
                        comentario.getAutor().getPerfil() != null
                                ? comentario.getAutor().getPerfil().name()
                                : null
                )
                .chamadoId(comentario.getChamado().getId())
                .createdAt(comentario.getCreatedAt())
                .build();
    }
}