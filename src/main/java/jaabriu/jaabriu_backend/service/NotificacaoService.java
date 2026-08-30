package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.NotificacaoResponse;
import jaabriu.jaabriu_backend.entity.Notificacao;
import jaabriu.jaabriu_backend.entity.TipoNotificacao;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.exception.ResourceNotFoundException;
import jaabriu.jaabriu_backend.repository.NotificacaoRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Cria, salva e envia uma notificação em tempo real pro destinatário.
     * Se o destinatário for null, ou for a mesma pessoa que gerou a ação
     * (autorAcaoId), não faz nada — não faz sentido notificar a si mesmo.
     */
    public void notificar(
            Usuario destinatario,
            Long autorAcaoId,
            String titulo,
            String mensagem,
            TipoNotificacao tipo,
            Long chamadoId
    ) {
        if (destinatario == null) return;
        if (autorAcaoId != null && destinatario.getId().equals(autorAcaoId)) return;

        Notificacao notificacao = Notificacao.builder()
                .destinatario(destinatario)
                .titulo(titulo)
                .mensagem(mensagem)
                .tipo(tipo)
                .chamadoId(chamadoId)
                .lida(false)
                .build();

        Notificacao salva = notificacaoRepository.save(notificacao);

        NotificacaoResponse response = mapToResponse(salva);

        // Envia só pro destinatário (fila privada dele), em tempo real
        messagingTemplate.convertAndSendToUser(
                destinatario.getEmail(),
                "/queue/notificacoes",
                response
        );
    }

    public List<NotificacaoResponse> listarRecentes(Long usuarioId) {
        return notificacaoRepository
                .findTop30ByDestinatarioIdOrderByCreatedAtDesc(usuarioId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Long contarNaoLidas(Long usuarioId) {
        return notificacaoRepository.countByDestinatarioIdAndLidaFalse(usuarioId);
    }

    public void marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));

        if (!notificacao.getDestinatario().getId().equals(usuarioId)) {
            throw new ResourceNotFoundException("Notificação não encontrada");
        }

        notificacao.setLida(true);
        notificacaoRepository.save(notificacao);
    }

    public void marcarTodasComoLidas(Long usuarioId) {
        List<Notificacao> naoLidas = notificacaoRepository.findByDestinatarioIdAndLidaFalse(usuarioId);
        naoLidas.forEach(n -> n.setLida(true));
        notificacaoRepository.saveAll(naoLidas);
    }

    private NotificacaoResponse mapToResponse(Notificacao notificacao) {
        return NotificacaoResponse.builder()
                .id(notificacao.getId())
                .titulo(notificacao.getTitulo())
                .mensagem(notificacao.getMensagem())
                .tipo(notificacao.getTipo().name())
                .chamadoId(notificacao.getChamadoId())
                .lida(notificacao.getLida())
                .createdAt(notificacao.getCreatedAt())
                .build();
    }
}
