package jaabriu.jaabriu_backend.service;

import jaabriu.jaabriu_backend.dto.ChamadoResponse;
import jaabriu.jaabriu_backend.entity.Chamado;
import jaabriu.jaabriu_backend.entity.Usuario;
import jaabriu.jaabriu_backend.repository.UsuarioRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transmite eventos de chamado (criado, status alterado, prioridade
 * definida, fechado, etc.) em tempo real via WebSocket, sem precisar de F5.
 *
 * Regra de segurança (mesma da API REST): usuário comum só recebe eventos
 * dos PRÓPRIOS chamados; técnico e admin recebem de todos, já que também
 * enxergam todos os chamados pela API normal.
 *
 * Reaproveita a mesma infraestrutura já usada pelo chat e pelas
 * notificações (SimpMessagingTemplate + fila privada por usuário), sem
 * criar uma solução paralela.
 */
@Service
public class ChamadoRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioRepository usuarioRepository;

    public ChamadoRealtimeService(
            SimpMessagingTemplate messagingTemplate,
            UsuarioRepository usuarioRepository
    ) {
        this.messagingTemplate = messagingTemplate;
        this.usuarioRepository = usuarioRepository;
    }

    public void transmitir(Chamado chamado, ChamadoResponse response, String tipoEvento) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tipo", tipoEvento); // "CHAMADO_CRIADO" | "CHAMADO_ATUALIZADO"
        payload.put("chamado", response);

        // 1) Quem estiver com a TELA DE DETALHES desse chamado aberta —
        //    mesmo tópico por chamado já usado pelo chat (canal "eventos"
        //    separado do canal "comentarios" pra não misturar os payloads).
        messagingTemplate.convertAndSend(
                "/topic/chamados/" + chamado.getId() + "/eventos",
                payload
        );

        // 2) O dono do chamado — só o dele, nunca de chamados de outra
        //    pessoa (fila privada, igual ao sistema de notificações).
        if (chamado.getUsuario() != null) {
            messagingTemplate.convertAndSendToUser(
                    chamado.getUsuario().getEmail(),
                    "/queue/chamados",
                    payload
            );
        }

        // 3) Todo técnico e admin — eles já enxergam todos os chamados
        //    pela API normal, então também recebem todas as atualizações
        //    em tempo real pra manter a lista deles sempre correta.
        List<Usuario> equipe = usuarioRepository.findAll().stream()
                .filter(u -> u.getPerfil() == Usuario.Perfil.TECNICO
                        || u.getPerfil() == Usuario.Perfil.ADMIN)
                .toList();

        for (Usuario membro : equipe) {
            messagingTemplate.convertAndSendToUser(
                    membro.getEmail(),
                    "/queue/chamados",
                    payload
            );
        }
    }
}
